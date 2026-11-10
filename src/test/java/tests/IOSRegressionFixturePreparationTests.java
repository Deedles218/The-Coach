package tests;

import io.qameta.allure.Allure;
import lib.CoreTestCase;
import lib.Platform;
import lib.TestData;
import lib.UpdateEvidence;
import lib.RestoringTestAction;
import lib.ui.CoachFlowPageObject;
import lib.ui.SubscriptionPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.ios.iOSOnboardingPageObject;
import lib.ui.ios.iOSSubscriptionPageObject;
import lib.ui.ios.iOSTestAccountPageObject;
import lib.ui.ios.iOSProgramSelectorPageObject;
import org.junit.Assert;
import org.junit.Test;

/** Opt-in preparation of a dedicated regression fixture through the installed app UI. */
public class IOSRegressionFixturePreparationTests extends CoreTestCase {
    private boolean preparationComplete;
    @Override protected boolean isPlatformSupported() {
        return Platform.getInstance().isIOS() && Boolean.getBoolean("coach.fixture.prepare");
    }

    @Test public void prepareAndVerifyAccount() throws Exception {
        UpdateEvidence.attachInstalledBuild();
        String goal = System.getProperty("coach.fixture.goal");
        Assert.assertTrue("An explicit fixture onboarding goal is required", goal != null && !goal.trim().isEmpty());
        TestData.TestAccount account = TestData.existingProgressAccount();
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        iOSTestAccountPageObject registration = new iOSTestAccountPageObject(driver);
        if (Boolean.getBoolean("coach.fixture.resumeRegistration")) {
            iOSOnboardingPageObject onboarding = new iOSOnboardingPageObject(driver);
            onboarding.closePaywallsAndPopups();
            onboarding.waitForToday();
            registration.linkAndVerify(account);
            preparationComplete = true;
            Allure.addAttachment("Fixture preparation", "Pending anonymous profile linked and re-login verified; purchase state requires separate evidence");
            return;
        }
        if (Boolean.getBoolean("coach.fixture.resumePurchase")
                || Boolean.getBoolean("coach.fixture.resumePurchaseResult")) {
            iOSSubscriptionPageObject subscription = new iOSSubscriptionPageObject(driver);
            if (Boolean.getBoolean("coach.fixture.resumePurchase")) subscription.confirmTestPurchase();
            subscription.assertPurchaseCompleted();
            iOSOnboardingPageObject onboarding = new iOSOnboardingPageObject(driver);
            onboarding.closePaywallsAndPopups();
            onboarding.waitForToday();
            registration.linkAndVerify(account);
            preparationComplete = true;
            Allure.addAttachment("Fixture preparation", "Pending test purchase confirmed; email linked and re-login verified");
            return;
        }
        if (registration.resumeAnonymousRegistration(account)) {
            preparationComplete = true;
            Allure.addAttachment("Fixture preparation", "Pending registration completed and re-login verified");
            return;
        }
        boolean resumeOnboarding = Boolean.getBoolean("coach.fixture.resumeOnboarding");
        if (!resumeOnboarding && !Boolean.getBoolean("coach.fixture.newAccount")) {
            coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
            coach.openProfile();
            new lib.ui.ios.iOSCoachFlowPageObject(driver).assertAuthenticatedProfileForEmail(account.getEmail());
            preparationComplete = true;
            Allure.addAttachment("Fixture preparation", "Dedicated account already exists; re-login verified");
            return;
        }

        iOSOnboardingPageObject onboarding = new iOSOnboardingPageObject(driver);
        if (resumeOnboarding) {
            onboarding.openStartFlow();
            onboarding.selectGoal(goal);
            onboarding.completeQuestionnaire();
        } else {
            coach.ensureLoggedOutOnStartScreen();
            onboarding.completeNewUserJourneyToPaywall(goal);
        }
        if (Boolean.getBoolean("coach.fixture.purchaseSubscription")) {
            Assert.assertTrue("Fixture purchase requires explicit StoreKit Sandbox authorization",
                    Platform.getInstance().isStoreKitSandboxEnabled()
                            && Platform.getInstance().isStoreKitPurchaseAllowed());
            iOSSubscriptionPageObject subscription = new iOSSubscriptionPageObject(driver);
            subscription.assertPaywallIsDisplayed();
            subscription.selectPlan(SubscriptionPageObject.PlanPosition.valueOf(
                    System.getProperty("coach.fixture.subscriptionPlan", "TOP")));
            subscription.startPurchaseAndWaitForStore();
            subscription.confirmTestPurchase();
            subscription.assertPurchaseCompleted();
        }
        onboarding.closePaywallsAndPopups();
        onboarding.waitForToday();
        registration.linkAndVerify(account);
        preparationComplete = true;
        Allure.addAttachment("Fixture preparation", "Email confirmed through app UI; logout and re-login verified");
    }

