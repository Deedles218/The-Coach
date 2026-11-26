package lib.ui;

import lib.EmailEnforcementFixture.Stage;
import org.junit.Assert;

import java.util.regex.Pattern;

public interface EmailReleaseScreen {
    void reachDailyWithoutDismissingRequest();
    void assertStage(Stage stage);
    void dismiss(Stage stage, String action);
    void waitForDaily();
    void assertNoRequest();
    void revisitDaily();
    void enterEmail(String email);
    void submit();
    void assertSingleOtp(String email);
    void changeEmailAndCheckValidation(Stage stage);
    void typeSecurityCode(String code);
    void finishConfirmation();
    void assertProfileEmail(String email);
    void openEmailSupport();
    void returnFromEmailSupport(Stage stage);
    void assertExistingAccount(String email);
    boolean isDailyVisible();
    void assertEmptyEmail();
    void assertMandatoryCannotBeDismissed(Stage stage);
    void assertPreviouslyDisplayedFirstRequest();
    void returnFromExistingAccount(Stage stage);
    void assertSpanishExistingAccountCopy();
    void waitForResendReady();
    long resendLinkingCode(String email);
    void enterOtpWithLayoutCheck(String code);
    void assertConfirmationLayout();
    void openStartupLogin();
    boolean hasOnboardingGoals();
    void finishStartupLogin();

    static void assertSpanishLoginCopy(String question, String action) {
        Pattern expectedQuestion = Pattern.compile(
                "(?isu)(?=.*(?:iniciar sesi[oó]n|entrar|acceder|identificar|conectar))"
                        + "(?=.*(?:correo|e-?mail)).*\\?");
        Pattern expectedAction = Pattern.compile(
                "(?iu)(?:inicia(?:r)? sesi[oó]n|entrar|accede(?:r)?|conectarse|identificarse)");
        Assert.assertTrue("Expected a Spanish login question and action. Visible: " + question + " / " + action,
                expectedQuestion.matcher(question.trim()).matches() && expectedAction.matcher(action.trim()).matches());
    }
}
