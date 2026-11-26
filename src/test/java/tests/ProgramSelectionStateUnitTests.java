package tests;

import lib.ProgramSelectionState;
import org.junit.Assert;
import org.junit.Test;
import java.util.*;

/** COA-9853: a changed absolute day must not hide behind an unchanged module/stage. */
public class ProgramSelectionStateUnitTests {
    private Map<String, Object> program(int day) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("program_id", "sex_skill_app"); p.put("membership_status", "main_journey");
        p.put("status", "in_progress"); p.put("current_module_name", "Module 2");
        p.put("current_stage", 3); p.put("total_stages", 4);
        p.put("current_program_day", day); p.put("total_program_stages", 35); p.put("progress_percent", 20);
        return p;
    }
    private ProgramSelectionState state(Map<String, Object> p) {
        Map<String, Object> value = new HashMap<>(); value.put("main", p);
        value.put("secondary", Collections.emptyList()); return new ProgramSelectionState(value);
    }
    @Test public void absoluteDayIsNotTheStageWithinItsModule() {
        Assert.assertEquals("Day 8 of 35", state(program(8)).dayLabel("sex_skill_app"));
    }
    @Test public void changingOnlyTheAbsoluteDayFailsTheProgressInvariant() {
        Assert.assertEquals(state(program(8)).progressLabel("sex_skill_app", "Stage"),
                state(program(2)).progressLabel("sex_skill_app", "Stage"));
        try { state(program(2)).assertSameProgress(state(program(8))); Assert.fail("Day reset was accepted"); }
        catch (AssertionError expected) { Assert.assertTrue(expected.getMessage().contains("Switching changed progress")); }
    }
    @Test public void missingDayCannotFallBackToTheModuleStage() {
        Map<String, Object> p = program(8); p.remove("current_program_day");
        try { state(p).dayLabel("sex_skill_app"); Assert.fail("Missing day was accepted"); }
        catch (AssertionError expected) { Assert.assertTrue(expected.getMessage().contains("current_program_day")); }
    }
    @Test public void anOutOfRangeDayCannotBecomeAValidUiLabel() {
        try { state(program(36)).dayLabel("sex_skill_app"); Assert.fail("Out of range day was accepted"); }
        catch (AssertionError expected) { Assert.assertTrue(expected.getMessage().contains("Invalid program day")); }
    }
    @Test public void androidPercentageWhitespaceDoesNotChangeItsValue() {
        state(program(8)).assertDisplayedPercent("sex_skill_app", "20 %");
        state(program(8)).assertDisplayedPercent("sex_skill_app", "20\u00a0%");
    }
    @Test public void reviewedUnitSelectsTheCorrespondingCounter() {
        ProgramSelectionState state = state(program(8));
        Assert.assertEquals("Day 8 of 35", state.progressLabel("sex_skill_app", "Day"));
        Assert.assertEquals("Stage 3 of 4", state.progressLabel("sex_skill_app", "Stage"));
    }
    @Test public void missingOrInvalidStageCannotFallBackToTheDay() {
        for (Object invalid : Arrays.asList(null, 5, 1.5)) {
            Map<String, Object> p = program(8); p.put("current_stage", invalid);
            try { state(p).progressLabel("sex_skill_app", "Stage"); Assert.fail("Invalid stage was accepted"); }
            catch (AssertionError expected) { Assert.assertTrue(expected.getMessage().contains("stage")); }
        }
    }
    @Test public void unknownUnitIsRejected() {
        try { state(program(8)).progressLabel("sex_skill_app", "Module"); Assert.fail("Unknown unit was accepted"); }
        catch (AssertionError expected) { Assert.assertTrue(expected.getMessage().contains("Day or Stage")); }
    }
    @Test public void fixtureMustDeclareTheUnitInsteadOfInferringItFromModuleFields() {
        String key = "coach.programProgress.unit.sex_skill_app", previous = System.getProperty(key);
        try {
            System.clearProperty(key);
            try { state(program(8)).progressLabel("sex_skill_app"); Assert.fail("Missing unit was inferred"); }
            catch (AssertionError expected) { Assert.assertTrue(expected.getMessage().contains("Configure")); }
            System.setProperty(key, "Stage");
            Assert.assertEquals("Stage 3 of 4", state(program(8)).progressLabel("sex_skill_app"));
            System.setProperty(key, "Day");
            Assert.assertEquals("Day 8 of 35", state(program(8)).progressLabel("sex_skill_app"));
        } finally {
            if (previous == null) System.clearProperty(key); else System.setProperty(key, previous);
        }
    }
}