    @Override protected void cleanupSessionForNextTest() {
        if (preparationComplete) super.cleanupSessionForNextTest();
        else Allure.addAttachment("Incomplete fixture preserved",
                "Keep the current setup screen and anonymous profile for diagnosis; no logout or account deletion.");
    }

    @Test public void verifyFreshQuestionnaireToPaywall() throws Exception {
        UpdateEvidence.attachInstalledBuild();
        String goal = System.getProperty("coach.fixture.goal");
        Assert.assertTrue("Configure an explicit onboarding goal", goal != null && !goal.trim().isEmpty());
        preparationComplete = true; // This check never purchases or links an account.
        iOSOnboardingPageObject onboarding = new iOSOnboardingPageObject(driver);
        if (Boolean.getBoolean("coach.fixture.resumeOnboarding")) {
            Assert.assertTrue("Resume requires the observed interrupted satisfaction question",
                    onboarding.isElementVisible("id:How satisfied are you with your intimate life?"));
            onboarding.completeQuestionnaire();
        } else {
            CoachFlowPageObjectFactory.get(driver).ensureLoggedOutOnStartScreen();
            onboarding.completeNewUserJourneyToPaywall(goal);
        }
        iOSSubscriptionPageObject paywall = new iOSSubscriptionPageObject(driver);
        paywall.assertPaywallIsDisplayed();
        paywall.assertAutoRenewalDisclosure();
    }

    /** Fixture discovery only: observe configured days without completing their content. */
    @Test public void inspectProgramDays() throws Exception {
        UpdateEvidence.attachInstalledBuild();
        String program = System.getProperty("coach.fixture.inspectProgram");
        String days = System.getProperty("coach.fixture.inspectDays");
        Assert.assertTrue("Configure an explicit program and comma-separated positive days",
                program != null && !program.trim().isEmpty() && days != null && days.matches("[1-9][0-9]*(,[1-9][0-9]*)*"));
        TestData.TestAccount account = TestData.existingProgressAccount();
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        coach.openProfile();
        new lib.ui.ios.iOSCoachFlowPageObject(driver).assertAuthenticatedProfileForEmail(account.getEmail());
        coach.openToday();
        iOSProgramSelectorPageObject today = new iOSProgramSelectorPageObject(driver);
        String originalProgram = today.getActiveProgramName();
        String originalDay = today.getCurrentDayLabel();
        preparationComplete = true; // Inspection uses an already linked account; normal cleanup is safe.
        RestoringTestAction.run(() -> {
            today.ensureProgramSelected(program);
            for (String day : days.split(",")) {
                String label = today.getCurrentDayLabel().replaceFirst("[0-9]+", day);
                today.selectObservedDay(label);
                for (int viewport = 0; viewport < 3; viewport++) {
                    Allure.addAttachment("Fixture candidate " + program + " " + label + " viewport " + viewport,
                            "application/xml", TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
                    today.mobileSwipeUp();
                }
                for (int viewport = 0; viewport < 3; viewport++) today.mobileSwipeDown();
            }
        }, () -> {
            if (today.isLockedNextDayPopupDisplayed()) today.closeLockedNextDayPopup();
            if (today.isLockedModulePopupDisplayed()) today.closeLockedModulePopup();
            today.dismissSelectorIfOpen();
            today.ensureProgramSelected(originalProgram);
            today.selectObservedDay(originalDay);
        });
        preparationComplete = true;
    }
}
