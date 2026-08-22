package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

@Epic(value = "The Coach Smoke")
public class SmokeTests extends CoreTestCase {
    @Test
    @Features(value = {@Feature(value = "Clean install"), @Feature(value = "Onboarding"), @Feature(value = "Today")})
    @Issue("COA-7914")
    @DisplayName("Smoke: clean install onboarding opens Daily Plan")
    @Description("Starts the app from a real clean-install capability set, completes the configured onboarding steps, verifies the in-app push prompt is shown, and verifies the authorized Daily Plan is underneath it.")
    @Step("Start test testCleanInstallOnboardingLeadsToDailyPlan")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testCleanInstallOnboardingLeadsToDailyPlan() {
        requireIOSPlatform();

        requireCleanInstallConfiguration();
        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.activateAppIfPossible();
        coachFlow.waitForStartScreen();
        coachFlow.openStartFlow();
        coachFlow.completeOnboardingUsingConfiguredSteps(Platform.getInstance().getIOSOnboardingStepLocators());
        coachFlow.assertNotificationPromptIsDisplayed();
        coachFlow.assertAuthorizedDashboardIsDisplayed();
    }

    @Test
    @Features(value = {@Feature(value = "Today"), @Feature(value = "Daily Plan")})
    @Issue("COA-7956")
    @DisplayName("Smoke: Today displays the Daily Plan entry point")
    @Description("Verifies the current day switcher and Daily Practice section for the existing-progress smoke fixture.")
    @Step("Start test testTodayDailyPlanIsDisplayed")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testTodayDailyPlanIsDisplayed() {
        requireIOSPlatform();

        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);
        authorizedCoachFlow();
        TestData.Fixture fixture = TestData.deterministicFixture();
        dailyPlan.openTodayTab();
        dailyPlan.assertDailyPlanDaySwitcherIsDisplayed();
        dailyPlan.assertCurrentDayMatches(fixture.getDailyPlanDay());
        dailyPlan.assertDailyLessonsAreDisplayed();
        dailyPlan.assertDailyPracticeIsDisplayed();
    }

    @Test
    @Features(value = {@Feature(value = "Welcome"), @Feature(value = "Login"), @Feature(value = "Validation")})
    @Issue("COA-7948")
    @DisplayName("Smoke: Login with empty email keeps Continue disabled")
    @Description("Opens Login from the Welcome screen and verifies the empty email validation state without submitting the form.")
    @Step("Start test testLoginWithEmptyEmailKeepsContinueDisabled")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testLoginWithEmptyEmailKeepsContinueDisabled() {
        requireIOSPlatform();

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);
        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.openLoginFlow();
        coachFlow.assertLoginContinueButtonIsDisabled();
        coachFlow.assertDisabledLoginContinueIsNoOp();
        coachFlow.returnFromLoginFlowToStartScreen();
    }

    @Test
    @Features(value = {@Feature(value = "Authorization"), @Feature(value = "OTP"), @Feature(value = "Daily Plan")})
    @Issue("COA-7935")
    @DisplayName("Smoke: email and OTP open the authorized Daily Plan")
    @Description("Uses the secret-backed existing-progress fixture, logs in with email and OTP, and verifies the authorized Daily Plan entry point.")
    @Step("Start test testEmailAndOtpOpenAuthorizedDailyPlan")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testEmailAndOtpOpenAuthorizedDailyPlan() {
        requireIOSPlatform();

        CoachFlowPageObject coachFlow = authorizedCoachFlow();
        coachFlow.assertAuthorizedDashboardIsDisplayed();
        coachFlow.openToday();
    }

    @Test
    @Features(value = {@Feature(value = "Profile"), @Feature(value = "Accessibility")})
    @Issue("COA-8503")
    @DisplayName("Smoke: Profile opens through accessibility locators")
    @Description("Verifies Profile opens from Today without coordinate tapping and displays its primary progress/settings content.")
    @Step("Start test testProfileOpensWithoutCoordinateFallback")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testProfileOpensWithoutCoordinateFallback() {
        requireIOSPlatform();

        CoachFlowPageObject coachFlow = authorizedCoachFlow();
        coachFlow.openProfile();
        coachFlow.assertProfileSmokeContentIsDisplayed();
    }

    @Test
    @Features(value = {@Feature(value = "Profile"), @Feature(value = "Logout"), @Feature(value = "Welcome")})
    @Issue("COA-8500")
    @DisplayName("Smoke: Logout returns the user to Welcome")
    @Description("Opens Profile for the existing-progress account, logs out, and verifies that Welcome is displayed again.")
    @Step("Start test testLogoutReturnsToWelcome")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testLogoutReturnsToWelcome() {
        requireIOSPlatform();

        CoachFlowPageObject coachFlow = authorizedCoachFlow();
        coachFlow.openProfile();
        coachFlow.logOut();
        coachFlow.waitForStartScreen();
    }

    @Test
    @Features(value = {@Feature(value = "Kegel"), @Feature(value = "Player"), @Feature(value = "Safe exit")})
    @Issue("COA-8087")
    @DisplayName("Smoke: Kegel player starts, controls respond, and exits safely")
    @Description("Uses the deterministic Kegel fixture, completes the default stretching video, closes the stretching completion popup, verifies the Kegel player controls, and exits through the safe confirmation path.")
    @Step("Start test testKegelPlayerFlow")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testKegelPlayerFlow() {
        requireIOSPlatform();

        CoachFlowPageObject coachFlow = authorizedCoachFlow(TestData.kegelPlayerAccount());
        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);
        dailyPlan.openTodayTab();
        dailyPlan.openKegelExerciseFromDailyPlan();
        dailyPlan.assertKegelStartScreenIsDisplayed();
        dailyPlan.startKegelExercise();
        dailyPlan.assertKegelPlayerIsDisplayed();
        dailyPlan.pressAllKegelPlayerControls();
        dailyPlan.exitKegelExercisePlayer();
        dailyPlan.assertDailyPracticeIsDisplayed();
    }

    private CoachFlowPageObject authorizedCoachFlow() {
        return authorizedCoachFlow(TestData.existingProgressAccount());
    }

    private CoachFlowPageObject authorizedCoachFlow(TestData.TestAccount account) {
        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);
        TestData.deterministicFixture();
        coachFlow.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        return coachFlow;
    }

    private void requireCleanInstallConfiguration() {
        Assert.assertTrue(
                "P0 clean-install smoke requires -Dios.fullReset=true and -Dios.noReset=false",
                Platform.getInstance().isCleanInstallConfigured()
        );
        Assert.assertTrue(
                "P0 clean-install smoke requires -Dios.app=/path/to/The-Coach.app or IOS_APP",
                Platform.getInstance().getIOSAppPath() != null && !Platform.getInstance().getIOSAppPath().trim().isEmpty()
        );
        Assert.assertTrue(
                "P0 clean-install smoke requires -Dios.onboarding.steps=id:<step1>,id:<step2>,... or IOS_ONBOARDING_STEPS",
                Platform.getInstance().getIOSOnboardingStepLocators().length > 0
        );
    }
}
