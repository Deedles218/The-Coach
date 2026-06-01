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
    private static final String LOGIN_EMAIL = "favika9669@fpxnet.com";
    private static final String OTP_CODE = "8654";

    @Test
    @Features(value = {@Feature(value = "Start screen"), @Feature(value = "Authorization")})
    @DisplayName("Authorize from the start screen with email and OTP")
    @Description("Verifies that Login opens the authorization flow, email opens OTP entry, and OTP proceeds to the post-auth screen.")
    @Step("Start test testAuthorizeWithEmailAndOtp")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testAuthorizeWithEmailAndOtp() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject CoachFlowPageObject = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", CoachFlowPageObject);

        CoachFlowPageObject.waitForStartScreen();
        CoachFlowPageObject.openLoginFlow();
        CoachFlowPageObject.typeLoginEmail(LOGIN_EMAIL);
        CoachFlowPageObject.submitLoginEmail();
        CoachFlowPageObject.typeSecurityCode(OTP_CODE);
        CoachFlowPageObject.waitForPostAuthorizationScreen();
    }
}
