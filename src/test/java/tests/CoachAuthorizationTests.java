package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.ui.CoachFlowPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

@Epic(value = "The Coach authorization")
public class CoachAuthorizationTests extends CoreTestCase {
    private static final String EXISTING_PROGRESS_EMAIL = "ds@vamapps.com";
    private static final String OTP_CODE = "8654";
    private static final String VALID_EMAIL_WITHOUT_PROGRESS = "test1@vamapps.com";
    private static final String INVALID_EMAIL = "not-an-email";

    @Test
    @Features(value = {@Feature(value = "Start screen"), @Feature(value = "Authorization")})
    @DisplayName("COA-7954 COA-7948 Login entry opens email form and empty email keeps Continue disabled")
    @Description("Starts from logged-out state, opens Login from the welcome screen, and verifies Continue is disabled when email is empty.")
    @Step("Start test test01LoginEntryAndEmptyEmailContinueDisabled")
    @Severity(value = SeverityLevel.BLOCKER)
    public void test01LoginEntryAndEmptyEmailContinueDisabled() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.openLoginFlow();
        coachFlow.assertLoginContinueButtonIsDisabled();
        coachFlow.returnFromLoginFlowToStartScreen();
    }

    @Test
    @Features(value = {@Feature(value = "Authorization"), @Feature(value = "Email validation")})
    @DisplayName("COA-7950 Invalid email does not enable Continue")
    @Description("Verifies the mail form rejects an invalid email format before OTP is requested.")
    @Step("Start test test02InvalidEmailKeepsContinueDisabled")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test02InvalidEmailKeepsContinueDisabled() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.openLoginFlow();
        coachFlow.typeLoginEmail(INVALID_EMAIL);
        coachFlow.assertLoginContinueButtonIsDisabled();
        coachFlow.returnFromLoginFlowToStartScreen();
    }

    @Test
    @Features(value = {@Feature(value = "Authorization"), @Feature(value = "OTP")})
    @DisplayName("COA-7949 COA-7951 Valid email opens OTP screen and Cancel returns to start")
    @Description("Submits a valid non-progress email, verifies the OTP screen, then cancels back to the start screen without logging in.")
    @Step("Start test test03ValidEmailOpensOtpAndCancelReturnsToStart")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test03ValidEmailOpensOtpAndCancelReturnsToStart() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.openLoginFlow();
        coachFlow.typeLoginEmail(VALID_EMAIL_WITHOUT_PROGRESS);
        coachFlow.assertLoginContinueButtonIsEnabled();
        coachFlow.submitLoginEmail();
        coachFlow.assertOtpScreenIsDisplayedForEmail(VALID_EMAIL_WITHOUT_PROGRESS);
        coachFlow.returnFromOtpFlowToStartScreen();
    }

    @Test
    @Features(value = {@Feature(value = "Authorization"), @Feature(value = "OTP")})
    @DisplayName("COA-7952 Resend code is available on OTP screen")
    @Description("Verifies the Resend code action remains available after requesting an OTP for a valid non-progress email.")
    @Step("Start test test04ResendCodeOnOtpScreen")
    @Severity(value = SeverityLevel.NORMAL)
    public void test04ResendCodeOnOtpScreen() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.openLoginFlow();
        coachFlow.typeLoginEmail(VALID_EMAIL_WITHOUT_PROGRESS);
        coachFlow.submitLoginEmail();
        coachFlow.assertOtpScreenIsDisplayedForEmail(VALID_EMAIL_WITHOUT_PROGRESS);
        coachFlow.resendSecurityCode();
        coachFlow.returnFromOtpFlowToStartScreen();
    }

    @Test
    @Features(value = {@Feature(value = "Authorization"), @Feature(value = "Existing user progress")})
    @DisplayName("COA-7935 Existing-progress user logs in with email and OTP")
    @Description("Logs in with ds@vamapps.com and OTP 8654, then verifies the authorized dashboard is displayed.")
    @Step("Start test test05ExistingProgressUserLoginWithEmailAndOtp")
    @Severity(value = SeverityLevel.BLOCKER)
    public void test05ExistingProgressUserLoginWithEmailAndOtp() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.loginWithEmailAndOtp(EXISTING_PROGRESS_EMAIL, OTP_CODE);
    }
}
