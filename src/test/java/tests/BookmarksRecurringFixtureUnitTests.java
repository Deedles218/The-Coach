package tests;

import lib.ui.ios.iOSBookmarksPageObject;
import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookmarksRecurringFixtureUnitTests {
    private static class Screen extends iOSBookmarksPageObject {
        final List<String> days = new ArrayList<>();
        boolean completed;
        boolean failNavigation;
        Screen() { super(null); }
        @Override protected void selectRecurringDay(String day, boolean previous) {
            days.add(day);
            if (previous && failNavigation) throw new IllegalStateException("navigation failed midway");
        }
        @Override public void swipeUpToFindFirstVisibleElement(String[] locators, String message, int swipes) { }
        @Override public int getAmountElements(String locator) { return 1; }
        @Override protected boolean isDailyPlanItemCompleted(String title, boolean attach) { return completed; }
    }

    @Test public void nonAdjacentOccurrenceIsCheckedAndSavedDayIsRestored() throws Exception {
        Screen screen = new Screen();
        screen.assertRecurringPreviousDayFixture("5 Senses Exercise", "Stage 4 of 4", "Stage 2 of 4");
        Assert.assertEquals(Arrays.asList("Stage 2 of 4", "Stage 4 of 4"), screen.days);
    }

    @Test public void completedEarlierOccurrenceFailsAndStillRestoresSavedDay() throws Exception {
        Screen screen = new Screen();
        screen.completed = true;
        try {
            screen.assertRecurringPreviousDayFixture("Practice", "Day 4", "Day 2");
            Assert.fail("A completed earlier occurrence was accepted");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage().contains("already completed"));
        }
        Assert.assertEquals(Arrays.asList("Day 2", "Day 4"), screen.days);
    }

    @Test public void partialNavigationFailureStillRestoresSavedDay() throws Exception {
        Screen screen = new Screen();
        screen.failNavigation = true;
        try {
            screen.assertRecurringPreviousDayFixture("Practice", "Day 4", "Day 2");
            Assert.fail("The navigation failure was swallowed");
        } catch (IllegalStateException expected) {
            Assert.assertEquals("navigation failed midway", expected.getMessage());
        }
        Assert.assertEquals(Arrays.asList("Day 2", "Day 4"), screen.days);
    }

    @Test public void finalAssertionChecksTheConfiguredEarlierOccurrence() {
        Screen screen = new Screen();
        screen.assertPreviousDayItemNotCompleted("Practice", "Day 2");
        Assert.assertEquals(Arrays.asList("Day 2"), screen.days);
        screen.completed = true;
        try {
            screen.assertPreviousDayItemNotCompleted("Practice", "Day 2");
            Assert.fail("Completion leaked to the earlier day without failing");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage().contains("also completed"));
        }
    }
}
