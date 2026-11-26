package tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import lib.CoreTestCase;
import lib.EmailEnforcementFixture.Stage;
import lib.EmailTestMailbox;
import lib.RestoringTestAction.CheckedAction;
import lib.TestData;
import lib.ui.EmailReleaseScreen;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/** Shared release scenarios for both platforms and both app variants. */
public abstract class EmailReleaseRegression extends CoreTestCase {
    private static final String FREE_EMAIL = "coach.validEmailWithoutProgress";
    private static final String TEST_OTP = "coach.emailRelease.otp";

    @Rule public final TestName testName = new TestName();
    private String previousFreeEmail;
    private String previousTestOtp;
    private boolean savedProperties;

    protected abstract EmailReleaseScreen screen();
    protected abstract void startNewUserOnboarding();
    protected abstract void finishOnboarding(boolean requirePaywall);
    protected abstract void withPreviousRelease(CheckedAction scenario) throws Exception;
    protected abstract void installUpdate() throws Exception;
    protected abstract void switchLanguage(String language) throws Exception;
    protected abstract void returnToWelcomeForLogin() throws Exception;
    protected abstract int defaultFirstRequestLaunch();

    protected void coldLaunch() { closeAndReopenCoachApplication(); }

    protected final String testLanguage() {
        String method = testName.getMethodName();
        if ("testCoa9807ExistingAccountCopyInSpanish".equals(method)) return "es";
        return System.getProperty("coach.emailRelease.language", "en").trim().toLowerCase(Locale.ROOT);
    }

    protected final void validateLanguage(String supportedChangeEmailLanguages) {
        String method = testName.getMethodName();
        String language = testLanguage();
        if ("testCoa9767ChangeEmailClearsInput".equals(method)) {
            Assert.assertTrue("Unsupported Change Email locale", language.matches(supportedChangeEmailLanguages));
        } else if ("testCoa9806BackFromExistingAccountAllowsAnotherEmail".equals(method)
                || "testCoa9768SupportDoesNotOverlapOtp".equals(method)) {
            Assert.assertTrue("Use en or es for the occupied-address or OTP layout case", language.matches("en|es"));
        } else {
            Assert.assertTrue("Run the other release scenarios in English", "en".equals(language)
                    || "testCoa9807ExistingAccountCopyInSpanish".equals(method));
        }
    }

    @Override public void setUp() throws Exception {
        previousFreeEmail = System.getProperty(FREE_EMAIL);
        previousTestOtp = System.getProperty(TEST_OTP);
        savedProperties = true;
        super.setUp();
    }

    @Test @Issue("COA-9781")
    public void testCoa9781PurchaseLoginStartsMandatoryEmail() {
        startNewUserOnboarding();
        screen().assertStage(Stage.N);
        screen().assertEmptyEmail();
        screen().assertMandatoryCannotBeDismissed(Stage.N);
    }

    @Test @Issues({@Issue("COA-9782"), @Issue("COA-9764"), @Issue("COA-9765"), @Issue("COA-9766"), @Issue("COA-9701")})
    public void testCoa9782FirstRequestAndProgressiveDismissal() throws Exception {
        verifyProgressiveDismissal("later");
    }

    @Test @Issues({@Issue("COA-9782"), @Issue("COA-9765"), @Issue("COA-9766")})
    public void testCoa9782DismissOptionalRequestsWithCross() throws Exception {
        verifyProgressiveDismissal("cross");
    }

    private void verifyProgressiveDismissal(String action) throws Exception {
        withPreviousRelease(() -> {
            installUpdate();
            reachFirstRequest();
            screen().assertStage(Stage.E1);
            screen().dismiss(Stage.E1, action);
            for (int visit = 0; visit < 3; visit++) screen().revisitDaily();
            reopen();
            screen().assertStage(Stage.E2);
            screen().dismiss(Stage.E2, action);
            reopen();
            screen().assertStage(Stage.E3);
            screen().assertMandatoryCannotBeDismissed(Stage.E3);
            reopen();
            screen().assertStage(Stage.E3);
        });
    }

    @Test @Issues({@Issue("COA-9700"), @Issue("COA-9764"), @Issue("COA-9699")})
    public void testCoa9700UpdatePreservesAnAlreadyDisplayedFirstRequest() throws Exception {
        withPreviousRelease(() -> {
            reachFirstRequest();
            screen().assertPreviouslyDisplayedFirstRequest();
            installUpdate();
            reopen();
            screen().assertStage(Stage.E2);
            reopen();
            screen().assertStage(Stage.E3);
        });
    }

    @Test @Issue("COA-9806")
    public void testCoa9806BackFromExistingAccountAllowsAnotherEmail() throws Exception {
        String occupied = TestData.existingProgressAccountEmail();
        String freeEmail = TestData.validEmailWithoutProgress();
        Assert.assertFalse("Use a free address distinct from the occupied test account", freeEmail.equalsIgnoreCase(occupied));
        Stage stage = prepareMandatory(testLanguage());
        screen().enterEmail(occupied);
        screen().submit();
        screen().assertExistingAccount(occupied);
        screen().returnFromExistingAccount(stage);
        submitEmail(freeEmail);
    }

