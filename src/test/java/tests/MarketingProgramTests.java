package tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.MarketingProgramPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Epic("The Coach for Him")
@Feature("Marketing program assignment")
public class MarketingProgramTests extends CoreTestCase {
    private CoachFlowPageObject coach;
    private DailyPlanPageObject today;
    private MarketingProgramPageObject marketing;
    private boolean verifiedLinkAccount;

    @Before @Override
    public void setUp() throws Exception {
        requireMobilePlatform();
        PreparedMobileProfile.assertPreservationAllowed();
        Platform platform = Platform.getInstance();
        String app = platform.isAndroid() ? platform.getAndroidAppPackage() : platform.getIOSBundleId();
        Assert.assertEquals("Use The Coach for Him", platform.isAndroid() ? "com.vamapps.thecoach" : "com.vamapps.The-Coach", app);
        Assert.assertTrue("Use a dedicated device or simulator for the marketing fixture",
                Boolean.getBoolean("coach.marketing.dedicatedDevice"));
        super.setUp();
        coach = CoachFlowPageObjectFactory.get(driver);
        today = DailyPlanPageObjectFactory.get(driver);
        marketing = new MarketingProgramPageObject(driver);
    }

    @Test @Issue("COA-9385")
    @DisplayName("COA-9385 The SIAS marketing route opens Sex Is a Skill")
    public void testCoa9385SiasMarketingAssignment() {
        assertLinkedProgram("COA-9385", "Sex Is a Skill", null);
    }

    @Test @Issue("COA-9388")
    @DisplayName("COA-9388 The assigned Squirt program survives a cold restart")
    public void testCoa9388SquirtAssignmentSurvivesRestart() {
        requireCase("COA-9388");
        PreparedMobileProfile.verifyFromToday(coach);
        assertActiveProgram("Squirt");
        String day = today.getCurrentDayLabel();
        String progress = today.getProgramProgressValue();
        Assert.assertFalse("The persistence fixture must expose its current day", day.trim().isEmpty());
        Assert.assertFalse("The persistence fixture must expose its program progress", progress.trim().isEmpty());

        closeAndReopenCoachApplication();
        marketing.waitForRestoredToday();
        PreparedMobileProfile.verifyFromToday(coach);
        assertActiveProgram("Squirt");
        Assert.assertEquals("The assigned program day changed after restart", day, today.getCurrentDayLabel());
        Assert.assertEquals("The assigned program progress changed after restart", progress, today.getProgramProgressValue());
    }

    @Test @Issue("COA-9393")
    @DisplayName("COA-9393 The Squirt marketing route opens Squirt")
    public void testCoa9393SquirtMarketingAssignment() {
        assertLinkedProgram("COA-9393", "Squirt", null);
    }

    @Test @Issue("COA-9394")
    @DisplayName("COA-9394 The Last Longer marketing route opens Last Longer")
    public void testCoa9394LastLongerMarketingAssignment() {
        assertLinkedProgram("COA-9394", "Last Longer", null);
    }

    @Test @Issue("COA-9400")
    @DisplayName("COA-9400 m_sias_new_agr assigns Squirt despite the sias substring")
    public void testCoa9400SquirtClassificationHasPriority() {
        assertLinkedProgram("COA-9400", "Squirt", "m_sias_new_agr");
    }

    @Test @Issue("COA-9404")
    @DisplayName("COA-9404 Choosing Improve sex skills does not replace the assigned Kegar program")
    public void testCoa9404GoalsDoNotOverrideKegarAssignment() {
        requireCase("COA-9404");
        coach.waitForStartScreen();
        marketing.openConfiguredLink("test_kegar_test");
        marketing.finishWithConflictingGoal();
        assertActiveProgram("Kegar");
    }

    private void assertLinkedProgram(String testCase, String expectedProgram, String expectedW2w) {
        requireCase(testCase);
        TestData.existingProgressAccountEmail();
        coach.waitForStartScreen();
        if (expectedW2w == null) {
            marketing.openConfiguredLink();
        } else {
            marketing.openConfiguredLink(expectedW2w);
        }
        marketing.waitForTodayOrSignIn();
        PreparedMobileProfile.verifyFromToday(coach);
        verifiedLinkAccount = true;
        assertActiveProgram(expectedProgram);
    }

    private void assertActiveProgram(String expectedProgram) {
        today.openTodayTab();
        Assert.assertEquals("Today shows a different program for this marketing fixture",
                expectedProgram, today.getActiveProgramName());
    }

    private static void requireCase(String testCase) {
        Assert.assertEquals("Select the fixture for the exact Xray case with -Dcoach.marketing.case",
                testCase, System.getProperty("coach.marketing.case"));
        Assert.assertTrue("Use an explicitly dedicated marketing QA account (disposable anonymous fixture for COA-9404)",
                Boolean.getBoolean("coach.marketing.dedicatedAccount"));
    }

    @Override
    protected void cleanupSessionForNextTest() {
        // Never log out a profile that the app opened unexpectedly. The
        // prepared persistence fixture and new-user journey own their state.
        if (verifiedLinkAccount) coach.ensureLoggedOutOnStartScreen();
    }
}
