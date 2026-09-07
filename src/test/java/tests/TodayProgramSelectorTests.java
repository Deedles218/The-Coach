package tests;

import io.qameta.allure.*;
import lib.*;
import lib.ui.CoachFlowPageObject;
import lib.ui.ExplorePageObject;
import lib.ui.factories.*;
import lib.ui.ios.iOSProgramSelectorPageObject;
import org.junit.*;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.OutputType;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import org.junit.runner.Description;

@Epic("Today")
@Feature("Program selector")
public class TodayProgramSelectorTests extends CoreTestCase {
    private iOSProgramSelectorPageObject today;
    private String activeProgram;
    private String sourceDay;
    private String sourceProgress;

    @Override protected void cleanupSessionForNextTest() {
        if (today != null) today.dismissSelectorIfOpen();
        super.cleanupSessionForNextTest();
    }

    private void prepare() {
        prepare("COA-8235");
    }

    private void prepare(String caseKey) {
        Assume.assumeTrue("Selector implementation requires iOS", Platform.getInstance().isIOS());
        TestData.TestAccount account = TestData.testModelAccount(caseKey);
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        today = new iOSProgramSelectorPageObject(driver);
        today.openTodayTab();
        activeProgram = today.getActiveProgramName();
    }

    private void prepareProgramChange(String caseKey) throws Exception {
        prepare(caseKey);
        String expectedUid = System.getenv("COACH_" + caseKey.replace("-", "") + "_UID");
        Assert.assertNotNull("Program changes require a provisioned dedicated UID", expectedUid);
        Assert.assertEquals("Wrong fixture identity; no program changes allowed", expectedUid, SimulatorTestIdentity.currentUid());
        // Recover only our known pair after an interrupted earlier run.
        if (today.getActiveProgramName().equalsIgnoreCase("Keep It Hard")) {
            today.openProgramSelector();
            today.selectProgram("Last Longer");
            today.waitForSelectedProgram("Last Longer");
        }
        today.assertActiveProgramMatches("Last Longer");
        sourceDay = today.getCurrentDayLabel();
        sourceProgress = today.getProgramProgressValue();
        Assert.assertEquals("Selection fixture must not accumulate progress", "0%", sourceProgress.replaceAll("\\s+", ""));
    }

    private void restoreSourceProgram() {
        today.dismissSelectorIfOpen();
        if (!today.getActiveProgramName().equalsIgnoreCase("Last Longer")) {
            today.openProgramSelector();
            today.selectProgram("Last Longer");
        }
        today.waitForSelectedProgram("Last Longer");
        Assert.assertEquals("Original program day was not restored", sourceDay, today.getCurrentDayLabel());
        Assert.assertEquals("Original program progress was not restored", sourceProgress, today.getProgramProgressValue());
        Allure.addAttachment("Verified selector fixture restoration", "Last Longer; " + sourceDay + "; " + sourceProgress);
    }

    private void captureBeforeRestoration(Throwable failure) {
        captureFailureArtifacts(failure, Description.createTestDescription(getClass(), "selectorBeforeRestoration"));
    }

    @Test @Issue("COA-8232")
    public void testCoa8232SelectingProgramUpdatesToday() throws Exception {
        prepareProgramChange("COA-8232");
        RestoringTestAction.run(() -> {
            try {
                today.openProgramSelector();
                String uid = SimulatorTestIdentity.currentUid();
                try (SimulatorAnalyticsEvidence analytics = new SimulatorAnalyticsEvidence(
                        "COA-8232", "ProgramModalSelect", uid, "keep_it_hard");
                     SimulatorModalMotion recording = new SimulatorModalMotion("COA-8232")) {
                    long actionBoundary = System.currentTimeMillis();
                    today.selectProgram("Keep It Hard");
                    today.waitForSelectedProgram("Keep It Hard");
                    Path frames = recording.finishRecording();
                    analytics.assertObserved();
                    Map<String,Object> content = SimulatorProgramContent.read("COA-8232", "keep_it_hard", actionBoundary);
                    today.assertSelectedProgramContent(content);
                    SimulatorProgramLoadingEvidence.assertLoaderAppearedAndDisappeared(frames);
                }
            } catch (Exception | AssertionError failure) {
                captureBeforeRestoration(failure);
                throw failure;
            }
        }, this::restoreSourceProgram);
    }

