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
    private boolean preservePreparedProfile;
    private ProgramSelectionState activationBaseline;

    @Before @Override public void setUp() throws Exception {
        if (Boolean.getBoolean("coach.selector.preparedProfile")) PreparedMobileProfile.assertPreservationAllowed();
        super.setUp();
    }

    @Override protected void cleanupSessionForNextTest() {
        if (!preservePreparedProfile) super.cleanupSessionForNextTest();
    }

    private void prepare() throws Exception {
        AndroidCoachFlowPageObject coach=new AndroidCoachFlowPageObject(driver);
        TestData.TestAccount account=TestData.existingProgressAccount();
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(),account.getOtp());
        Assert.assertEquals("Wrong selector account",System.getenv("COACH_EXPECTED_ACCOUNT_UID"),AndroidModuleEvidence.currentUid());
        captureBaseline();
    }

    private void captureBaseline() {
        today=new AndroidProgramSelectorPageObject(driver); today.openTodayTab();
        original=today.getActiveProgramName();day=today.getCurrentDayLabel();progress=today.getProgramProgressValue();
        if(AndroidProgramSelectorPageObject.isActivationFlow()) {
            try {activationBaseline=ProgramSelectionApi.read();}
            catch(Exception failure) {throw new AssertionError("Cannot read the bound activation selector fixture",failure);}
        }
    }
    private void restore() {
        today.dismissSelectorIfOpen();
        today.openTodayTab();
        if(!original.equalsIgnoreCase(today.getActiveProgramName())) { today.openProgramSelector();today.selectProgram(original); }
        today.waitForSelectedProgram(original);
        Assert.assertEquals("Original stage changed",day,today.getCurrentDayLabel());
        Assert.assertEquals("Original progress changed",progress,today.getProgramProgressValue());
        if(activationBaseline!=null) {
            try {
                ProgramSelectionState restored=ProgramSelectionApi.read();
                restored.assertSameRoles(activationBaseline);restored.assertSameProgress(activationBaseline);
                restored.assertActive(activationBaseline.active());
            } catch(Exception failure) {throw new AssertionError("Cannot verify restored program state",failure);}
        }
    }
    private void scenario(RestoringTestAction.CheckedAction action) throws Exception {
        scenario(action, false);
    }

    private void scenario(RestoringTestAction.CheckedAction action, boolean readOnly) throws Exception {
        if (readOnly && Boolean.getBoolean("coach.selector.preparedProfile")) {
            preservePreparedProfile = true;
            PreparedMobileProfile.verifyFromToday(new AndroidCoachFlowPageObject(driver));
            captureBaseline();
        } else {
            prepare();
        }
        RestoringTestAction.run(()-> {
            try { action.run(); }
            catch(Exception|AssertionError e) { captureFailureArtifacts(e,Description.createTestDescription(getClass(),"androidSelectorBeforeRestore"));throw e; }
        },this::restore);
    }
    private void outline() throws Exception {
        SimulatorModalMotion.assertOrangeOutline(((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES),today.firstCardBounds(),driver.manage().window().getSize().width);
    }
    private Set<String> savedProgramNames() {
        Set<String> result=new LinkedHashSet<String>();
        if(activationBaseline!=null)for(Map<String,Object> row:activationBaseline.programs())result.add((String)row.get("title"));
        return result;
    }
    private void assertSelectionAppearance(String active) throws Exception {
        if(activationBaseline!=null)
            today.assertActivationSelection((String)activationBaseline.main().get("title"),savedProgramNames(),active);
        else {today.assertActiveProgramIsFirst(active);outline();}
    }
    @Test @Issue("COA-8227") @Issue("COA-8229")
    @io.qameta.allure.junit4.DisplayName("Program selector opens, marks the active program, and closes (updated)")
    public void testOpensAndClosesSelectorWithAnalyticsAndMotion() throws Exception {
        scenario(()-> {
            try(AndroidAnalyticsEvidence event=new AndroidAnalyticsEvidence("COA-8227","ProgramModalOpen",AndroidModuleEvidence.currentUid());
                AndroidModalMotion motion=new AndroidModalMotion(driver,"COA-8227")) {
                today.openProgramSelector();
                motion.assertVerticalMotion(today.selectorMotionBounds(),driver.manage().window().getSize().width,true,AndroidProgramSelectorPageObject.isActivationFlow());
                event.assertObserved();
            }
            assertSelectionAppearance(original);
            Rectangle card=today.selectorMotionBounds();
            try(AndroidModalMotion motion=new AndroidModalMotion(driver,"COA-8229")) {
                today.closeProgramSelector();motion.assertVerticalMotion(card,driver.manage().window().getSize().width,false,AndroidProgramSelectorPageObject.isActivationFlow());
            }
        });
    }
    @Test @Issue("COA-8228")
    public void testSwipeDownClosesSelectorWithMotion() throws Exception {
        scenario(()-> {
            today.openProgramSelector();Rectangle card=today.selectorMotionBounds();
            try(AndroidModalMotion motion=new AndroidModalMotion(driver,"COA-8228")) {
                today.swipeSelectorDown();motion.assertVerticalMotion(card,driver.manage().window().getSize().width,false,AndroidProgramSelectorPageObject.isActivationFlow());
            }
        }, true);
    }
    @Test @Issue("COA-8230")
    @io.qameta.allure.junit4.DisplayName("Saved programs are reachable with the required vertical scrolling (updated)")
    public void testProgramsScrollVertically() throws Exception {
        scenario(()-> { today.openProgramSelector();today.assertProgramsScrollVertically(savedProgramNames());today.closeProgramSelector(); }, true);
    }
    @Test @Issue("COA-8231") @Issue("COA-8253")
    @io.qameta.allure.junit4.DisplayName("Program switching preserves the configured selector roles and active mark (updated)")
    public void testActiveProgramIsFirstAfterSwitch() throws Exception {
        scenario(()-> {
            Assert.assertEquals("Selector baseline must be Last Longer","Last Longer",original);
            today.openProgramSelector();assertSelectionAppearance(original);
            today.selectProgram("Keep It Hard");
            today.openProgramSelector();assertSelectionAppearance("Keep It Hard");today.closeProgramSelector();
            if(activationBaseline!=null) {
                ProgramSelectionState after=ProgramSelectionApi.read();after.assertSameRoles(activationBaseline);
                after.assertSameProgress(activationBaseline);after.assertActive("keep_it_hard");
            }
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
