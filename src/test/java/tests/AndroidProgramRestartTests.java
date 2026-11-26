package tests;

import io.qameta.allure.*;
import lib.*;
import lib.ui.android.AndroidMyProgramsPageObject;
import lib.ui.android.AndroidProgramSelectionPageObject;
import org.junit.*;

/** Mutates only the explicitly UID/backend/APK-bound Completed Main QA fixture. */
@Epic("COA-9583 Program activation")
@Feature("Android program settings and Restart")
public class AndroidProgramRestartTests extends CoreTestCase {
    private AndroidProgramSelectionPageObject selection;
    private AndroidMyProgramsPageObject today;

    @Before @Override public void setUp() throws Exception {
        Assert.assertTrue("Run on Android", Platform.getInstance().isAndroid());
        Assert.assertTrue("Restart requires an installed dedicated QA app, noReset=true and fullReset=false",
                Platform.getInstance().isAndroidAttachConfigured());
        super.setUp();
        closeAndReopenCoachApplication();
        selection = new AndroidProgramSelectionPageObject(driver);
        today = new AndroidMyProgramsPageObject(driver);
        selection.closeStartupPrompts();
        ProgramSelectionApi.assertDedicatedProfile("COA-9794");
    }

    @Override protected void cleanupSessionForNextTest() { /* Keep the dedicated fixture and its evidence. */ }

    @Test @Issues({@Issue("COA-9864"), @Issue("COA-9866")})
    public void completedMainRestartsOnlyAfterConfirmationAndPreservesSecondaryProgress() throws Exception {
        ProgramSelectionState before = ProgramSelectionApi.read();
        Assert.assertNotNull("Prepare a Completed Main Journey", before.main());
        before.assertActive(before.mainId());
        Assert.assertEquals("Complete a lesson on Main's final day before this test", "completed", before.main().get("status"));
        Assert.assertTrue("Prepare independent nonzero Secondary progress", before.secondary().stream().anyMatch(p ->
                p.get("progress_percent") instanceof Number && ((Number) p.get("progress_percent")).doubleValue() > 0));
        before.assertAllProgressFormulas();
        // Validate the post-reset UI oracle before making any changes, including a Completed Main's unit.
        for (java.util.Map<String, Object> row : before.programs()) {
            ProgramSelectionState.progressUnit((String) row.get("program_id"));
        }
        String id = before.mainId(), title = (String) before.main().get("title");
        today.openProgramSelector(); today.assertMatchesState(before); today.closeProgramSelector();

        selection.openResetProgress(title);
        selection.decideResetProgress(false);
        ProgramSelectionApi.read().assertExactlyEquals(before);

        selection.openResetProgress(title);
        selection.decideResetProgress(true);
        ProgramSelectionState after = ProgramSelectionApi.read();
        after.assertOnlyProgramRestarted(before, id);

        closeAndReopenCoachApplication(); selection.closeStartupPrompts();
        ProgramSelectionApi.read().assertExactlyEquals(after);
        Assert.assertEquals("Restart opened another Daily Plan", title, today.getActiveProgramName());
        Assert.assertEquals("Restart did not show the first Day/Stage", after.progressLabel(id), today.getCurrentDayLabel());
        after.assertDisplayedPercent(id, today.getProgramProgressValue());
        today.openProgramSelector(); today.assertMatchesState(after); today.closeProgramSelector();
    }
}
