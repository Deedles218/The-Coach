package tests;

import io.qameta.allure.*;
import lib.*;
import lib.ui.android.AndroidMyProgramsPageObject;
import lib.ui.android.AndroidProgramSelectionPageObject;
import org.junit.*;
import java.util.*;

/** A preserved, UID/backend/APK-bound preprod fixture; never seeds or completes content. */
@Epic("COA-9583 Program activation")
@Feature("Program Day/Stage and independent progress")
public class AndroidProgramProgressTests extends CoreTestCase {
    private AndroidMyProgramsPageObject today;
    @Before @Override public void setUp() throws Exception {
        Assert.assertTrue("Run on Android", Platform.getInstance().isAndroid());
        Assert.assertTrue("Progress tests require an installed app, noReset=true and fullReset=false",
                Platform.getInstance().isAndroidAttachConfigured());
        super.setUp();
        // A preserved fixture may have an expired SDK token after a long idle period.
        // Normal startup refreshes it; never refresh or replace credentials in the reader.
        closeAndReopenCoachApplication();
        today = new AndroidMyProgramsPageObject(driver);
        new AndroidProgramSelectionPageObject(driver).closeStartupPrompts();
        ProgramSelectionApi.assertDedicatedProfile("COA-9794");
    }
    @Override protected void cleanupSessionForNextTest() { }

    @Test @Issue("COA-9794")
    public void allSavedProgramsUseTheDayFormulaAndDisplayItsPercentage() throws Exception {
        ProgramSelectionState original = ProgramSelectionApi.read();
        original.assertAllProgressFormulas();
        try {
            today.openProgramSelector();
            today.assertMatchesState(original);
            today.closeProgramSelector();
        } finally {
            ProgramSelectionApi.read().assertExactlyEquals(original);
        }
    }

    @Test @Issues({@Issue("COA-9794"), @Issue("COA-9853"), @Issue("COA-9833")})
    public void dayOrStageMatchApiAndSurviveSwitchingAndColdLaunch() throws Exception {
        ProgramSelectionState original = ProgramSelectionApi.read();
        Assert.assertNotNull("Prepare Main Journey", original.main());
        Assert.assertFalse("Prepare at least one Secondary", original.secondary().isEmpty());
        String main = original.mainId(), secondary = original.secondaryIds().iterator().next();
        Assert.assertNotEquals("Prepare distinguishable saved positions", original.progressLabel(main), original.progressLabel(secondary));
        Assert.assertTrue("Prepare nonzero saved progress in Main or the selected Secondary",
                Arrays.asList(main, secondary).stream().anyMatch(id ->
                        ((Number) original.program(id).get("progress_percent")).doubleValue() > 0));
        try {
            for (String id : Arrays.asList(main, secondary, main)) {
                if (!id.equals(ProgramSelectionApi.read().active())) {
                    today.openProgramSelector(); today.assertMatchesState(ProgramSelectionApi.read());
                    today.selectSavedProgram((String) original.program(id).get("title"), false);
                }
                assertDaily(original, id);
                ProgramSelectionState current = ProgramSelectionApi.read();
                current.assertSameRoles(original); current.assertSameProgress(original); current.assertActive(id);
                today.openProgramSelector(); today.assertMatchesState(current); today.closeProgramSelector();
            }
            closeAndReopenCoachApplication();
            new AndroidProgramSelectionPageObject(driver).closeStartupPrompts();
            assertDaily(original, main);
            ProgramSelectionState reopened = ProgramSelectionApi.read();
            reopened.assertSameRoles(original); reopened.assertSameProgress(original); reopened.assertActive(main);
            today.openProgramSelector(); today.assertMatchesState(reopened); today.closeProgramSelector();
        } finally {
            if (!original.active().equals(ProgramSelectionApi.read().active())) {
                today.openProgramSelector(); today.selectSavedProgram((String) original.program(original.active()).get("title"), false);
            }
            ProgramSelectionApi.read().assertExactlyEquals(original);
        }
    }
    private void assertDaily(ProgramSelectionState state, String id) {
        Assert.assertEquals("Wrong active Daily Plan", state.program(id).get("title"), today.getActiveProgramName());
        Assert.assertEquals("Daily Plan differs from the saved program's unit and position", state.progressLabel(id), today.getCurrentDayLabel());
        state.assertDisplayedPercent(id, today.getProgramProgressValue());
    }
}
