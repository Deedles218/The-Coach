package tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.SimulatorTestIdentity;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.ExplorePageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import lib.ui.factories.ExplorePageObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/** Executed as a separate gate by the profile launcher, before dependent tests. */
@Feature("Regression preconditions")
public class IOSRegressionPreflightTests extends CoreTestCase {
    @Override protected boolean isPlatformSupported() { return Platform.getInstance().isIOS(); }

    @Before @Override public void setUp() throws Exception {
        TestData.existingProgressAccount();
        super.setUp();
    }

    @Test
    @DisplayName("Preflight: account, Daily Plan fixture and Explore are ready")
    public void testExistingProgressFixtureAndExplore() throws Exception {
        TestData.TestAccount account = TestData.existingProgressAccount();
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        String expectedUid = System.getenv("COACH_EXPECTED_ACCOUNT_UID");
        if (expectedUid != null && !expectedUid.trim().isEmpty()) {
            assertFixtureIdentity(expectedUid, SimulatorTestIdentity.currentUid());
        }
        DailyPlanPageObject today = DailyPlanPageObjectFactory.get(driver);
        today.openTodayTab();
        Allure.addAttachment("Fixture preflight",
                "accountRole=existing-progress\nexpectedDay=" + TestData.deterministicFixture().getDailyPlanDay()
                        + "\nactualDay=" + today.getCurrentDayLabel()
                        + "\nactualProgram=" + today.getActiveProgramName());
        today.assertCurrentDayMatches(TestData.deterministicFixture().getDailyPlanDay());
        String expectedProgram = System.getenv("COACH_DAILY_PLAN_PROGRAM");
        if (expectedProgram != null && !expectedProgram.trim().isEmpty()) {
            today.assertActiveProgramMatches(expectedProgram);
        }
        today.assertDailyLessonsAreDisplayed();
        today.assertDailyPracticeIsDisplayed();
        assertExploreCatalog();
    }

    @Test
    @DisplayName("Preflight: existing-progress Explore catalog is ready")
    public void testExploreCatalogIsReady() {
        TestData.TestAccount account = TestData.existingProgressAccount();
        CoachFlowPageObjectFactory.get(driver).ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        assertExploreCatalog();
    }

    private void assertExploreCatalog() {
        ExplorePageObject explore = ExplorePageObjectFactory.get(driver);
        explore.openExploreTab();
        explore.assertMainProgramsRecommendedSection();
    }

    static void assertFixtureIdentity(String expectedUid, String actualUid) {
        // assertEquals would expose both identifiers in Maven and Allure failures.
        Assert.assertTrue("Wrong fixture identity", expectedUid != null && !expectedUid.trim().isEmpty()
                && expectedUid.trim().equals(actualUid));
    }
}
