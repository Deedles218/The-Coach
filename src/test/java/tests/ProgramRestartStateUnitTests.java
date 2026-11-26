package tests;

import lib.ProgramSelectionState;
import org.junit.Assert;
import org.junit.Test;
import java.util.*;

/** Reject partial, wrong-account and cross-program resets using the same API oracle as Android. */
public class ProgramRestartStateUnitTests {
    private ProgramSelectionState state(boolean restarted) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("backend_identity", "dedicated-qa-profile"); value.put("active", "for_her");
        value.put("main", program("for_her", true, restarted ? 1 : 35, restarted ? 0 : 100,
                restarted ? "in_progress" : "completed"));
        value.put("secondary", Collections.singletonList(program("sex_skill_app", false, 8, 20, "in_progress")));
        return new ProgramSelectionState(value);
    }
    private Map<String, Object> program(String id, boolean main, int day, int percent, String status) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("program_id", id); p.put("membership_status", main ? "main_journey" : "secondary");
        p.put("is_active", main); p.put("status", status); p.put("current_program_day", day);
        p.put("total_program_stages", 35); p.put("progress_percent", percent);
        p.put("current_module_name", "completed".equals(status) ? null : "Module " + (day == 1 ? 1 : 2));
        p.put("current_stage", "completed".equals(status) ? null : day == 1 ? 1 : 3);
        p.put("total_stages", "completed".equals(status) ? null : 4);
        return p;
    }
    private void rejects(ProgramSelectionState after, String message) {
        try { after.assertOnlyProgramRestarted(state(false), "for_her"); Assert.fail("Invalid reset was accepted"); }
        catch (AssertionError failure) { Assert.assertTrue(failure.getMessage(), failure.getMessage().contains(message)); }
    }
    @Test public void completedMainCanReturnToDayOneWithoutChangingSecondary() {
        state(true).assertOnlyProgramRestarted(state(false), "for_her");
    }
    @Test public void unchangedCompletedProgramIsNotAReset() { rejects(state(false), "in_progress"); }
    @Test public void zeroPercentAloneDoesNotProveTheAbsoluteDayWasReset() {
        ProgramSelectionState after = state(true); after.main().put("current_program_day", 35);
        rejects(after, "absolute Day 1");
    }
    @Test public void clearingAnotherProgramIsRejected() {
        ProgramSelectionState after = state(true); after.secondary().get(0).put("current_program_day", 1);
        after.secondary().get(0).put("progress_percent", 0); rejects(after, "another program");
    }
    @Test public void changingActiveDuringResetIsRejected() {
        ProgramSelectionState after = state(true); after.raw().put("active", "sex_skill_app");
        after.main().put("is_active", false); after.secondary().get(0).put("is_active", true);
        rejects(after, "Active Program");
    }
    @Test public void anotherBackendAccountCannotSupplyTheAfterSnapshot() {
        ProgramSelectionState after = state(true); after.raw().put("backend_identity", "another-profile");
        rejects(after, "backend account");
    }
}
