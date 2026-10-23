package tests;

import lib.Platform;
import lib.PostAuthQuestionnaireException;
import lib.ui.android.AndroidCoachFlowPageObject;
import lib.ui.android.AndroidOnboardingPageObject;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;

public class AndroidPostAuthPreparationUnitTests {
    private final Map<String, String> original = new HashMap<>();
    @Before public void configure() {
        for (String key : new String[]{"platform", "coach.android.preparePostAuthQuestionnaire", "postAuth.questionnaire.goal"}) {
            original.put(key, System.getProperty(key));
        }
        System.setProperty("platform", "android");
        System.setProperty("coach.android.preparePostAuthQuestionnaire", "false");
        System.setProperty("postAuth.questionnaire.goal", "Prepared goal");
    }
    @After public void restore() {
        for (Map.Entry<String, String> e : original.entrySet()) {
            if (e.getValue() == null) System.clearProperty(e.getKey()); else System.setProperty(e.getKey(), e.getValue());
        }
    }
    private static class Auth extends AndroidCoachFlowPageObject {
        int authentications;
        Auth() { super(null); }
        @Override public void authenticateForFixtureSetup(String email, String otp) { authentications++; }
        @Override public boolean isElementPresent(String locator) { return locator.endsWith("/tvAnswerNum"); }
    }

    @Test public void fixtureLoginRejectsMissingApprovalBeforeAuthentication() {
        Auth auth = new Auth();
        try { auth.loginForFixturePreparation("fixture@example.invalid", "0000"); Assert.fail("Missing approval accepted"); }
        catch (IllegalStateException expected) { Assert.assertTrue(expected.getMessage().contains("opt-in")); }
        Assert.assertEquals(0, auth.authentications);
    }

    @Test(expected = IllegalStateException.class)
    public void onboardingEntryCannotMutateWithoutApproval() {
        new AndroidOnboardingPageObject(null).completePostAuthQuestionnaire();
    }

    @Test public void strictLoginStillFailsOnQuestionnaireEvenWhenPreparationIsApproved() {
        System.setProperty("coach.android.preparePostAuthQuestionnaire", "true");
        Auth auth = new Auth();
        try { auth.loginWithEmailAndOtp("fixture@example.invalid", "0000"); Assert.fail("Strict login consumed questionnaire"); }
        catch (PostAuthQuestionnaireException expected) { Assert.assertTrue(expected.getMessage().contains("questionnaire")); }
        Assert.assertEquals(1, auth.authentications);
    }

    @Test(expected = IllegalStateException.class)
    public void fixturePreparationCannotBeEnabledForIOS() {
        System.setProperty("platform", "ios");
        System.setProperty("coach.android.preparePostAuthQuestionnaire", "true");
        Platform.getInstance().validateAndroidPostAuthFixturePreparation();
    }

    @Test public void explicitlyConfiguredGoalIsUsed() {
        System.setProperty("coach.android.preparePostAuthQuestionnaire", "true");
        Platform.getInstance().validateAndroidPostAuthFixturePreparation();
        Assert.assertEquals("Prepared goal", Platform.getInstance().getPostAuthQuestionnaireGoal());
    }
}
