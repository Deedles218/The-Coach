package tests;

import lib.*;
import lib.ui.android.*;
import io.qameta.allure.*;
import org.junit.*;
import org.junit.runner.Description;
import org.openqa.selenium.*;
import java.util.*;

@Epic("Android Today") @Feature("Program selector")
public class AndroidProgramSelectorTests extends AndroidTestCase {
    private AndroidProgramSelectorPageObject today;
    private String original, day, progress;
    private void prepare() throws Exception {
        AndroidCoachFlowPageObject coach=new AndroidCoachFlowPageObject(driver);
        TestData.TestAccount account=TestData.existingProgressAccount();
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(),account.getOtp());
        Assert.assertEquals("Wrong selector account",System.getenv("COACH_EXPECTED_ACCOUNT_UID"),AndroidModuleEvidence.currentUid());
        today=new AndroidProgramSelectorPageObject(driver); today.openTodayTab();
        original=today.getActiveProgramName();day=today.getCurrentDayLabel();progress=today.getProgramProgressValue();
    }
    private void restore() {
        today.dismissSelectorIfOpen();
        today.openTodayTab();
        if(!original.equalsIgnoreCase(today.getActiveProgramName())) { today.openProgramSelector();today.selectProgram(original); }
        today.waitForSelectedProgram(original);
        Assert.assertEquals("Original stage changed",day,today.getCurrentDayLabel());
        Assert.assertEquals("Original progress changed",progress,today.getProgramProgressValue());
    }
    private void scenario(RestoringTestAction.CheckedAction action) throws Exception {
        prepare();
        RestoringTestAction.run(()-> {
            try { action.run(); }
            catch(Exception|AssertionError e) { captureFailureArtifacts(e,Description.createTestDescription(getClass(),"androidSelectorBeforeRestore"));throw e; }
        },this::restore);
    }
    private void outline() throws Exception {
        SimulatorModalMotion.assertOrangeOutline(((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES),today.firstCardBounds(),driver.manage().window().getSize().width);
    }
    @Test @Issue("COA-8227") @Issue("COA-8229")
    public void testOpensAndClosesSelectorWithAnalyticsAndMotion() throws Exception {
        scenario(()-> {
            try(AndroidAnalyticsEvidence event=new AndroidAnalyticsEvidence("COA-8227","ProgramModalOpen",AndroidModuleEvidence.currentUid());
                AndroidModalMotion motion=new AndroidModalMotion(driver,"COA-8227")) {
                today.openProgramSelector();
                motion.assertVerticalMotion(today.firstCardBounds(),driver.manage().window().getSize().width,true);
                event.assertObserved();
            }
            today.assertActiveProgramIsFirst(original);outline();
            Rectangle card=today.firstCardBounds();
            try(AndroidModalMotion motion=new AndroidModalMotion(driver,"COA-8229")) {
                today.closeProgramSelector();motion.assertVerticalMotion(card,driver.manage().window().getSize().width,false);
            }
        });
    }
    @Test @Issue("COA-8228")
    public void testSwipeDownClosesSelectorWithMotion() throws Exception {
        scenario(()-> {
            today.openProgramSelector();Rectangle card=today.firstCardBounds();
            try(AndroidModalMotion motion=new AndroidModalMotion(driver,"COA-8228")) {
                today.swipeSelectorDown();motion.assertVerticalMotion(card,driver.manage().window().getSize().width,false);
            }
        });
    }
    @Test @Issue("COA-8230")
    public void testProgramsScrollVertically() throws Exception {
        scenario(()-> { today.openProgramSelector();today.assertProgramsScrollVertically();today.closeProgramSelector(); });
    }
    @Test @Issue("COA-8231") @Issue("COA-8253")
    public void testActiveProgramIsFirstAfterSwitch() throws Exception {
        scenario(()-> {
            Assert.assertEquals("Selector baseline must be Last Longer","Last Longer",original);
            today.openProgramSelector();today.assertActiveProgramIsFirst(original);outline();
            today.selectProgram("Keep It Hard");
            today.openProgramSelector();today.assertActiveProgramIsFirst("Keep It Hard");outline();today.closeProgramSelector();
        });
    }
    @Test @Issue("COA-8232")
    public void testSelectingProgramUpdatesContentAndAnalytics() throws Exception {
        scenario(()-> {
            Assert.assertEquals("Selector baseline must be Last Longer","Last Longer",original);
            today.openProgramSelector();
            try(AndroidAnalyticsEvidence event=new AndroidAnalyticsEvidence("COA-8232","ProgramModalSelect",AndroidModuleEvidence.currentUid(),"keep_it_hard");
                AndroidModalMotion motion=new AndroidModalMotion(driver,"COA-8232")) {
                long boundary=System.currentTimeMillis();
                today.selectProgramWithLoadingCheck("Keep It Hard");
                motion.finishRecording();event.assertObserved();
                today.assertSelectedProgramContent(AndroidProgramContent.read("keep_it_hard",boundary));
            }
        });
    }
    @Test public void testProgramChangeRestoresAfterFailure() throws Exception {
        prepare();Assert.assertEquals("Last Longer",original);
        AssertionError injected=new AssertionError("Deliberate fixture restoration check");
        try {
            RestoringTestAction.run(()->{today.openProgramSelector();today.selectProgram("Keep It Hard");throw injected;},this::restore);
            Assert.fail("Injected failure was swallowed");
        } catch(AssertionError error) { Assert.assertSame(injected,error);Assert.assertEquals(0,error.getSuppressed().length); }
    }
    @Test @Issue("COA-8235")
    public void testTodayProgramsMatchExplore() throws Exception {
        scenario(()-> {
            today.openProgramSelector();List<String> names=today.getProgramNamesFromSelector();today.closeProgramSelector();
            AndroidExplorePageObject explore=new AndroidExplorePageObject(driver);explore.openExploreTab();
            TestModelAutomationTests.assertProgramListsMatch(names,explore.getProgramNamesFromExplore());today.openTodayTab();
        });
    }
}
