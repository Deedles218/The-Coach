package tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.MobileCaseFixture;
import lib.ModuleStage;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.SystemAppearanceState;
import lib.TestData;
import lib.UpdateEvidence;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyContentCompletionPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.ModulesPageObject;
import lib.ui.SystemAppearancePageObject;
import lib.ui.android.AndroidCoachFlowPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import lib.ui.factories.ModulesPageObjectFactory;
import lib.ui.factories.OnboardingPageObjectFactory;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.List;

@Feature("Next reviewed mobile TMS cases")
public class MobileNextCasesTests extends CoreTestCase {
    @Rule public TestName selectedMethod = new TestName();
    private MobileCaseFixture fixture;
    private String caseKey;

    @Before @Override public void setUp() throws Exception {
        caseKey = getClass().getMethod(selectedMethod.getMethodName()).getAnnotation(Issue.class).value();
        fixture = MobileCaseFixture.load(caseKey);
        validatePrerequisites(); // Fail before creating a session or altering application state.
        super.setUp();
        requireMobilePlatform();
        UpdateEvidence.attachInstalledBuild(driver);
    }

    @Test @Issue("COA-7917")
    @DisplayName("COA-7917 System dark mode preserves Today rendering and controls")
    public void testCoa7917TodayKeepsItsOwnAppearance() throws Exception {
        if (usesPreparedProfile()) {
            PreparedMobileProfile.verifyFromToday(CoachFlowPageObjectFactory.get(driver));
        } else {
            loginDedicatedAccount();
        }
        assertTodayFixture();
        DailyPlanPageObject daily = DailyPlanPageObjectFactory.get(driver);
        String originalProgress = daily.getProgramProgressValue();
        SystemAppearancePageObject today = new SystemAppearancePageObject(driver, fixture);
        try (SystemAppearanceState appearance = SystemAppearanceState.capture(driver)) {
            appearance.setDark(false);
            List<SystemAppearancePageObject.Region> light = today.snapshot();
            appearance.setDark(true);
            assertTodayFixture();
            today.assertUnchanged(light);
        }
        Assert.assertEquals("Appearance test changed account progress", originalProgress, daily.getProgramProgressValue());
    }

    @Test @Issue("COA-7980")
    @DisplayName("COA-7980 Lesson completion survives closing and reopening the app")
    public void testCoa7980LessonCompletionSurvivesRestart() {
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        String email;
        if (usesPreparedProfile()) {
            email = TestData.existingProgressAccountEmail();
            PreparedMobileProfile.verifyFromToday(coach);
        } else {
            email = loginDedicatedAccount().getEmail();
        }
        ModulesPageObject androidModules = null;
        ModuleStage originalStage = null;
        if (Platform.getInstance().isAndroid() && fixture.required("day").startsWith("Stage ")) {
            androidModules = ModulesPageObjectFactory.get(driver);
            androidModules.openTodayTab();
            originalStage = androidModules.position();
        }
        DailyContentCompletionPageObject content = new DailyContentCompletionPageObject(driver);
        content.finishLessonForPersistence(fixture);
        closeAndReopenCoachApplication();

        OnboardingPageObjectFactory.get(driver).closePaywallsAndPopups();
        coach.waitForAuthorizedDashboard();
        coach.openProfile();
        assertRestoredProfileEmail(coach, email);
        if (Platform.getInstance().isAndroid()) {
            coach.returnToMainScreenFromProfile();
            if (androidModules != null) {
                Assert.assertEquals("Cannot reopen the lesson day in another program",
                        fixture.required("program"), androidModules.getActiveProgramName());
                androidModules.restore(originalStage);
            }
        } else coach.openToday();
        content.assertCompletedLessonAfterRestart(fixture);
    }

    private void assertRestoredProfileEmail(CoachFlowPageObject coach, String email) {
        try {
            if (Platform.getInstance().isIOS()) {
                ((iOSCoachFlowPageObject) coach).assertProfileForEmail(email, false);
            } else {
                ((AndroidCoachFlowPageObject) coach).assertAuthenticatedProfileForEmail(email);
            }
        } catch (org.openqa.selenium.TimeoutException | AssertionError mismatch) {
            throw new AssertionError("The opened profile does not belong to the expected test account");
        }
    }

    private TestData.TestAccount loginDedicatedAccount() {
        TestData.TestAccount account = TestData.testModelAccount(caseKey);
        CoachFlowPageObjectFactory.get(driver).ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        return account;
    }

    private boolean usesPreparedProfile() {
        return ("COA-7917".equals(caseKey) && Boolean.getBoolean("coach.coa7917.preparedProfile"))
                || ("COA-7980".equals(caseKey) && Boolean.getBoolean("coach.coa7980.preparedProfile"));
    }

    @Override protected void cleanupSessionForNextTest() {
        if (!usesPreparedProfile()) super.cleanupSessionForNextTest();
    }

    private void assertTodayFixture() {
        DailyPlanPageObject daily = DailyPlanPageObjectFactory.get(driver);
        daily.openTodayTab();
        Assert.assertEquals("Fixture opened another program", fixture.required("program"), daily.getActiveProgramName());
        Assert.assertEquals("Fixture opened another program day", fixture.required("day"), daily.getCurrentDayLabel());
    }

    private void validatePrerequisites() {
        if (usesPreparedProfile()) {
            PreparedMobileProfile.assertPreservationAllowed();
            TestData.existingProgressAccountEmail();
        } else {
            TestData.testModelAccount(caseKey);
        }
        if ("COA-7917".equals(caseKey)) {
            int regions = fixture.positiveInt("appearance.regions");
            Assert.assertTrue("Review at least three stable Today regions", regions >= 3);
            for (int n = 1; n <= regions; n++) fixture.selector("appearance.region." + n);
            if (Platform.getInstance().isAndroid()) lib.AndroidDevice.uiSettingsSerial();
            else {
                String udid = System.getProperty("ios.udid", System.getenv("IOS_UDID"));
                Assert.assertTrue("Appearance changes require an explicit iOS Simulator UDID", udid != null && udid.matches("[A-Fa-f0-9-]{36}"));
            }
        } else if ("COA-7980".equals(caseKey)) {
            fixture.required("content.title");
            fixture.required("content.kindText");
            fixture.required("content.type");
            fixture.relativeSelector("content.kind");
            Assert.assertFalse("Completion helper requires a title without apostrophes", fixture.required("content.title").contains("'"));
        } else {
            throw new IllegalArgumentException("No prepared profile contract for " + caseKey);
        }
    }
}
