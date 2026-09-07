package lib.ui.ios;

import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.MainPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import java.util.Collections;

/** Account linking through the approved preprod UI. Never deletes accounts. */
public final class iOSTestAccountPageObject extends MainPageObject {
    private static final String MAIL_TITLE = "id:ENTER YOUR EMAIL TO SYNC YOUR PROGRESS AND SETTINGS";
    public iOSTestAccountPageObject(RemoteWebDriver driver) { super(driver); }

    /** Mail form for connecting an anonymous profile, not the Welcome login form. */
    public void openAnonymousEmailForm() {
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        if (!isElementVisible("xpath://XCUIElementTypeNavigationBar[contains(@name, 'UserProfileView')]")) coach.openProfile();
        waitForElementAndClick("id:CONNECT", "Anonymous profile must expose CONNECT", 10);
        waitForElementPresent(MAIL_TITLE, "CONNECT opened an unexpected mail form", 10);
        waitForElementPresent("xpath://XCUIElementTypeTextField", "Connect mail form did not open", 10);
    }

    public void finishMailEditing() {
        waitForElementAndClick(MAIL_TITLE, "Cannot finish editing the CONNECT mail form", 10);
    }

    public void assertDisabledMailContinueIsNoOp() {
        tapDisabledAndAssertNoTransition(new String[]{"id:CONTINUE"}, new String[]{MAIL_TITLE},
                new String[]{"id:ENTER SECURITY CODE", "id:WE’VE FOUND AN EXISTING ACCOUNT"},
                "Disabled CONNECT Continue", 10);
    }

    // No credential argument is captured in Allure step metadata.
    public void assertExistingAccountPrompt(String email) {
        waitForElementPresent("id:WE’VE FOUND AN EXISTING ACCOUNT", "Existing-account prompt is absent", 10);
        // The prompt is animated: re-query detached text nodes within the same
        // bounded wait, without re-submitting email or repeating the login action.
        createWait(10).ignoring(StaleElementReferenceException.class)
                .withMessage("Existing-account prompt does not identify the requested email").until(ignored -> {
            for (WebElement text : driver.findElements(By.className("XCUIElementTypeStaticText"))) {
                if (text.isDisplayed() && email.equals(text.getAttribute("label"))) return true;
            }
            return false;
        });
    }

    /** Setup login only. This does not replace COA-7949's strict Send code assertion. */
    public void loginExistingAccountFromAnonymousProfile(TestData.TestAccount account) {
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        openAnonymousEmailForm();
        coach.typeLoginEmail(account.getEmail());
        waitForElementAndClick("id:CONTINUE", "Cannot look up the approved existing account", 10);
        assertExistingAccountPrompt(account.getEmail());
        waitForElementAndClick("id:LOGIN", "Cannot log into the approved existing account", 10);
        coach.assertOtpScreenIsDisplayedForEmail(account.getEmail());
        coach.typeSecurityCode(account.getOtp());
        coach.completeAuthorizationAfterSecurityCode();
    }

    public boolean resumeAnonymousMailProfile() {
        String warning = "xpath://XCUIElementTypeStaticText[contains(@name, 'Your account is currently anonymous')]";
        if (isElementVisible(warning)) {
            waitForElementAndClick("id:CANCEL", "Cannot preserve the anonymous mail fixture", 5);
        }
        String profile = "xpath://XCUIElementTypeNavigationBar[contains(@name, 'UserProfileView')]";
        if (!isElementVisible(profile)) return false;
        for (int i = 0; i < 4 && !isElementVisible("id:CONNECT"); i++) {
            ((JavascriptExecutor) driver).executeScript("mobile: swipe", Collections.singletonMap("direction", "down"));
        }
        return isElementVisible("id:CONNECT");
    }

    public void preserveAnonymousMailProfile() {
        for (int i = 0; i < 3 && isElementVisible("id:CloseRoundBlack"); i++) {
            waitForElementAndClick("id:CloseRoundBlack", "Cannot close the interrupted mail flow", 5);
        }
        Assert.assertTrue("Anonymous mail fixture was not preserved", resumeAnonymousMailProfile());
    }

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
