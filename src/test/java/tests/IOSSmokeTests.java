package tests;

import io.qameta.allure.*;
import lib.CoreTestCase;
import lib.Platform;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import lib.ui.factories.OnboardingPageObjectFactory;
import org.junit.Test;

/** The five AndroidSmokeTests scenarios, using the existing iOS Page Objects. */
@Epic("The Coach iOS Smoke")
public class IOSSmokeTests extends CoreTestCase {
    @Override protected boolean isPlatformSupported() { return Platform.getInstance().isIOS(); }

    @Test @Issue("COA-7948") public void test01EmptyEmailKeepsContinueDisabled() {
        CoachFlowPageObject coach = coachFlow();
        coach.ensureLoggedOutOnStartScreen();
        coach.waitForStartScreen();
        coach.openLoginFlow();
        coach.assertLoginContinueButtonIsDisabled();
        coach.assertDisabledLoginContinueIsNoOp();
        coach.returnFromLoginFlowToStartScreen();
    }
    @Test @Issue("COA-7935") public void test02EmailAndOtpOpenAuthorizedToday() {
        CoachFlowPageObject coach = authorizedCoach();
        coach.assertAuthorizedDashboardIsDisplayed();
        coach.openToday();
    }
    @Test @Issue("COA-8178") public void test03MainThreeTabNavigation() {
        CoachFlowPageObject coach = authorizedCoach();
        coach.assertMainTabsAreDisplayed();
        coach.openToday();
        coach.openExplore();
        coach.openShop();
    }
    @Test @Issue("COA-8087") public void test04DailyPracticeOpensCustomKegelStartScreen() {
        authorizedCoach(TestData.kegelPlayerAccount()).openToday();
        DailyPlanPageObject today = DailyPlanPageObjectFactory.get(driver);
        today.openTodayTab();
        today.assertDailyPlanDaySwitcherIsDisplayed();
        today.assertDailyPracticeIsDisplayed();
        today.openKegelExerciseFromDailyPlan();
        today.assertKegelStartScreenIsDisplayed();
        today.closeKegelExerciseFlowIfPresent();
    }
    @Test @Issue("COA-8500") public void test05ProfileAndLogout() {
        CoachFlowPageObject coach = authorizedCoach();
        coach.openToday();
        coach.openProfile();
        coach.assertProfileSmokeContentIsDisplayed();
        coach.logOut();
        coach.waitForStartScreen();
    }
    private CoachFlowPageObject authorizedCoach() {
        return authorizedCoach(TestData.existingProgressAccount());
    }
    private CoachFlowPageObject authorizedCoach(TestData.TestAccount account) {
        CoachFlowPageObject coach = coachFlow();
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        return coach;
    }
    private CoachFlowPageObject coachFlow() {
        OnboardingPageObjectFactory.get(driver).closePaywallsAndPopups();
        return CoachFlowPageObjectFactory.get(driver);
    }
}
