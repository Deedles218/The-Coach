package tests;

import lib.PostAuthQuestionnaireException;
import lib.ui.ios.iOSOnboardingPageObject;
import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IOSPostLoginGoalsUnitTests {
    private static class Screen extends iOSOnboardingPageObject {
        boolean primary = true;
        boolean secondary;
        boolean full;
        boolean thirdQuestion;
        final List<String> actions = new ArrayList<>();
        Screen() { super(null); }
        @Override public boolean isGoalSelectionDisplayed() { return primary; }
        @Override public boolean isElementVisible(String locator) {
            return secondary && locator.contains("WHAT ELSE DO YOU WANT TO ACHIEVE?");
        }
        @Override protected boolean isUnexpectedPostLoginQuestionnaireVisible() { return full; }
        @Override public void selectGoal(String goal) {
            actions.add(goal);
            primary = false;
            full = thirdQuestion;
        }
        @Override public void completeQuestionnaire() { Assert.fail("Ordinary login invoked full onboarding"); }
    }

    @Test public void knownGoalsReuseTheExistingGoalSelector() {
        Screen screen = new Screen();
        screen.completePostLoginGoalQuestions("Improve sex skills");
        Assert.assertEquals(Arrays.asList("Improve sex skills"), screen.actions);
    }

    @Test public void missingGoalIsRejectedBeforeAnyAnswer() {
        Screen screen = new Screen();
        try {
            screen.completePostLoginGoalQuestions(null);
            Assert.fail("Missing goal was guessed");
        } catch (PostAuthQuestionnaireException expected) {
            Assert.assertTrue(expected.getMessage().contains("explicit"));
        }
        Assert.assertTrue(screen.actions.isEmpty());
    }

    @Test public void resumedAdditionalGoalsAreNotAnsweredWithoutThePrimaryScreen() {
        Screen screen = new Screen();
        screen.primary = false;
        screen.secondary = true;
        rejects(screen);
        Assert.assertTrue(screen.actions.isEmpty());
    }

    @Test public void unrelatedQuestionnaireBeforeGoalsIsNotAnswered() {
        Screen screen = new Screen();
        screen.primary = false;
        screen.full = true;
        rejects(screen);
        Assert.assertTrue(screen.actions.isEmpty());
    }

    @Test public void thirdQuestionAfterGoalsIsNotAnswered() {
        Screen screen = new Screen();
        screen.thirdQuestion = true;
        rejects(screen);
        Assert.assertEquals(Arrays.asList("Improve sex skills"), screen.actions);
    }

    @Test public void todayWithoutGoalsNeedsNoAnswers() {
        Screen screen = new Screen();
        screen.primary = false;
        screen.completePostLoginGoalQuestions(null);
        Assert.assertTrue(screen.actions.isEmpty());
    }

    private void rejects(Screen screen) {
        try {
            screen.completePostLoginGoalQuestions("Improve sex skills");
            Assert.fail("Unexpected questionnaire was accepted");
        } catch (PostAuthQuestionnaireException expected) {
            Assert.assertTrue(expected.getMessage().contains("questionnaire"));
        }
    }
}