    /** Separate infrastructure check: a real UI change must be undone even if the test fails. */
    @Test
    public void testProgramChangeFixtureRestoresAfterFailure() throws Exception {
        prepareProgramChange("COA-8232");
        AssertionError injected = new AssertionError("Deliberate failure after selecting destination program");
        try {
            RestoringTestAction.run(() -> {
                today.openProgramSelector();
                today.selectProgram("Keep It Hard");
                today.waitForSelectedProgram("Keep It Hard");
                throw injected;
            }, this::restoreSourceProgram);
            Assert.fail("Injected failure was swallowed");
        } catch (AssertionError failure) {
            Assert.assertSame("Unexpected failure during fixture validation", injected, failure);
            Assert.assertEquals("Restoration also failed", 0, failure.getSuppressed().length);
        }
        today.assertActiveProgramMatches("Last Longer");
        Assert.assertEquals(sourceDay, today.getCurrentDayLabel());
        Assert.assertEquals(sourceProgress, today.getProgramProgressValue());
    }

    @Test @Issue("COA-8231") @Issue("COA-8253")
    public void testCoa8231ActiveProgramIsFirstAfterSwitch() throws Exception {
        prepareProgramChange("COA-8231");
        RestoringTestAction.run(() -> {
            try {
                today.openProgramSelector();
                today.assertActiveProgramIsFirst("Last Longer");
                SimulatorModalMotion.assertOrangeOutline(((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES),
                        today.firstCardBounds(), driver.manage().window().getSize().width);
                try (SimulatorModalMotion recording = new SimulatorModalMotion("COA-8231")) {
                    today.selectProgram("Keep It Hard");
                    today.waitForSelectedProgram("Keep It Hard");
                    recording.finishRecording();
                }
                today.openProgramSelector();
                today.assertActiveProgramIsFirst("Keep It Hard");
                SimulatorModalMotion.assertOrangeOutline(((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES),
                        today.firstCardBounds(), driver.manage().window().getSize().width);
                today.closeProgramSelector();
            } catch (Exception | AssertionError failure) {
                captureBeforeRestoration(failure);
                throw failure;
            }
        }, this::restoreSourceProgram);
    }

    @Test @Issue("COA-8228")
    public void testCoa8228SwipeDownClosesSelector() throws Exception {
        prepare();
        today.openProgramSelector();
        Rectangle card = today.firstCardBounds();
        try (SimulatorModalMotion motion = new SimulatorModalMotion("COA-8228")) {
            today.swipeSelectorDown();
            motion.assertVerticalMotion(card, driver.manage().window().getSize().width, false);
        }
    }

    @Test @Issue("COA-8230")
    public void testCoa8230ProgramsScrollVertically() {
        prepare();
        today.openProgramSelector();
        today.assertProgramsScrollVertically();
        today.closeProgramSelector();
        today.assertDailyPlanDaySwitcherIsDisplayed();
    }

    @Test @Issue("COA-8227") @Issue("COA-8229")
    public void testCoa8227OpensSelectorWithAnalytics() throws Exception {
        prepare();
        // Identity is read only after the existing fixture login verifies its email.
        String uid = SimulatorTestIdentity.currentUid();
        try (SimulatorAnalyticsEvidence analytics = new SimulatorAnalyticsEvidence("COA-8227", "ProgramModalOpen", uid);
             SimulatorModalMotion motion = new SimulatorModalMotion("COA-8227")) {
            today.openProgramSelector();
            Rectangle card = today.firstCardBounds();
            motion.assertVerticalMotion(card, driver.manage().window().getSize().width, true);
            analytics.assertObserved();
        }
        today.assertActiveProgramIsFirst(activeProgram);
        SimulatorModalMotion.assertOrangeOutline(((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES),
                today.firstCardBounds(), driver.manage().window().getSize().width);
        today.assertProgramsScrollVertically();
        // Reuse COA-8235's collection reader and membership oracle.
        today.closeProgramSelector();
        today.openProgramSelector();
        List<String> programs = today.getProgramNamesFromSelector();
        today.closeProgramSelector();
        today.openProgramSelector();
        Rectangle card = today.firstCardBounds();
        try (SimulatorModalMotion motion = new SimulatorModalMotion("COA-8229")) {
            today.closeProgramSelector();
            motion.assertVerticalMotion(card, driver.manage().window().getSize().width, false);
        }
        ExplorePageObject explore = ExplorePageObjectFactory.get(driver);
        explore.openExploreTab();
        TestModelAutomationTests.assertProgramListsMatch(programs, explore.getProgramNamesFromExplore());
        today.openTodayTab();
    }
}
