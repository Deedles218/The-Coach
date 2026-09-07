package tests;

import org.junit.Test;

/** User-approved invariant: actual pre-action progress, never configuration. */
public class TestModelProgressUnitTests {
    @Test public void exactTwoPercentPasses() {
        check("2%", "2%");
    }

    @Test public void presentationWhitespaceDoesNotChangeTheValue() {
        check(" 6\u00a0% ", "6%");
    }

    @Test(expected = AssertionError.class)
    public void changedProgressDoesNotPass() {
        check("7%", "6%");
    }

    @Test(expected = AssertionError.class)
    public void resetToZeroAfterRestartCannotPass() {
        check("0%", "6%");
    }

    @Test(expected = AssertionError.class)
    public void missingProgressCannotPass() {
        check(null, "6%");
    }

    @Test(expected = AssertionError.class)
    public void containingTwoPercentDoesNotSatisfyExactValue() {
        check("12%", "2%");
    }

    @Test(expected = AssertionError.class)
    public void oldConfigurationCannotWeakenTheReviewedOracle() {
        String key = "coach.testModel.customization.expectedProgress";
        String previous = System.getProperty(key);
        try {
            System.setProperty(key, "1%");
            check("1%", "6%");
        } finally {
            if (previous == null) System.clearProperty(key);
            else System.setProperty(key, previous);
        }
    }

    @Test public void moduleBaselinePasses() { check("6%", "6%"); }
    @Test(expected = AssertionError.class) public void missingBaselineFails() { check("6%", null); }
    @Test(expected = AssertionError.class) public void twoMissingValuesFail() { check(null, null); }
    @Test(expected = AssertionError.class) public void matchingGarbageFails() { check("Loading", "Loading"); }
    @Test(expected = AssertionError.class) public void outOfRangeFails() { check("100.1%", "100.1%"); }
    private static void check(String actual, String baseline) {
        TestModelAutomationTests.assertCoa8512ProgressMatches(actual, baseline, "after move/restart");
    }
}
