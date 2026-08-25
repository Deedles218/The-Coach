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
import lib.AndroidTestCase;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.android.AndroidDailyPlanPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Android parity smoke checks for the current 1.40.x native application.
 *
 * The suite deliberately uses AndroidTestCase so an iOS run is reported as a
 * platform skip, not as a green test method that returned before making an
 * assertion.
 */
@Epic(value = "The Coach Android Smoke")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AndroidSmokeTests extends AndroidTestCase {
    @Test
    @Features({@Feature("Welcome"), @Feature("Login"), @Feature("Validation")})
    @Issue("COA-7948")
    @DisplayName("Android smoke: empty email keeps Continue disabled")
    @Description("Opens the Android Login form from Welcome, verifies the disabled Continue state and verifies a disabled tap is a no-op.")
    @Step("Start test test01EmptyEmailKeepsContinueDisabled")
    @Severity(SeverityLevel.BLOCKER)
    public void test01EmptyEmailKeepsContinueDisabled() {
        CoachFlowPageObject coachFlow = coachFlow();
        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.waitForStartScreen();
        coachFlow.openLoginFlow();
        coachFlow.assertLoginContinueButtonIsDisabled();
        coachFlow.assertDisabledLoginContinueIsNoOp();
        coachFlow.returnFromLoginFlowToStartScreen();
    }

    @Test
    @Features({@Feature("Authorization"), @Feature("OTP"), @Feature("Today")})
    @Issue("COA-7935")
    @DisplayName("Android smoke: email and OTP open the authorized Today screen")
    @Description("Uses secret-backed test data, authenticates the Android build and verifies the authorized Today entry point.")
    @Step("Start test test02EmailAndOtpOpenAuthorizedToday")
    @Severity(SeverityLevel.BLOCKER)
    public void test02EmailAndOtpOpenAuthorizedToday() {
        CoachFlowPageObject coachFlow = authorizedCoachFlow();
        coachFlow.assertAuthorizedDashboardIsDisplayed();
        coachFlow.openToday();
    }

    @Test
    @Features({@Feature("Navigation"), @Feature("Main tabs")})
    @Issue("COA-8178")
    @DisplayName("Android smoke: Today, Explore and Shop are reachable")
    @Description("Verifies the current three-tab Android navigation and opens each destination with a single semantic tap.")
    @Step("Start test test03MainThreeTabNavigation")
    @Severity(SeverityLevel.CRITICAL)
    public void test03MainThreeTabNavigation() {
        CoachFlowPageObject coachFlow = authorizedCoachFlow();
        coachFlow.assertMainTabsAreDisplayed();
        coachFlow.openToday();
        coachFlow.openExplore();
        coachFlow.openShop();
    }

    @Test
    @Features({@Feature("Today"), @Feature("Daily Practice"), @Feature("Kegel")})
    @Issue("COA-8087")
    @DisplayName("Android smoke: Daily Practice opens the Custom Kegel start screen")
    @Description("Verifies the Android Daily Practice card is visible, enabled and tappable, then verifies the start screen and safe close control.")
    @Step("Start test test04DailyPracticeOpensCustomKegelStartScreen")
    @Severity(SeverityLevel.BLOCKER)
    public void test04DailyPracticeOpensCustomKegelStartScreen() {
        CoachFlowPageObject coachFlow = authorizedCoachFlow();
        coachFlow.openToday();

        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertTrue(
                "Android smoke requires the Android Daily Plan page object",
                dailyPlan instanceof AndroidDailyPlanPageObject
        );
        AndroidDailyPlanPageObject androidDailyPlan = (AndroidDailyPlanPageObject) dailyPlan;
        androidDailyPlan.openTodayTab();
        androidDailyPlan.assertDailyPlanDaySwitcherIsDisplayed();
        androidDailyPlan.assertDailyPracticeIsDisplayed();
        androidDailyPlan.openKegelExerciseFromDailyPlan();
        androidDailyPlan.assertKegelStartScreenIsDisplayed();
        androidDailyPlan.closeKegelExerciseFlowIfPresent();
    }

    @Test
    @Features({@Feature("Profile"), @Feature("Logout"), @Feature("Welcome")})
    @Issue("COA-8500")
    @DisplayName("Android smoke: Profile content is visible and Logout returns to Welcome")
    @Description("Opens the Android Profile from Today, verifies progress/settings controls and verifies the confirmed logout destination.")
    @Step("Start test test05ProfileAndLogout")
    @Severity(SeverityLevel.BLOCKER)
    public void test05ProfileAndLogout() {
        CoachFlowPageObject coachFlow = authorizedCoachFlow();
        coachFlow.openToday();
        coachFlow.openProfile();
        coachFlow.assertProfileSmokeContentIsDisplayed();
        coachFlow.logOut();
        coachFlow.waitForStartScreen();
    }

    private CoachFlowPageObject coachFlow() {
        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Android Coach page object is not available", coachFlow);
        return coachFlow;
    }

    private CoachFlowPageObject authorizedCoachFlow() {
        TestData.TestAccount account = TestData.existingProgressAccount();
        CoachFlowPageObject coachFlow = coachFlow();
        coachFlow.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        return coachFlow;
    }
}
