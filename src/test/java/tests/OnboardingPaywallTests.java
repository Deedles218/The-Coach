package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.ui.OnboardingGoal;
import lib.ui.OnboardingPageObject;
import lib.ui.SubscriptionPageObject;
import lib.ui.factories.OnboardingPageObjectFactory;
import lib.ui.factories.SubscriptionPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

@Epic("The Coach new-user onboarding paywall")
public class OnboardingPaywallTests extends CoreTestCase {
    @Test
    @Features({@Feature("New user"), @Feature("Questionnaire"), @Feature("Top subscription")})
    @DisplayName("Case 1: New user purchases the top paywall subscription")
    @Description("Creates a new anonymous user, starts with the first Start now button, selects a goal, completes the long questionnaire, selects the top plan and confirms it in the platform test store.")
    @Step("Run onboarding paywall case 1")
    @Severity(SeverityLevel.BLOCKER)
    public void test01NewUserPurchasesTopSubscription() {
        requireExplicitTestPurchaseApproval();
        SubscriptionPageObject paywall = openNewUserPaywall();
        paywall.selectPlan(SubscriptionPageObject.PlanPosition.TOP);
        paywall.startPurchaseAndWaitForStore();
        paywall.confirmTestPurchase();
        paywall.assertPurchaseCompleted();
    }

    @Test
    @Features({@Feature("New user"), @Feature("Questionnaire"), @Feature("Bottom subscription")})
    @DisplayName("Case 2: New user purchases the bottom paywall subscription")
    @Description("Creates a new anonymous user, completes onboarding, selects the bottom plan and confirms it in the platform test store.")
    @Step("Run onboarding paywall case 2")
    @Severity(SeverityLevel.BLOCKER)
    public void test02NewUserPurchasesBottomSubscription() {
        requireExplicitTestPurchaseApproval();
        SubscriptionPageObject paywall = openNewUserPaywall();
        paywall.selectPlan(SubscriptionPageObject.PlanPosition.BOTTOM);
        paywall.startPurchaseAndWaitForStore();
        paywall.confirmTestPurchase();
        paywall.assertPurchaseCompleted();
    }

    @Test
    @Features({@Feature("New user"), @Feature("Paywall"), @Feature("Terms & Privacy")})
    @DisplayName("Case 3: Terms & Privacy opens from onboarding paywall")
    @Description("Completes new-user onboarding up to the paywall and verifies that the Terms & Privacy destination opens.")
    @Step("Run onboarding paywall case 3")
    @Severity(SeverityLevel.CRITICAL)
    public void test03TermsAndPrivacyOpens() {
        SubscriptionPageObject paywall = openNewUserPaywall();
        paywall.openTermsAndPrivacy();
        paywall.assertTermsAndPrivacyOpened();
    }

    @Test
    @Features({@Feature("New user"), @Feature("Paywall"), @Feature("Subscription disclosure")})
    @DisplayName("Case 4: Paywall shows the exact renewal, privacy and terms disclosure")
    @Description("Verifies: Subscription renews automatically. Cancel anytime. Privacy Policy & Terms of Service")
    @Step("Run onboarding paywall case 4")
    @Severity(SeverityLevel.CRITICAL)
    public void test04AutoRenewalDisclosureIsExact() {
        SubscriptionPageObject paywall = openNewUserPaywall();
        paywall.assertAutoRenewalDisclosure();
    }

    @Test
    @Features({@Feature("New user"), @Feature("Paywall"), @Feature("Restore")})
    @DisplayName("Case 5: Restore is available and returns an explicit result")
    @Description("Verifies Restore is visible, enabled and responds with restored access or an explicit no-purchase/error result.")
    @Step("Run onboarding paywall case 5")
    @Severity(SeverityLevel.CRITICAL)
    public void test05RestoreResponds() {
        SubscriptionPageObject paywall = openNewUserPaywall();
        paywall.assertRestoreButtonIsAvailable();
        paywall.tapRestoreAndAssertResult();
    }

    private SubscriptionPageObject openNewUserPaywall() {
        requireFreshInstallConfiguration();
        OnboardingPageObject onboarding = OnboardingPageObjectFactory.get(driver);
        SubscriptionPageObject paywall = SubscriptionPageObjectFactory.get(driver);
        Assert.assertNotNull("Onboarding page object is unavailable for current platform", onboarding);
        Assert.assertNotNull("Subscription page object is unavailable for current platform", paywall);

        OnboardingGoal goal = OnboardingGoal.fromConfiguredValue(
                Platform.getInstance().getOnboardingGoal()
        );
        onboarding.completeNewUserJourneyToPaywall(goal);
        paywall.assertPaywallIsDisplayed();
        return paywall;
    }

    private void requireFreshInstallConfiguration() {
        if (Platform.getInstance().isAndroid()) {
            Assert.assertTrue(
                    "Android paywall tests require -Dandroid.app=<apk> -Dandroid.fullReset=true -Dandroid.noReset=false",
                    Platform.getInstance().isFreshAndroidInstallConfigured()
            );
            return;
        }
        Assert.assertTrue(
                "iOS paywall tests attach to the installed TestFlight build by bundle ID: omit ios.app and use ios.noReset=true, ios.fullReset=false",
                Platform.getInstance().isTestFlightAttachConfigured()
        );
    }

    private void requireExplicitTestPurchaseApproval() {
        String environmentHint = Platform.getInstance().isIOS()
                ? "Enable TestFlight/StoreKit sandbox with -Dstorekit.sandbox=true"
                : "Use a Google Play license tester and set -Dgoogleplay.licenseTester=true";
        Assert.assertTrue(environmentHint, Platform.getInstance().isTestPurchaseEnvironmentEnabled());
        Assert.assertTrue(
                "Purchase confirmation is disabled by default; explicitly enable test transactions with -Dpurchase.allow=true",
                Platform.getInstance().isTestPurchaseAllowed()
        );
    }
}
