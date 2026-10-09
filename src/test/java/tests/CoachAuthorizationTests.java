package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.TestData;
import lib.UpdateEvidence;
import lib.ui.CoachFlowPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

@Epic(value = "The Coach authorization")
public class CoachAuthorizationTests extends CoreTestCase {
    @Test
    @Issue("COA-7944")
    @Features({@Feature("Start screen"), @Feature("Onboarding entry")})
    @DisplayName("COA-7944 START NOW opens the new-user questionnaire")
    @Description("Verifies START NOW is enabled on Welcome, opens the goal questionnaire rather than Login, and returns to Welcome.")
    @Severity(SeverityLevel.BLOCKER)
    public void testStartNowOpensQuestionnaireWithoutLogin() throws Exception {
        requireMobilePlatform();
        UpdateEvidence.attachInstalledBuild();
        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);
        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.assertStartButtonIsEnabled();
        coachFlow.openStartFlow();
        coachFlow.assertQuestionnaireOpenedWithoutLogin();
        coachFlow.returnFromOnboardingFlowToStartScreen();
    }

    @Test
    @Features(value = {@Feature(value = "Start screen"), @Feature(value = "Authorization")})
    @DisplayName("COA-7954 COA-7948 Login entry opens email form and empty email keeps Continue disabled")
    @Description("Starts from logged-out state, opens Login from the welcome screen, and verifies Continue is disabled when email is empty.")
    @Step("Start test test01LoginEntryAndEmptyEmailContinueDisabled")
    @Severity(value = SeverityLevel.BLOCKER)
    public void test01LoginEntryAndEmptyEmailContinueDisabled() {
        requireMobilePlatform();

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
        requireMobilePlatform();

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.openLoginFlow();
        coachFlow.typeLoginEmail(TestData.invalidEmail());
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
        requireMobilePlatform();

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.openLoginFlow();
        String validEmail = TestData.validEmailWithoutProgress();
        coachFlow.typeLoginEmail(validEmail);
        coachFlow.assertLoginContinueButtonIsEnabled();
        coachFlow.submitLoginEmail();
        coachFlow.assertOtpScreenIsDisplayedForEmail(validEmail);
        coachFlow.returnFromOtpFlowToStartScreen();
    }

    @Test
    @Features(value = {@Feature(value = "Authorization"), @Feature(value = "OTP")})
    @DisplayName("COA-7952 Resend code is available on OTP screen")
    @Description("Verifies the Resend code action remains available after requesting an OTP for a valid non-progress email.")
    @Step("Start test test04ResendCodeOnOtpScreen")
    @Severity(value = SeverityLevel.NORMAL)
    public void test04ResendCodeOnOtpScreen() {
        requireMobilePlatform();

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.openLoginFlow();
        String validEmail = TestData.validEmailWithoutProgress();
        coachFlow.typeLoginEmail(validEmail);
        coachFlow.submitLoginEmail();
        coachFlow.assertOtpScreenIsDisplayedForEmail(validEmail);
        coachFlow.resendSecurityCode();
        coachFlow.returnFromOtpFlowToStartScreen();
    }

    @Test
    @Features(value = {@Feature(value = "Authorization"), @Feature(value = "Existing user progress")})
    @DisplayName("COA-7935 Existing-progress user logs in with email and OTP")
    @Description("Logs in with the secret-backed existing-progress test account and verifies the authorized dashboard is displayed.")
    @Step("Start test test05ExistingProgressUserLoginWithEmailAndOtp")
    @Severity(value = SeverityLevel.BLOCKER)
    public void test05ExistingProgressUserLoginWithEmailAndOtp() {
        requireMobilePlatform();

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        TestData.TestAccount account = TestData.existingProgressAccount();
        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.loginWithEmailAndOtp(account.getEmail(), account.getOtp());
    }
}
