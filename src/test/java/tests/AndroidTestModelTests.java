package tests;

import lib.*;
import lib.ui.android.*;
import lib.ui.OnboardingGoal;
import org.junit.*;
import io.qameta.allure.*;

/** Android equivalents of applicable mail scenarios. Selector parity is in AndroidProgramSelectorTests.
 * Postpone/remove/recovery and Coach for Her tooltip are not Android male features. */
public class AndroidTestModelTests extends AndroidTestCase {
    @Test @Issue("COA-7947") public void testValidEmailEnablesContinue() {
        AndroidCoachFlowPageObject coach=new AndroidCoachFlowPageObject(driver);
        coach.ensureLoggedOutOnStartScreen();coach.openLoginFlow();
        coach.typeLoginEmail(TestData.validEmailWithoutProgress());
        coach.assertLoginContinueButtonIsEnabled();coach.returnFromLoginFlowToStartScreen();
    }
    private AndroidCoachFlowPageObject anonymousConnect() {
        Assert.assertTrue("Anonymous CONNECT scenarios require a fresh Android installation",Platform.getInstance().isFreshAndroidInstallConfigured());
        AndroidOnboardingPageObject onboarding=new AndroidOnboardingPageObject(driver);
        onboarding.completeNewUserJourneyToPaywall(OnboardingGoal.BEAT_PREMATURE_EJACULATION);
        onboarding.closePaywallsAndPopups();onboarding.waitForToday();
        AndroidCoachFlowPageObject coach=new AndroidCoachFlowPageObject(driver);
        coach.openToday();coach.openProfile();
        coach.waitForElementVisible("id:com.vamapps.thecoach:id/btnConnect","Expected a new anonymous profile",15);
        coach.openLoginFlow();return coach;
    }
    @Test @Issue("COA-7949") public void testAnonymousConnectAuthorizesExistingAccount() throws Exception {
        AndroidCoachFlowPageObject coach=anonymousConnect();
        TestData.TestAccount account=TestData.existingProgressAccount();
        coach.typeLoginEmail(account.getEmail());coach.assertLoginContinueButtonIsEnabled();coach.submitLoginEmail();
        coach.assertOtpScreenIsDisplayedForEmail(account.getEmail());coach.typeSecurityCode(account.getOtp());
        coach.closeOtpConfirmationIfPresent();coach.assertAuthorizedDashboardIsDisplayed();
        Assert.assertEquals("CONNECT authorized a different identity",System.getenv("COACH_EXPECTED_ACCOUNT_UID"),AndroidModuleEvidence.currentUid());
    }
    @Test @Issue("COA-7950") public void testAnonymousConnectRejectsInvalidEmailWithoutNavigation() {
        AndroidCoachFlowPageObject coach=anonymousConnect();
        coach.typeLoginEmail(TestData.invalidEmail());coach.assertLoginContinueButtonIsDisabled();
        coach.assertDisabledLoginContinueIsNoOp();
        coach.waitForElementVisible("id:com.vamapps.thecoach:id/edtEmail","Invalid email left the CONNECT form",10);
    }
}
