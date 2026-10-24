package tests;

import lib.ui.android.AndroidCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AndroidPostLoginAuthUnitTests {
    private static class Auth extends AndroidCoachFlowPageObject {
        final List<String> actions = new ArrayList<>();
        boolean goals = true;
        boolean rejectGoals;
        Auth() { super(null); }
        @Override public void authenticateForFixtureSetup(String email, String otp) { actions.add("otp"); }
        @Override public boolean isElementPresent(String locator) { return goals && locator.endsWith("/tvAnswerNum"); }
        // The login flow must call the bounded goal handler, not full fixture preparation.
        @Override protected void completePostLoginGoalsIfPresent() {
            actions.add("two-goals");
            if (rejectGoals) throw new AssertionError("Unexpected questionnaire");
        }
        @Override public void waitForAuthorizedDashboard() { actions.add("dashboard"); }
        @Override public void waitForLoadingToDisappearIfPresent(String locator, String message, long timeout) { actions.add("loaded"); }
        @Override public void openToday() { actions.add("today"); }
    }
    @Test public void expectedTwoGoalsArePartOfNormalOtpLogin() {
        Auth auth = new Auth(); auth.loginWithEmailAndOtp("fixture@example.invalid", "0000");
        Assert.assertEquals(Arrays.asList("otp", "two-goals", "dashboard", "loaded", "today"), auth.actions);
    }
    @Test public void aDirectDashboardDoesNotNeedGoalAnswers() {
        Auth auth = new Auth(); auth.goals = false;
        auth.loginWithEmailAndOtp("fixture@example.invalid", "0000");
        Assert.assertEquals(Arrays.asList("otp", "dashboard", "loaded", "today"), auth.actions);
    }
    @Test public void unexpectedQuestionnaireCannotBecomeASuccessfulLogin() {
        Auth auth = new Auth(); auth.rejectGoals = true;
        try { auth.loginWithEmailAndOtp("fixture@example.invalid", "0000"); Assert.fail("Unexpected questionnaire was accepted"); }
        catch (AssertionError expected) { Assert.assertEquals("Unexpected questionnaire", expected.getMessage()); }
        Assert.assertEquals(Arrays.asList("otp", "two-goals"), auth.actions);
    }
}
