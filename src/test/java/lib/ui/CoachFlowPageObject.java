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
            PROFILE_USERNAME,
            PROFILE_PREMIUM_BADGE,
            PROFILE_ACCOUNT_SETTINGS_BUTTON,
            PROFILE_ACCOUNT_SETTINGS_SCREEN,
            PROFILE_SUPPORT_BUTTON,
            PROFILE_SUPPORT_SCREEN,
            PROFILE_SUPPORT_FAQ_BUTTON,
            PROFILE_MY_WORKBOOK_BUTTON,
            PROFILE_MY_WORKBOOK_SCREEN,
            PROFILE_MY_WORKBOOK_CLOSE_BUTTON,
            PROFILE_FAQ_BUTTON,
            PROFILE_FAQ_SCREEN,
            PROFILE_TERMS_BUTTON,
            PROFILE_TERMS_SCREEN,
            PROFILE_BROWSER_CLOSE_BUTTON,
            PROFILE_SUBSCREEN_BACK_BUTTON,
            DELETE_ACCOUNT_BUTTON,
            DELETE_ACCOUNT_CONFIRM_TITLE,
            DELETE_ACCOUNT_CANCEL_BUTTON,
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

    @Step("Verify Profile screen content")
    public void assertProfileScreenIsDisplayed() {
        this.waitForElementPresent(PROFILE_SCREEN, "Profile navigation bar is not displayed", 10);
        this.waitForElementPresent(PROFILE_USERNAME, "Profile username is not displayed", 10);
        this.waitForElementPresent(PROFILE_PREMIUM_BADGE, "Profile premium badge is not displayed", 10);
        this.waitForElementPresent(PROFILE_ACCOUNT_SETTINGS_BUTTON, "Account Settings item is not displayed", 10);
        this.waitForElementPresent(PROFILE_SUPPORT_BUTTON, "Support item is not displayed", 10);
        this.waitForElementPresent(PROFILE_MY_WORKBOOK_BUTTON, "My workbook item is not displayed", 10);
        this.waitForElementPresent(PROFILE_FAQ_BUTTON, "FAQ item is not displayed", 10);
        this.waitForElementPresent(PROFILE_TERMS_BUTTON, "Terms & privacy policy item is not displayed", 10);
    }

    @Step("Open Account Settings from Profile")
    public void openAccountSettingsFromProfile() {
        this.waitForElementAndClick(PROFILE_ACCOUNT_SETTINGS_BUTTON, "Cannot tap Account Settings", 10);
        this.waitForElementPresent(PROFILE_ACCOUNT_SETTINGS_SCREEN, "Account Settings screen did not open", 15);
    }

    @Step("Open Support from Profile")
    public void openSupportFromProfile() {
        this.waitForElementAndClick(PROFILE_SUPPORT_BUTTON, "Cannot tap Support", 10);
        this.waitForElementPresent(PROFILE_SUPPORT_SCREEN, "Support screen did not open", 15);
        this.waitForElementPresent(PROFILE_SUPPORT_FAQ_BUTTON, "Support FAQ entry is not visible", 10);
    }

    @Step("Open My workbook from Profile")
    public void openMyWorkbookFromProfile() {
        this.waitForElementAndClick(PROFILE_MY_WORKBOOK_BUTTON, "Cannot tap My workbook", 10);
        this.waitForElementPresent(PROFILE_MY_WORKBOOK_SCREEN, "My workbook screen did not open", 15);
    }

    @Step("Close My workbook screen")
    public void closeMyWorkbookScreen() {
        this.waitForElementAndClick(PROFILE_MY_WORKBOOK_CLOSE_BUTTON, "Cannot close My workbook screen", 10);
        this.assertProfileScreenIsDisplayed();
    }

    @Step("Open FAQ from Profile")
    public void openFaqFromProfile() {
        this.waitForElementAndClick(PROFILE_FAQ_BUTTON, "Cannot tap FAQ", 10);
        this.waitForElementPresent(PROFILE_FAQ_SCREEN, "FAQ browser did not open", 20);
    }

    @Step("Open Terms and privacy policy from Profile")
    public void openTermsFromProfile() {
        this.waitForElementAndClick(PROFILE_TERMS_BUTTON, "Cannot tap Terms & privacy policy", 10);
        this.waitForElementPresent(PROFILE_TERMS_SCREEN, "Terms browser did not open", 20);
    }

    @Step("Return to Profile screen from an internal Profile screen")
    public void returnToProfileFromSubscreen() {
        this.waitForElementAndClick(PROFILE_SUBSCREEN_BACK_BUTTON, "Cannot return to Profile screen", 10);
        this.assertProfileScreenIsDisplayed();
    }

    @Step("Close Profile browser screen")
    public void closeProfileBrowser() {
        this.waitForElementAndTapNearLeftEdge(PROFILE_BROWSER_CLOSE_BUTTON, "Cannot close browser screen", 10);
        this.assertProfileScreenIsDisplayed();
    }

    @Step("Verify Delete Account confirmation and cancel it")
    public void verifyDeleteAccountDialogAndCancel() {
        this.swipeUpToFindElement(DELETE_ACCOUNT_BUTTON, "Cannot find Delete my account button", 4);
        this.waitForElementAndClick(DELETE_ACCOUNT_BUTTON, "Cannot tap Delete my account", 10);
        this.waitForElementPresent(DELETE_ACCOUNT_CONFIRM_TITLE, "Delete account confirmation did not open", 10);
        // Account deletion is destructive and must never be confirmed by automated tests.
        this.waitForElementAndClick(DELETE_ACCOUNT_CANCEL_BUTTON, "Cannot cancel Delete account confirmation", 10);
        this.assertProfileScreenIsDisplayed();
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
            if (this.isElementPresent(LOG_OUT_CONFIRM_BUTTON)) {
                this.waitForElementAndClick(LOG_OUT_CONFIRM_BUTTON, "Cannot confirm logout", 10);
            } else {
                System.out.println("Logout confirmation dialog was not shown; continuing.");
            }
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
