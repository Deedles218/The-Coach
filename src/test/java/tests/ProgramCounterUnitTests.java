package tests;

import lib.ProgramSelectionState;
import lib.ui.ios.IOSMyProgramsPageObject;
import org.junit.Assert;
import org.junit.Test;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Regression: Her Daily Plan uses Day even when module/stage metadata is present. */
public class ProgramCounterUnitTests {
    private Map<String, Object> program() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("program_id", "sex_skill_app");
        row.put("status", "active");
        row.put("current_module_name", "Your Pleasure Anatomy");
        row.put("current_stage", 1);
        row.put("total_stages", 5);
        row.put("current_program_day", 1);
        row.put("total_program_stages", 35);
        row.put("progress_percent", 0);
        return row;
    }

    @Test public void moduleMetadataDoesNotOverrideTheDailyPlanDayUnit() {
        IOSMyProgramsPageObject.assertCounterMatchesProgram("DAY 1 OF 35", program(), "Day 1 of 35");
    }

    @Test public void stageProgramKeepsItsStageCounter() {
        IOSMyProgramsPageObject.assertCounterMatchesProgram("STAGE 1 OF 5", program(), "Stage 1 of 5");
    }

    @Test(expected = AssertionError.class) public void selectorStageCannotPassAgainstDailyPlanDay() {
        IOSMyProgramsPageObject.assertCounterMatchesProgram("STAGE 1 OF 5", program(), "Day 1 of 35");
    }

    @Test(expected = AssertionError.class) public void correctUnitWithTheWrongTotalStillFails() {
        IOSMyProgramsPageObject.assertCounterMatchesProgram("DAY 1 OF 5", program(), "Day 1 of 35");
    }

    @Test(expected = AssertionError.class) public void matchingUiCountersCannotOverrideTheSavedPosition() {
        IOSMyProgramsPageObject.assertCounterMatchesProgram("DAY 2 OF 35", program(), "Day 2 of 35");
    }

    @Test public void completedRemainsDistinctFromDayAndStage() {
        Map<String, Object> row = program();
        row.put("status", "completed");
        row.put("current_stage", null);
        row.put("total_stages", null);
        IOSMyProgramsPageObject.assertCounterMatchesProgram("Completed", row, null);
    }

    @Test public void changingOnlyTheProgramDayCannotPassTheProgressPreservationCheck() {
        Map<String, Object> first = program(), second = program();
        second.put("current_program_day", 2);
        try {
            state(second).assertSameProgress(state(first));
            Assert.fail("Day advancement must be observed even if the module stage did not change");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage().contains("Switching changed progress"));
        }
    }

    private ProgramSelectionState state(Map<String, Object> program) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("main", program);
        value.put("secondary", Collections.emptyList());
        return new ProgramSelectionState(value);
    }
}
