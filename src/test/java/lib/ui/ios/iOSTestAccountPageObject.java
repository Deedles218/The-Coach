package lib.ui.ios;

import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.MainPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.JavascriptExecutor;
import java.util.Collections;

/** Account linking through the approved preprod UI. Never deletes accounts. */
public final class iOSTestAccountPageObject extends MainPageObject {
    public iOSTestAccountPageObject(RemoteWebDriver driver) { super(driver); }

    public boolean resumeAnonymousRegistration(TestData.TestAccount account) {
        String anonymousLogout = "xpath://XCUIElementTypeStaticText[contains(@name, 'Your account is currently anonymous')]";
        if (isElementVisible(anonymousLogout)) {
            waitForElementAndClick("id:CANCEL", "Cannot preserve pending anonymous registration", 5);
        }
        if (isElementVisible("xpath://XCUIElementTypeNavigationBar[contains(@name, 'UserProfileView')]")) {
            for (int i = 0; i < 4 && !isElementVisible("id:CONNECT"); i++) {
                ((JavascriptExecutor) driver).executeScript("mobile: swipe", Collections.singletonMap("direction", "down"));
            }
        }
        if (isElementVisible("id:CONNECT")) {
            linkAndVerify(account);
            return true;
        }
        return false;
    }

    public boolean loginIfRegistered(TestData.TestAccount account) {
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        coach.openLoginFlow();
        coach.typeLoginEmail(account.getEmail());
        waitForElementAndClick("id:CONTINUE", "Account lookup Continue is not enabled", 10);
        String missing = "xpath://XCUIElementTypeStaticText[contains(@name, 'Email not found')]";
        waitForFirstElementPresent(new String[]{"id:ENTER SECURITY CODE", missing},
                "Account lookup returned neither OTP nor Email not found", 20);
        if (isElementVisible(missing)) {
            coach.returnFromLoginFlowToStartScreen();
            return false;
        }
        coach.typeSecurityCode(account.getOtp());
        coach.completeAuthorizationAfterSecurityCode();
        coach.openProfile();
        Assert.assertFalse("Existing test account is not linked", isElementVisible("id:CONNECT"));
        return true;
    }

    // No @Step on credential-bearing methods: Allure records arguments.
    public void linkAndVerify(TestData.TestAccount account) {
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        if (!isElementVisible("id:CONNECT")) coach.openProfile();
        waitForElementAndClick("id:CONNECT", "New test profile must expose CONNECT", 10);
        WebElement email = waitForElementPresent("xpath://XCUIElementTypeTextField",
                "Connect email input is not visible", 10);
        email.sendKeys(account.getEmail());
        coach.hideKeyboardIfPossible();
        waitForElementAndClick("id:CONTINUE", "Connect email Continue is not enabled", 10);
        coach.assertOtpScreenIsDisplayedForEmail(account.getEmail());
        coach.typeSecurityCode(account.getOtp());
        waitForElementPresent("id:Your Email is confirmed!", "New email was not confirmed", 20);
        waitForElementAndClick("id:CLOSE", "Cannot close email confirmation", 5);
        waitForElementNotPresent("id:CONNECT", "Profile still asks to connect an email", 10);
        coach.logOut();
        coach.loginWithEmailAndOtp(account.getEmail(), account.getOtp());
        coach.openProfile();
        Assert.assertFalse("Re-login did not restore the confirmed profile", isElementVisible("id:CONNECT"));
    }
}
