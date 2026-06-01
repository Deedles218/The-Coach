package lib.ui;

import io.qameta.allure.Step;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.RemoteWebDriver;

abstract public class CoachFlowPageObject extends MainPageObject {
    protected static String
            TAB_FEED,
            FEED_SCREEN_TITLE,
            FEED_DEPRECATION_POPUP_TITLE,
            FEED_DEPRECATION_POPUP_CONFIRM_BUTTON,
            PROFILE_BUTTON,
            PROFILE_SCREEN,
            LOG_OUT_BUTTON,
            LOG_OUT_CONFIRM_TITLE,
            LOG_OUT_CONFIRM_BUTTON,
            START_SCREEN_TITLE,
            START_BUTTON,
            LOGIN_BUTTON,
            LOGIN_SCREEN_TITLE,
            LOGIN_EMAIL_INPUT,
            LOGIN_CONTINUE_BUTTON,
            OTP_SCREEN_TITLE,
            OTP_CODE_INPUT,
            POST_AUTH_ONBOARDING_MARKER,
            CLOSE_LOGIN_BUTTON,
            ONBOARDING_GOALS_TITLE,
            ONBOARDING_BACK_BUTTON,
            AUTHORIZED_DASHBOARD_MARKER;

    public CoachFlowPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Step("Verify that the authorized dashboard is displayed")
    public void assertAuthorizedDashboardIsDisplayed() {
        this.waitForElementPresent(
                AUTHORIZED_DASHBOARD_MARKER,
                "Expected authorized The Coach dashboard before starting Feed/logout flow",
                15
        );
    }

    @Step("Open Feed screen")
    public void openFeed() {
        this.waitForElementAndClick(TAB_FEED, "Cannot find and tap Feed tab", 10);
        this.waitForElementPresent(FEED_SCREEN_TITLE, "Feed screen did not open", 10);
    }

    @Step("Verify Feed deprecation popup is displayed")
    public void assertFeedDeprecationPopupIsDisplayed() {
        this.waitForElementPresent(
                FEED_DEPRECATION_POPUP_TITLE,
                "Feed deprecation popup is not displayed",
                10
        );
    }

    @Step("Close Feed deprecation popup")
    public void closeFeedDeprecationPopup() {
        this.waitForElementAndClick(
                FEED_DEPRECATION_POPUP_CONFIRM_BUTTON,
                "Cannot find and tap Feed deprecation popup confirm button",
                10
        );
        this.waitForElementNotPresent(
                FEED_DEPRECATION_POPUP_TITLE,
                "Feed deprecation popup is still visible after confirmation",
                10
        );
    }

    @Step("Open Profile screen")
    public void openProfile() {
        this.waitForElementAndClick(PROFILE_BUTTON, "Cannot find and tap profile button", 10);
        this.waitForElementPresent(PROFILE_SCREEN, "Profile screen did not open", 10);
    }

    @Step("Log out from Profile screen")
    public void logOut() {
        this.swipeUpToFindElement(LOG_OUT_BUTTON, "Cannot find Log Out button on Profile screen", 4);
        this.waitForElementAndClick(LOG_OUT_BUTTON, "Cannot tap Log Out button", 10);
        this.confirmLogoutIfNeeded();
        this.waitForStartScreen();
    }

    @Step("Confirm logout if confirmation dialog appears")
    public void confirmLogoutIfNeeded() {
        try {
            this.waitForElementPresent(LOG_OUT_CONFIRM_TITLE, "Logout confirmation dialog did not appear", 3);
            this.waitForElementAndClick(LOG_OUT_CONFIRM_BUTTON, "Cannot confirm logout", 10);
        } catch (TimeoutException e) {
            System.out.println("Logout confirmation dialog was not shown; continuing.");
        }
    }

    @Step("Verify start screen is displayed")
    public void waitForStartScreen() {
        this.waitForElementPresent(START_SCREEN_TITLE, "Start screen title is not displayed", 20);
        this.waitForElementPresent(LOGIN_BUTTON, "Login button is not displayed on start screen", 10);
        this.waitForElementPresent(START_BUTTON, "Start button is not displayed on start screen", 10);
    }

    @Step("Open login flow from start screen")
    public void openLoginFlow() {
        this.waitForElementAndClick(LOGIN_BUTTON, "Cannot tap Login button on start screen", 10);
        this.waitForElementPresent(LOGIN_SCREEN_TITLE, "Login flow did not open after tapping Login", 10);
    }

    @Step("Type email into login flow")
    public void typeLoginEmail(String email) {
        this.waitForElementAndSendKeys(
                LOGIN_EMAIL_INPUT,
                email,
                "Cannot find email input on login screen",
                10
        );
    }

    @Step("Submit email in login flow")
    public void submitLoginEmail() {
        this.waitForElementAndClick(LOGIN_CONTINUE_BUTTON, "Cannot tap Continue on login screen", 10);
        this.waitForElementPresent(OTP_SCREEN_TITLE, "Security code screen did not open after submitting email", 20);
    }

    @Step("Type security code")
    public void typeSecurityCode(String code) {
        this.waitForElementPresent(OTP_SCREEN_TITLE, "Security code screen is not displayed", 10);
        for (int i = 0; i < code.length(); i++) {
            String digit = String.valueOf(code.charAt(i));
            this.waitForElementAndClick(
                    "id:" + digit,
                    "Cannot tap keyboard key '" + digit + "' for security code",
                    10
            );
        }
    }

    @Step("Verify authorization moved past OTP screen")
    public void waitForPostAuthorizationScreen() {
        this.waitForElementPresent(
                POST_AUTH_ONBOARDING_MARKER,
                "Authorization did not proceed to the expected post-auth screen",
                30
        );
    }

    @Step("Return from login flow to start screen")
    public void returnFromLoginFlowToStartScreen() {
        this.waitForElementAndClick(CLOSE_LOGIN_BUTTON, "Cannot close login flow", 10);
        this.waitForStartScreen();
    }

    @Step("Open onboarding flow from start screen")
    public void openStartFlow() {
        this.waitForElementAndClick(START_BUTTON, "Cannot tap Start button on start screen", 10);
        this.waitForElementPresent(
                ONBOARDING_GOALS_TITLE,
                "Onboarding goals screen did not open after tapping Start",
                10
        );
    }

    @Step("Return from onboarding flow to start screen")
    public void returnFromOnboardingFlowToStartScreen() {
        this.waitForElementAndClick(ONBOARDING_BACK_BUTTON, "Cannot return from onboarding flow", 10);
        this.waitForStartScreen();
    }
}
