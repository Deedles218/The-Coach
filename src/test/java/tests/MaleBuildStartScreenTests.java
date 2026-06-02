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
import lib.ui.MainPageObject;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;

import java.util.HashMap;
import java.util.Map;

@Epic(value = "The Coach male build")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MaleBuildStartScreenTests extends CoreTestCase {
    private static final String MALE_LOGIN_EMAIL = "ds@vamapps.com";
    private static final String OTP_CODE = "8654";
    private static final String BUNDLE_ID = "com.vamapps.preprod.The-Coach";

    private static final String START_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'FEEL THE NEW LEVEL') or contains(@name, 'Feel the new level')]";
    private static final String START_BUTTON = "id:GET STARTED";
    private static final String LOGIN_BUTTON = "id:LOG IN";
    private static final String TERMS_LINK = "id:TERMS";
    private static final String PRIVACY_LINK = "id:PRIVACY";
    private static final String TERMS_SCREEN = "xpath://XCUIElementTypeButton[@name='BackButton'] | //XCUIElementTypeButton[@name='PageFormatMenuButton'] | //XCUIElementTypeStaticText[contains(@name, 'TERMS') or contains(@name, 'Terms')]";
    private static final String PRIVACY_SCREEN = "xpath://XCUIElementTypeStaticText[contains(@name, 'PRIVACY POLICY') or contains(@name, 'Privacy Policy')]";
    private static final String ONBOARDING_GOALS_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'WHAT DO YOU WANT TO ACHIEVE') or contains(@name, 'What do you want to achieve')]";
    private static final String ONBOARDING_BACK_BUTTON = "id:ic outline chevron left";
    private static final String LOGIN_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'ENTER THE MAIL THAT IS LINKED') or contains(@name, 'Enter the mail that is linked')]";
    private static final String LOGIN_EMAIL_INPUT = "xpath://XCUIElementTypeTextField";
    private static final String LOGIN_CONTINUE_BUTTON = "id:CONTINUE";
    private static final String OTP_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[@name='ENTER SECURITY CODE']";
    private static final String AUTHORIZED_DASHBOARD_MARKER = "xpath://XCUIElementTypeButton[@name='UserProfileImage'] | //XCUIElementTypeButton[@name='Today'] | //XCUIElementTypeButton[@name='Feed']";
    private static final String CLOSE_LOGIN_BUTTON = "id:CloseRoundBlack";
    private static final String PROFILE_BUTTON = "id:UserProfileImage";
    private static final String PROFILE_SCREEN = "xpath://XCUIElementTypeNavigationBar[contains(@name, 'UserProfileView')] | //XCUIElementTypeStaticText[@name='Account Settings']";
    private static final String LOG_OUT_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Log Out']";
    private static final String LOG_OUT_CONFIRM_BUTTON = "id:YES";

    @Test
    @Features(value = {@Feature(value = "Male start screen"), @Feature(value = "Legal links"), @Feature(value = "Onboarding entry")})
    @DisplayName("Male build start screen buttons and links")
    @Description("Verifies male build start screen actions: Terms, Privacy, Get Started, and Login entry points.")
    @Step("Start test test01MaleBuildStartScreenButtonsAndLinks")
    @Severity(value = SeverityLevel.BLOCKER)
    public void test01MaleBuildStartScreenButtonsAndLinks() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        MainPageObject page = new MainPageObject(driver);
        ensureStartScreen(page);

        page.waitForElementAndClick(TERMS_LINK, "Cannot tap Terms link on male start screen", 10);
        page.waitForElementPresent(TERMS_SCREEN, "Terms screen did not open from male start screen", 20);
        returnToAppStartScreen(page);

        page.waitForElementAndClick(PRIVACY_LINK, "Cannot tap Privacy link on male start screen", 10);
        page.waitForElementPresent(PRIVACY_SCREEN, "Privacy screen did not open from male start screen", 20);
        returnToAppStartScreen(page);

        page.waitForElementAndClick(START_BUTTON, "Cannot tap Get Started on male start screen", 10);
        page.waitForElementPresent(ONBOARDING_GOALS_TITLE, "Male onboarding goals screen did not open", 15);
        page.waitForElementAndClick(ONBOARDING_BACK_BUTTON, "Cannot return from male onboarding goals screen", 10);
        assertStartScreen(page);

        page.waitForElementAndClick(LOGIN_BUTTON, "Cannot tap Login on male start screen", 10);
        page.waitForElementPresent(LOGIN_SCREEN_TITLE, "Male login screen did not open", 10);
        page.waitForElementAndClick(CLOSE_LOGIN_BUTTON, "Cannot close male login screen", 10);
        assertStartScreen(page);
    }

    @Test
    @Features(value = {@Feature(value = "Male start screen"), @Feature(value = "Authorization")})
    @DisplayName("Male build login with ds account and OTP")
    @Description("Logs in on the male build using ds@vamapps.com and OTP 8654, then verifies the authorized dashboard.")
    @Step("Start test test02MaleBuildLoginWithDsAccountAndOtp")
    @Severity(value = SeverityLevel.BLOCKER)
    public void test02MaleBuildLoginWithDsAccountAndOtp() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        MainPageObject page = new MainPageObject(driver);
        ensureStartScreen(page);

        page.waitForElementAndClick(LOGIN_BUTTON, "Cannot tap Login on male start screen", 10);
        page.waitForElementPresent(LOGIN_SCREEN_TITLE, "Male login screen did not open", 10);
        page.waitForElementAndSendKeys(LOGIN_EMAIL_INPUT, MALE_LOGIN_EMAIL, "Cannot type male build login email", 10);
        hideKeyboardIfPossible();
        page.waitForElementAndClick(LOGIN_CONTINUE_BUTTON, "Cannot submit male build login email", 10);
        page.waitForElementPresent(OTP_SCREEN_TITLE, "Male build OTP screen did not open", 20);
        typeOtpCode(OTP_CODE);
        page.waitForElementPresent(AUTHORIZED_DASHBOARD_MARKER, "Male build did not open authorized dashboard after OTP", 30);
    }

    private void ensureStartScreen(MainPageObject page) {
        if (page.isElementPresent(START_SCREEN_TITLE)) {
            assertStartScreen(page);
            return;
        }

        if (page.isElementPresent(AUTHORIZED_DASHBOARD_MARKER)) {
            logOutFromAuthorizedState(page);
            assertStartScreen(page);
            return;
        }

        if (page.isElementPresent(CLOSE_LOGIN_BUTTON)) {
            page.waitForElementAndClick(CLOSE_LOGIN_BUTTON, "Cannot close male login flow", 10);
            assertStartScreen(page);
            return;
        }

        reactivateApp();
        if (page.isElementPresent(START_SCREEN_TITLE)) {
            assertStartScreen(page);
            return;
        }

        Assert.assertTrue(
                "Male build start screen is not visible. Log out before running these tests.",
                page.isElementPresent(START_SCREEN_TITLE)
        );
    }

    private void assertStartScreen(MainPageObject page) {
        page.waitForElementPresent(START_SCREEN_TITLE, "Male start screen title is not displayed", 20);
        page.waitForElementPresent(START_BUTTON, "Get Started button is not displayed on male start screen", 10);
        page.waitForElementPresent(LOGIN_BUTTON, "Login button is not displayed on male start screen", 10);
        page.waitForElementPresent(TERMS_LINK, "Terms link is not displayed on male start screen", 10);
        page.waitForElementPresent(PRIVACY_LINK, "Privacy link is not displayed on male start screen", 10);
    }

    private void logOutFromAuthorizedState(MainPageObject page) {
        page.waitForElementAndClick(PROFILE_BUTTON, "Cannot open male profile from authorized dashboard", 10);
        page.waitForElementPresent(PROFILE_SCREEN, "Male profile screen did not open", 15);
        swipeUpToFindLogoutButton(page);
        page.waitForElementAndClick(LOG_OUT_BUTTON, "Cannot tap Log Out on male profile screen", 10);
        if (page.isElementPresent(LOG_OUT_CONFIRM_BUTTON)) {
            page.waitForElementAndClick(LOG_OUT_CONFIRM_BUTTON, "Cannot confirm male profile logout", 10);
        }
        page.waitForElementPresent(START_SCREEN_TITLE, "Male start screen did not open after logout", 20);
    }

    private void swipeUpToFindLogoutButton(MainPageObject page) {
        int alreadySwiped = 0;
        while (!page.isElementPresent(LOG_OUT_BUTTON)) {
            if (alreadySwiped >= 5) {
                page.waitForElementPresent(LOG_OUT_BUTTON, "Cannot find Log Out button on male profile screen", 1);
                return;
            }
            mobileSwipeUp();
            alreadySwiped++;
        }
    }

    private void returnToAppStartScreen(MainPageObject page) {
        driver.navigate().back();
        reactivateApp();
        page.waitForElementPresent(START_SCREEN_TITLE, "Could not return to male start screen", 20);
        assertStartScreen(page);
    }

    private void typeOtpCode(String code) {
        for (int i = 0; i < code.length(); i++) {
            String digit = String.valueOf(code.charAt(i));
            try {
                new MainPageObject(driver).waitForElementAndClick(
                        "id:" + digit,
                        "Cannot tap OTP digit " + digit,
                        2
                );
            } catch (TimeoutException e) {
                WebElement activeElement = driver.switchTo().activeElement();
                activeElement.sendKeys(digit);
            }
        }
    }

    private void hideKeyboardIfPossible() {
        try {
            Map<String, Object> args = new HashMap<String, Object>();
            ((JavascriptExecutor) driver).executeScript("mobile: hideKeyboard", args);
        } catch (Exception e) {
            System.out.println("Keyboard was not hidden automatically; continuing.");
        }
    }

    private void reactivateApp() {
        try {
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("bundleId", BUNDLE_ID);
            ((JavascriptExecutor) driver).executeScript("mobile: activateApp", args);
        } catch (Exception e) {
            System.out.println("App reactivation was not needed or failed: " + e.getMessage());
        }
    }

    private void mobileSwipeUp() {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("direction", "up");
        ((JavascriptExecutor) driver).executeScript("mobile: swipe", args);
    }
}
