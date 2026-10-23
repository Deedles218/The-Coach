package tests;

import lib.ExploreStateRestorer;
import lib.RestoringTestAction;
import lib.ui.DailyPlanPageObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Offline checks of the same capture/cleanup path used by both Explore suites. */
public class ExploreStateRestorerUnitTests {
    @Test
    public void unchangedStateDoesNotReselectProgramOrDay() {
        Today today = new Today();
        ExploreStateRestorer state = capture(today);
        today.calls.clear();
        state.restore();
        Assert.assertEquals(Arrays.asList("today"), today.calls);
    }

    @Test
    public void restoresProgramBeforeRestoringItsSelectedDay() {
        Today today = new Today();
        ExploreStateRestorer state = capture(today);
        today.program = "Kegel Journey";
        today.day = "Day 1 of 60";
        today.progress = "8%";
        today.calls.clear();
        state.restore();
        Assert.assertEquals(Arrays.asList("today", "program:Sex Is a Skill", "day:Day 6 of 35"), today.calls);
        assertOriginal(today);
    }

    @Test
    public void restoresEarlierDayWithoutReselectingProgram() {
        Today today = new Today();
        ExploreStateRestorer state = capture(today);
        today.day = "Day 1 of 35";
        today.calls.clear();
        state.restore();
        Assert.assertEquals(Arrays.asList("today", "day:Day 6 of 35"), today.calls);
        assertOriginal(today);
    }

    @Test
    public void successfulDestinationCloseIsFollowedByRestoration() throws Exception {
        Today today = new Today();
        ExploreStateRestorer state = capture(today);
        today.day = "Day 1 of 35";
        today.calls.clear();
        state.restoreAfter(() -> today.calls.add("screenshot"), () -> today.calls.add("close"));
        Assert.assertEquals(Arrays.asList("screenshot", "close", "today", "day:Day 6 of 35"), today.calls);
        assertOriginal(today);
    }

    @Test
    public void screenshotFailureCannotPreventClosingOrRestoring() {
        Today today = new Today();
        ExploreStateRestorer state = capture(today);
        today.day = "Day 1 of 35";
        AssertionError screenshot = new AssertionError("screenshot unavailable");
        Throwable failure = failure(() -> state.restoreAfter(() -> { throw screenshot; },
                () -> today.calls.add("close")));
        Assert.assertSame(screenshot, failure);
        Assert.assertTrue(today.calls.contains("close"));
        assertOriginal(today);
    }

    @Test
    public void closeFailureCannotPreventRestoration() {
        Today today = new Today();
        ExploreStateRestorer state = capture(today);
        today.day = "Day 1 of 35";
        AssertionError close = new AssertionError("detail did not close");
        Assert.assertSame(close, failure(() -> state.restoreAfter(() -> {}, () -> { throw close; })));
        assertOriginal(today);
    }

    @Test
    public void failedScenarioStillRestoresAndKeepsItsOriginalFailure() {
        Today today = new Today();
        ExploreStateRestorer state = capture(today);
        AssertionError scenario = new AssertionError("Explore assertion failed");
        Throwable actual = failure(() -> RestoringTestAction.run(() -> {
            today.day = "Day 1 of 35";
            throw scenario;
        }, () -> state.restoreAfter(() -> {}, () -> {})));
        Assert.assertSame(scenario, actual);
        assertOriginal(today);
    }

    @Test
    public void progressMutationFailsInsteadOfResettingOrHidingIt() {
        Today today = new Today();
        ExploreStateRestorer state = capture(today);
        today.day = "Day 1 of 35";
        today.progress = "32%";
        Throwable actual = failure(state::restore);
        Assert.assertTrue(actual instanceof AssertionError);
        Assert.assertTrue(actual.getMessage().contains("changed the original progress"));
        Assert.assertEquals("Day 6 of 35", today.day);
        Assert.assertEquals("32%", today.progress);
    }

    @Test
    public void restorationFailureRemainsVisibleAlongsideCloseFailure() {
        Today today = new Today();
        ExploreStateRestorer state = capture(today);
        today.progress = "32%";
        AssertionError close = new AssertionError("close failed");
        Throwable actual = failure(() -> state.restoreAfter(() -> {}, () -> { throw close; }));
        Assert.assertSame(close, actual);
        Assert.assertEquals(1, actual.getSuppressed().length);
        Assert.assertTrue(actual.getSuppressed()[0].getMessage().contains("changed the original progress"));
    }

    @Test
    public void incorrectProgramRestorationDoesNotNavigateDaysInAnotherProgram() {
        Today today = new Today();
        ExploreStateRestorer state = ExploreStateRestorer.capture(today, ignored -> {}, today::selectDay);
        today.program = "Kegel Journey";
        today.calls.clear();
        Assert.assertTrue(failure(state::restore).getMessage().contains("original program"));
        Assert.assertEquals(Arrays.asList("today"), today.calls);
    }

    @Test
    public void incompleteSnapshotFailsBeforeAnySelection() {
        Today today = new Today();
        today.progress = " ";
        Assert.assertTrue(failure(() -> capture(today)).getMessage().contains("progress is empty"));
        Assert.assertEquals(Arrays.asList("today"), today.calls);
    }

    @Test
    public void androidStageChangeStillFailsWithoutNewDayNavigation() {
        Today today = new Today();
        ExploreStateRestorer state = ExploreStateRestorer.capture(today, today::selectProgram, ignored -> {});
        today.day = "Stage 2 of 35";
        Assert.assertTrue(failure(state::restore).getMessage().contains("original day/stage"));
        Assert.assertEquals("Stage 2 of 35", today.day);
    }

    private static ExploreStateRestorer capture(Today today) {
        return ExploreStateRestorer.capture(today, today::selectProgram, today::selectDay);
    }

    private static void assertOriginal(Today today) {
        Assert.assertEquals("Sex Is a Skill", today.program);
        Assert.assertEquals("Day 6 of 35", today.day);
        Assert.assertEquals("31%", today.progress);
    }

    private static Throwable failure(RestoringTestAction.CheckedAction action) {
        try {
            action.run();
        } catch (Exception | Error failure) {
            return failure;
        }
        throw new AssertionError("Expected action to fail");
    }

    private static final class Today extends DailyPlanPageObject {
        private String program = "Sex Is a Skill";
        private String day = "Day 6 of 35";
        private String progress = "31%";
        private final List<String> calls = new ArrayList<>();

        private Today() { super(null); }
        @Override public void openTodayTab() { calls.add("today"); }
        @Override public String getActiveProgramName() { return program; }
        @Override public String getCurrentDayLabel() { return day; }
        @Override public String getProgramProgressValue() { return progress; }
        private void selectProgram(String title) {
            calls.add("program:" + title);
            program = title;
            day = "Day 1 of 35";
            progress = "31%";
        }
        private void selectDay(String title) {
            calls.add("day:" + title);
            day = title;
        }
    }
}
