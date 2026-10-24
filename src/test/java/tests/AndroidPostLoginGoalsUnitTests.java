package tests;

import lib.PostAuthQuestionnaireException;
import lib.ui.android.AndroidOnboardingPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AndroidPostLoginGoalsUnitTests {
    private static class Screen extends AndroidOnboardingPageObject {
        final List<String> actions = new ArrayList<>();
        boolean questions = true;
        boolean primary = true;
        boolean secondaryAtStart;
        boolean selected;
        boolean fullWeb;
        boolean thirdNative;
        boolean rejectClose;
        boolean secondaryFallback;
        Screen() { super(null); }
        @Override public boolean isElementPresent(String locator) {
            if (locator.endsWith("/tvAnswerNum")) return questions && (!selected || thirdNative);
            if (locator.contains("WHAT DO YOU WANT TO")) return primary && !selected;
            if (locator.endsWith("/bContinue")) return secondaryAtStart && !selected;
            if (locator.contains("android.webkit.WebView")) return fullWeb;
            return false;
        }
        @Override protected void selectGoal(String goal, boolean allowSecondaryFallback) {
            actions.add("goals:" + goal); secondaryFallback = allowSecondaryFallback; selected = true;
        }
        @Override public WebElement waitForFirstElementPresent(String[] locators, String message, long timeout) {
            Assert.assertTrue(Arrays.stream(locators).anyMatch(x -> x.endsWith("/nav_graph_daily")));
            Assert.assertTrue(Arrays.stream(locators).anyMatch(x -> x.contains("android.webkit.WebView")));
            actions.add("destination"); return null;
        }
        @Override public int closePaywallsAndPopups() {
            actions.add("popups");
            if (rejectClose) throw new AssertionError("Offer did not close");
            return 1;
        }
        @Override public void waitForToday() { actions.add("today"); }
        @Override public void completeQuestionnaire() { Assert.fail("Login must never run the full questionnaire"); }
        @Override public void completePostAuthQuestionnaire() { Assert.fail("Login must never invoke fixture setup"); }
    }
    private void rejects(Screen screen) {
        try { screen.completePostLoginGoalQuestions("Improve sex skills"); Assert.fail("Unexpected questionnaire accepted"); }
        catch (PostAuthQuestionnaireException expected) { Assert.assertTrue(expected.getMessage().contains("questionnaire")); }
    }
    @Test public void twoNativeGoalsReachTodayWithoutFixturePreparation() {
        Screen screen = new Screen(); screen.completePostLoginGoalQuestions("Improve sex skills");
        Assert.assertEquals(Arrays.asList("goals:Improve sex skills", "destination", "popups", "today"), screen.actions);
        Assert.assertFalse("Login must not choose an arbitrary first secondary goal", screen.secondaryFallback);
    }
    @Test public void anAlreadyOpenDashboardDoesNotRequireGoalConfiguration() {
        Screen screen = new Screen(); screen.questions = false;
        screen.completePostLoginGoalQuestions(null);
        Assert.assertTrue(screen.actions.isEmpty());
    }
    @Test public void missingGoalFailsBeforeAnyAnswerIsSelected() {
        Screen screen = new Screen();
        try { screen.completePostLoginGoalQuestions(null); Assert.fail("Missing goal accepted"); }
        catch (IllegalStateException expected) { Assert.assertTrue(expected.getMessage().contains("explicit")); }
        Assert.assertTrue(screen.actions.isEmpty());
    }
    @Test public void blankGoalFailsBeforeAnyAnswerIsSelected() {
        Screen screen = new Screen();
        try { screen.completePostLoginGoalQuestions("  "); Assert.fail("Blank goal accepted"); }
        catch (IllegalStateException expected) { Assert.assertTrue(expected.getMessage().contains("explicit")); }
        Assert.assertTrue(screen.actions.isEmpty());
    }
    @Test public void unrelatedNativeQuestionCannotBeAnsweredAsAGoal() {
        Screen screen = new Screen(); screen.primary = false; rejects(screen);
        Assert.assertTrue(screen.actions.isEmpty());
    }
    @Test public void aResumedSecondaryQuestionDoesNotGuessTheMissingPrimaryAnswer() {
        Screen screen = new Screen(); screen.secondaryAtStart = true; rejects(screen);
        Assert.assertTrue(screen.actions.isEmpty());
    }
    @Test public void aThirdNativeQuestionIsNotConsumed() {
        Screen screen = new Screen(); screen.thirdNative = true; rejects(screen);
        Assert.assertEquals(Arrays.asList("goals:Improve sex skills", "destination"), screen.actions);
    }
    @Test public void fullWebQuestionnaireAfterTheGoalsIsNotConsumed() {
        Screen screen = new Screen(); screen.fullWeb = true; rejects(screen);
        Assert.assertEquals(Arrays.asList("goals:Improve sex skills", "destination"), screen.actions);
    }
    @Test public void fullWebQuestionnaireWithoutNativeGoalsIsNotConsumed() {
        Screen screen = new Screen(); screen.questions = false; screen.fullWeb = true; rejects(screen);
        Assert.assertTrue(screen.actions.isEmpty());
    }
    @Test public void aStuckOfferCannotBeReportedAsToday() {
        Screen screen = new Screen(); screen.rejectClose = true;
        try { screen.completePostLoginGoalQuestions("Improve sex skills"); Assert.fail("Failed close accepted"); }
        catch (AssertionError expected) { Assert.assertEquals("Offer did not close", expected.getMessage()); }
        Assert.assertEquals(Arrays.asList("goals:Improve sex skills", "destination", "popups"), screen.actions);
    }
}