    @Test @Issue("COA-9807")
    public void testCoa9807ExistingAccountCopyInSpanish() throws Exception {
        String occupied = TestData.existingProgressAccountEmail();
        prepareMandatory("es");
        screen().enterEmail(occupied);
        screen().submit();
        screen().assertExistingAccount(occupied);
        screen().assertSpanishExistingAccountCopy();
    }

    @Test @Issue("COA-9767")
    public void testCoa9767ChangeEmailClearsInput() throws Exception {
        String email = TestData.validEmailWithoutProgress();
        Stage stage = prepareMandatory(testLanguage());
        submitEmail(email);
        screen().changeEmailAndCheckValidation(stage);
    }

    @Test @Issue("COA-9762")
    public void testCoa9762ResendKeepsOtpAndDeliversAnotherEmail() throws Exception {
        EmailTestMailbox mailbox = newMailbox();
        prepareMandatory("en");
        submitEmail(mailbox.address());
        mailbox.awaitCodeAfter(Collections.emptySet());
        screen().waitForResendReady();
        Set<String> previous = mailbox.messageIds();
        Assert.assertFalse("The first OTP delivery was not observed", previous.isEmpty());
        long resendBoundary = screen().resendLinkingCode(mailbox.address());
        mailbox.awaitCodeAfter(previous, resendBoundary);
        screen().assertSingleOtp(mailbox.address());
    }

    @Test @Issue("COA-9768")
    public void testCoa9768SupportDoesNotOverlapOtp() throws Exception {
        EmailTestMailbox mailbox = newMailbox();
        prepareMandatory(testLanguage());
        submitEmail(mailbox.address());
        String code = mailbox.awaitCodeAfter(Collections.emptySet());
        System.setProperty(TEST_OTP, code);
        screen().enterOtpWithLayoutCheck(code);
        screen().assertConfirmationLayout();
        screen().finishConfirmation();
    }

    @Test @Issues({@Issue("COA-9761"), @Issue("COA-9411"), @Issue("COA-9702")})
    public void testCoa9761ConfirmedEmailDoesNotReappearOnLaunchOrLogin() throws Exception {
        EmailTestMailbox mailbox = newMailbox();
        Stage stage = prepareMandatory("en");
        screen().openEmailSupport();
        screen().returnFromEmailSupport(stage);
        submitEmail(mailbox.address());
        enterCode(mailbox.awaitCodeAfter(Collections.emptySet()));
        screen().finishConfirmation();
        screen().assertProfileEmail(mailbox.address());
        reopen();
        screen().waitForDaily();
        screen().assertNoRequest();

        // The retest signs into the same just-confirmed account from a clean installation.
        Set<String> previous = mailbox.messageIds();
        returnToWelcomeForLogin();
        screen().openStartupLogin();
        submitEmail(mailbox.address());
        enterCode(mailbox.awaitCodeAfter(previous));
        screen().finishStartupLogin();
        if (screen().hasOnboardingGoals()) finishOnboarding(false);
        screen().reachDailyWithoutDismissingRequest();
        screen().waitForDaily();
        screen().assertNoRequest();
        screen().assertProfileEmail(mailbox.address());
    }

    private Stage prepareMandatory(String testLanguage) throws Exception {
        startNewUserOnboarding();
        Stage stage = Stage.N;
        if (Boolean.getBoolean("coach.emailRelease.allowE3Preparation") && screen().isDailyVisible()) {
            Allure.addAttachment("Starting condition", "Daily opened after onboarding; preparing E3 through cold launches.");
            reachFirstRequest();
            screen().assertStage(Stage.E1);
            screen().dismiss(Stage.E1, "later");
            reopen();
            screen().assertStage(Stage.E2);
            screen().dismiss(Stage.E2, "later");
            reopen();
            stage = Stage.E3;
        }
        screen().assertStage(stage);
        if (!"en".equals(testLanguage)) {
            switchLanguage(testLanguage);
            screen().reachDailyWithoutDismissingRequest();
            screen().assertStage(stage);
        }
        return stage;
    }

    private void reachFirstRequest() {
        int requestLaunch = Integer.getInteger("coach.emailRelease.firstRequestLaunch", defaultFirstRequestLaunch());
        Assert.assertTrue("The initial request launch must be positive", requestLaunch > 0);
        for (int launch = 1; launch <= requestLaunch; launch++) {
            reopen();
            if (launch < requestLaunch) {
                screen().waitForDaily();
                screen().assertNoRequest();
            }
        }
    }

    private void submitEmail(String email) {
        screen().enterEmail(email);
        screen().submit();
        screen().assertSingleOtp(email);
    }

    private EmailTestMailbox newMailbox() throws Exception {
        EmailTestMailbox mailbox = EmailTestMailbox.create();
        System.setProperty(FREE_EMAIL, mailbox.address());
        return mailbox;
    }

    private void enterCode(String code) {
        System.setProperty(TEST_OTP, code);
        screen().typeSecurityCode(code);
    }

    private void reopen() {
        coldLaunch();
        screen().reachDailyWithoutDismissingRequest();
    }

    @Override protected void cleanupSessionForNextTest() { }

    @Override public void tearDown() {
        try {
            super.tearDown();
        } finally {
            if (savedProperties) {
                restoreProperty(FREE_EMAIL, previousFreeEmail);
                restoreProperty(TEST_OTP, previousTestOtp);
            }
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) System.clearProperty(name);
        else System.setProperty(name, value);
    }
}
