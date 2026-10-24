package tests;

import lib.Platform;
import lib.PostAuthQuestionnaireException;
import lib.ui.android.AndroidCoachFlowPageObject;
import lib.ui.android.AndroidOnboardingPageObject;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NodeList;

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
        int normalizations;
        boolean questionnaire = true;
        Auth() { super(null); }
        String validationError() { return LOGIN_VALIDATION_ERROR; }
        @Override public void authenticateForFixtureSetup(String email, String otp) { authentications++; }
        @Override public boolean isElementPresent(String locator) { return questionnaire && locator.endsWith("/tvAnswerNum"); }
        @Override public void ensureLoggedOutOnStartScreen() { normalizations++; }
        @Override protected void completePostLoginGoalsIfPresent() {
            throw new PostAuthQuestionnaireException("Unexpected full questionnaire in this fixture");
        }
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

    @Test public void strictLoginStillFailsOnUnexpectedQuestionnaireEvenWhenPreparationIsApproved() {
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

    private int matchingErrors(String attributes) throws Exception {
        String xml = "<root><android.widget.TextView resource-id='"
                + Platform.getInstance().getAndroidAppPackage() + ":id/tvEmailNotfoundError' "
                + attributes + "/></root>";
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return ((NodeList) XPathFactory.newInstance().newXPath().evaluate(
                new Auth().validationError().substring(6), document, XPathConstants.NODESET)).getLength();
    }

    @Test public void unregisteredEmailIsARecognizedLoginErrorRatherThanAnOtpTimeout() throws Exception {
        Assert.assertEquals(1, matchingErrors("text='Email not found. Contact support below.' displayed='true'"));
    }

    @Test public void emptyOrHiddenEmailErrorCannotRejectAValidLogin() throws Exception {
        Assert.assertEquals(0, matchingErrors("text='' displayed='true'"));
        Assert.assertEquals(0, matchingErrors("text='Email not found. Contact support below.' displayed='false'"));
    }

    @Test public void unfinishedQuestionnaireCannotBeReportedAsVerifiedLogoutByNavigatingBack() {
        Auth auth = new Auth();
        try { auth.cleanupForNextTest(); Assert.fail("Questionnaire cleanup silently accepted navigation as logout"); }
        catch (IllegalStateException expected) { Assert.assertTrue(expected.getMessage().contains("questionnaire")); }
        Assert.assertEquals(0, auth.normalizations);
    }

    @Test public void ordinaryCleanupStillUsesTheExistingLogoutFlow() {
        Auth auth = new Auth(); auth.questionnaire = false;
        auth.cleanupForNextTest();
        Assert.assertEquals(1, auth.normalizations);
    }
}
