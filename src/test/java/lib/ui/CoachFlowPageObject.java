package lib.ui;

import io.qameta.allure.Step;
import lib.Platform;
import org.junit.Assert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.Map;

abstract public class CoachFlowPageObject extends MainPageObject {
    protected static String
            TAB_TODAY,
            TAB_EXPLORE,
            TAB_SHOP,
            TAB_FEED,
            FEED_SCREEN_TITLE,
            FEED_DEPRECATION_POPUP_TITLE,
            FEED_DEPRECATION_POPUP_CONFIRM_BUTTON,
            PROFILE_BUTTON,
            PROFILE_SCREEN,
            PROFILE_PREMIUM_BADGE,
            PROFILE_PROGRESS_EXERCISES,
            PROFILE_PROGRESS_LESSONS,
            PROFILE_PROGRESS_STREAK,
            PROFILE_PROGRESS_CHARTS,
            PROFILE_PROGRAM_SETTINGS_TITLE,
            PROFILE_PERSONALIZATION_BUTTON,
            PROFILE_MEAL_PLAN_BUTTON,
            PROFILE_WORKOUT_PLAN_BUTTON,
            PROFILE_SUPPLEMENTS_BUTTON,
            PROFILE_ACCOUNT_SETTINGS_BUTTON,
            PROFILE_ACCOUNT_SETTINGS_SCREEN,
            PROFILE_BACKED_BY_SCIENCE_BUTTON,
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
            OTP_EMAIL_SENT_TEXT,
            OTP_RESEND_CODE_BUTTON,
            OTP_CODE_INPUT,
            POST_AUTH_ONBOARDING_MARKER,
            CLOSE_LOGIN_BUTTON,
            NOTIFICATION_PROMPT_TITLE,
            NOTIFICATION_PROMPT_CLOSE_BUTTON,
            CONNECT_EMAIL_PROMPT_TITLE,
            CONNECT_EMAIL_PROMPT_LATER_BUTTON,
            PDF_GUIDE_UPSELL_TITLE,
            PDF_GUIDE_UPSELL_PRODUCT_TITLE,
            PDF_GUIDE_UPSELL_BUY_BUTTON,
            PDF_GUIDE_UPSELL_CLOSE_BUTTON,
            ONBOARDING_GOALS_TITLE,
            ONBOARDING_BACK_BUTTON,
            AUTHORIZED_DASHBOARD_MARKER;

    public CoachFlowPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Step("Ensure app is logged out and starts from the start screen")
    public void ensureLoggedOutOnStartScreen() {
        this.activateAppIfPossible();
        this.closePdfGuideUpsellIfPresent();
        this.closeNotificationPromptIfPresent();
        this.closeConnectEmailPromptIfPresent();

        if (this.isElementPresent(START_SCREEN_TITLE)) {
            this.waitForStartScreen();
            return;
        }

        if (this.isElementPresent(CLOSE_LOGIN_BUTTON)) {
            this.closeVisibleAuthFlow();
            if (this.isElementPresent(CLOSE_LOGIN_BUTTON)) {
                this.closeVisibleAuthFlow();
            }
            this.waitForStartScreen();
            return;
        }

        if (this.isElementPresent(ONBOARDING_BACK_BUTTON)) {
            this.waitForElementAndClick(ONBOARDING_BACK_BUTTON, "Cannot return from onboarding flow", 10);
            this.waitForStartScreen();
            return;
        }

        if (this.isElementPresent(PROFILE_SCREEN)) {
            this.logOut();
            return;
        }

        if (this.isElementPresent(AUTHORIZED_DASHBOARD_MARKER)) {
            this.openProfile();
            this.logOut();
            return;
        }

        this.waitForStartScreen();
    }

    @Step("Ensure existing-progress user is authorized")
    public void ensureExistingProgressUserIsLoggedIn(String email, String otpCode) {
        this.activateAppIfPossible();
        this.closePdfGuideUpsellIfPresent();
        this.closeNotificationPromptIfPresent();
        this.closeConnectEmailPromptIfPresent();

        if (this.isElementPresent(AUTHORIZED_DASHBOARD_MARKER)) {
            this.assertAuthorizedDashboardIsDisplayed();
            this.openToday();
            return;
        }

        if (this.isElementPresent(TAB_TODAY)) {
            this.openToday();
            return;
        }

        this.ensureLoggedOutOnStartScreen();
        this.loginWithEmailAndOtp(email, otpCode);
    }

    @Step("Log in with email and OTP")
    public void loginWithEmailAndOtp(String email, String otpCode) {
        this.openLoginFlow();
        this.typeLoginEmail(email);
        this.assertLoginContinueButtonIsEnabled();
        this.submitLoginEmail();
        this.assertOtpScreenIsDisplayedForEmail(email);
        this.typeSecurityCode(otpCode);
        this.waitForAuthorizedDashboard();
        this.closePdfGuideUpsellIfPresent();
        this.closeNotificationPromptIfPresent();
        this.openToday();
    }

    @Step("Verify that the authorized dashboard is displayed")
    public void assertAuthorizedDashboardIsDisplayed() {
        this.waitForElementPresent(
                AUTHORIZED_DASHBOARD_MARKER,
                "Expected authorized The Coach dashboard before starting Feed/logout flow",
                15
        );
    }

    @Step("Wait for authorized dashboard")
    public void waitForAuthorizedDashboard() {
        this.waitForElementPresent(
                AUTHORIZED_DASHBOARD_MARKER,
                "Authorized dashboard did not open",
                30
        );
    }

    @Step("Open Today screen")
    public void openToday() {
        this.waitForElementAndClick(TAB_TODAY, "Cannot find and tap Today tab", 10);
        this.waitForElementPresent(TAB_TODAY, "Today tab is not displayed", 10);
        this.closeConnectEmailPromptIfPresent();
    }

    @Step("Open Explore screen")
    public void openExplore() {
        this.waitForElementAndClick(TAB_EXPLORE, "Cannot find and tap Explore tab", 10);
        this.waitForElementPresent(TAB_EXPLORE, "Explore tab is not displayed", 10);
    }

    @Step("Open Shop screen")
    public void openShop() {
        this.waitForElementAndClick(TAB_SHOP, "Cannot find and tap Shop tab", 10);
        this.waitForElementPresent(TAB_SHOP, "Shop tab is not displayed", 10);
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
        try {
            this.waitForElementAndClick(PROFILE_BUTTON, "Cannot find and tap profile button", 10);
        } catch (WebDriverException e) {
            this.tapProfileButtonByScreenPosition();
        }
        try {
            this.waitForElementPresent(PROFILE_SCREEN, "Profile screen did not open", 10);
        } catch (TimeoutException e) {
            this.tapProfileButtonByScreenPosition();
            this.waitForElementPresent(PROFILE_SCREEN, "Profile screen did not open", 10);
        }
    }

    @Step("Verify Profile screen content")
    public void assertProfileScreenIsDisplayed() {
        this.waitForElementPresent(PROFILE_SCREEN, "Profile navigation bar is not displayed", 10);
        this.waitForElementPresent(PROFILE_PROGRESS_EXERCISES, "Exercise Completed profile metric is not displayed", 10);
        this.waitForElementPresent(PROFILE_PROGRESS_LESSONS, "Lessons Finished profile metric is not displayed", 10);
        this.waitForElementPresent(PROFILE_PROGRESS_STREAK, "Longest Streak Days profile metric is not displayed", 10);
        this.waitForElementPresent(PROFILE_PROGRAM_SETTINGS_TITLE, "Program settings block is not displayed", 10);
        this.waitForElementPresent(PROFILE_PERSONALIZATION_BUTTON, "Personalization item is not displayed", 10);
        this.waitForElementPresent(PROFILE_ACCOUNT_SETTINGS_BUTTON, "Account Settings item is not displayed", 10);
        this.waitForElementPresent(PROFILE_BACKED_BY_SCIENCE_BUTTON, "Backed By Science item is not displayed", 10);
        this.waitForElementPresent(PROFILE_SUPPORT_BUTTON, "Support item is not displayed", 10);
        this.waitForElementPresent(PROFILE_MY_WORKBOOK_BUTTON, "My workbook item is not displayed", 10);
        this.waitForElementPresent(PROFILE_FAQ_BUTTON, "FAQ item is not displayed", 10);
        this.swipeUpToFindElement(PROFILE_TERMS_BUTTON, "Cannot find Terms & privacy policy item on Profile screen", 2);
        this.waitForElementPresent(PROFILE_TERMS_BUTTON, "Terms & privacy policy item is not displayed", 10);
        this.swipeUpToFindElement(DELETE_ACCOUNT_BUTTON, "Cannot find Delete my account item on Profile screen", 2);
        this.waitForElementPresent(DELETE_ACCOUNT_BUTTON, "Delete my data/account item is not displayed", 10);
        if (this.isElementPresent(PROFILE_PREMIUM_BADGE)) {
            this.waitForElementPresent(PROFILE_PREMIUM_BADGE, "Profile premium badge is not displayed", 10);
        }
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

    @Step("Verify PDF guide upsell is displayed")
    public void assertPdfGuideUpsellIsDisplayed() {
        this.waitForElementPresent(PDF_GUIDE_UPSELL_TITLE, "PDF guide upsell title is not displayed", 10);
        this.waitForElementPresent(PDF_GUIDE_UPSELL_PRODUCT_TITLE, "PDF guide product title is not displayed", 10);
        this.waitForElementPresent(PDF_GUIDE_UPSELL_BUY_BUTTON, "PDF guide buy button is not displayed", 10);
        this.waitForElementPresent(PDF_GUIDE_UPSELL_CLOSE_BUTTON, "PDF guide close button is not displayed", 10);
    }

    @Step("Close PDF guide upsell")
    public void closePdfGuideUpsell() {
        this.waitForElementAndClick(PDF_GUIDE_UPSELL_CLOSE_BUTTON, "Cannot close PDF guide upsell", 10);
        this.waitForElementNotPresent(PDF_GUIDE_UPSELL_TITLE, "PDF guide upsell is still displayed", 10);
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
        this.waitForElementPresent(PROFILE_SCREEN, "Profile screen did not return after cancelling delete account", 10);
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
        this.hideKeyboardIfPossible();
    }

    @Step("Verify Continue button is disabled on login screen")
    public void assertLoginContinueButtonIsDisabled() {
        WebElement continueButton = this.waitForElementPresent(
                LOGIN_CONTINUE_BUTTON,
                "Continue button is not displayed on login screen",
                10
        );
        Assert.assertEquals("Login Continue button should be disabled", "false", continueButton.getAttribute("enabled"));
    }

    @Step("Verify Continue button is enabled on login screen")
    public void assertLoginContinueButtonIsEnabled() {
        WebElement continueButton = this.waitForElementPresent(
                LOGIN_CONTINUE_BUTTON,
                "Continue button is not displayed on login screen",
                10
        );
        Assert.assertEquals("Login Continue button should be enabled", "true", continueButton.getAttribute("enabled"));
    }

    @Step("Submit email in login flow")
    public void submitLoginEmail() {
        this.waitForElementAndClick(LOGIN_CONTINUE_BUTTON, "Cannot tap Continue on login screen", 10);
        this.waitForElementPresent(OTP_SCREEN_TITLE, "Security code screen did not open after submitting email", 20);
    }

    @Step("Verify OTP screen for email")
    public void assertOtpScreenIsDisplayedForEmail(String email) {
        this.waitForElementPresent(OTP_SCREEN_TITLE, "Security code screen is not displayed", 10);
        this.waitForElementPresent(
                OTP_EMAIL_SENT_TEXT.replace("{EMAIL}", email),
                "OTP email confirmation text is not displayed for " + email,
                10
        );
        this.waitForElementPresent(OTP_RESEND_CODE_BUTTON, "Resend code button is not displayed", 10);
    }

    @Step("Tap Resend code")
    public void resendSecurityCode() {
        this.waitForElementAndClick(OTP_RESEND_CODE_BUTTON, "Cannot tap Resend code", 10);
        this.waitForElementPresent(OTP_SCREEN_TITLE, "OTP screen disappeared after tapping Resend code", 10);
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
        this.closeVisibleAuthFlow();
        if (this.isElementPresent(CLOSE_LOGIN_BUTTON)) {
            this.closeVisibleAuthFlow();
        }
        this.waitForStartScreen();
    }

    @Step("Return from OTP flow to start screen")
    public void returnFromOtpFlowToStartScreen() {
        this.closeVisibleAuthFlow();
        if (this.isElementPresent(CLOSE_LOGIN_BUTTON)) {
            this.closeVisibleAuthFlow();
        }
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

    @Step("Verify Profile can be opened from Today and Explore")
    public void assertProfileCanBeOpenedFromMainTabs() {
        this.openToday();
        this.openProfile();
        this.returnToMainScreenFromProfile();

        this.openExplore();
        this.openProfile();
        this.returnToMainScreenFromProfile();
    }

    @Step("Return from Profile to main screen")
    public void returnToMainScreenFromProfile() {
        this.waitForElementAndClick(PROFILE_SUBSCREEN_BACK_BUTTON, "Cannot close Profile screen", 10);
        this.waitForElementNotPresent(PROFILE_SCREEN, "Profile screen is still displayed after closing", 10);
        this.waitForElementPresent(AUTHORIZED_DASHBOARD_MARKER, "Main screen did not open after closing Profile", 10);
    }

    @Step("Hide keyboard if it is visible")
    public void hideKeyboardIfPossible() {
        try {
            Map<String, Object> args = new HashMap<String, Object>();
            ((JavascriptExecutor) driver).executeScript("mobile: hideKeyboard", args);
        } catch (Exception e) {
            System.out.println("Keyboard was not hidden automatically; continuing.");
        }
    }

    @Step("Activate app if supported")
    public void activateAppIfPossible() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        try {
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("bundleId", getIOSBundleId());
            ((JavascriptExecutor) driver).executeScript("mobile: activateApp", args);
        } catch (Exception e) {
            System.out.println("App activation was not needed or failed: " + e.getMessage());
        }
    }

    @Step("Close notification prompt if it is displayed")
    public void closeNotificationPromptIfPresent() {
        try {
            this.waitForElementPresent(NOTIFICATION_PROMPT_TITLE, "Notification prompt is not displayed", 2);
            this.waitForElementAndClick(
                    NOTIFICATION_PROMPT_CLOSE_BUTTON,
                    "Cannot close notification prompt",
                    10
            );
            this.waitForElementNotPresent(
                    NOTIFICATION_PROMPT_TITLE,
                    "Notification prompt is still displayed",
                    10
            );
        } catch (Exception e) {
            System.out.println("Notification prompt was not shown; continuing.");
        }
    }

    @Step("Close PDF guide upsell if it is displayed")
    public void closePdfGuideUpsellIfPresent() {
        try {
            this.waitForElementPresent(PDF_GUIDE_UPSELL_TITLE, "PDF guide upsell is not displayed", 2);
            this.closePdfGuideUpsell();
        } catch (Exception e) {
            System.out.println("PDF guide upsell was not shown; continuing.");
        }
    }

    @Step("Close Connect Email prompt if it is displayed")
    public void closeConnectEmailPromptIfPresent() {
        try {
            this.waitForElementPresent(CONNECT_EMAIL_PROMPT_TITLE, "Connect Email prompt is not displayed", 2);
            this.waitForElementAndClick(
                    CONNECT_EMAIL_PROMPT_LATER_BUTTON,
                    "Cannot close Connect Email prompt",
                    10
            );
            this.waitForElementNotPresent(
                    CONNECT_EMAIL_PROMPT_TITLE,
                    "Connect Email prompt is still displayed",
                    10
            );
        } catch (Exception e) {
            System.out.println("Connect Email prompt was not shown; continuing.");
        }
    }

    @Step("Verify notification prompt is displayed")
    public void assertNotificationPromptIsDisplayed() {
        this.waitForElementPresent(
                NOTIFICATION_PROMPT_TITLE,
                "Notification prompt title is not displayed",
                10
        );
        this.waitForElementPresent(
                NOTIFICATION_PROMPT_CLOSE_BUTTON,
                "Notification prompt close button is not displayed",
                10
        );
    }

    public boolean isNotificationPromptDisplayed() {
        return this.isElementPresent(NOTIFICATION_PROMPT_TITLE);
    }

    private void closeVisibleAuthFlow() {
        this.waitForElementAndClick(CLOSE_LOGIN_BUTTON, "Cannot close login or OTP flow", 10);
    }

    private void tapProfileButtonByScreenPosition() {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x", driver.manage().window().getSize().getWidth() - 32);
        args.put("y", 80);
        ((JavascriptExecutor) driver).executeScript("mobile: tap", args);
    }

    private String getIOSBundleId() {
        String propertyBundleId = System.getProperty("ios.bundleId");
        if (propertyBundleId != null && !propertyBundleId.trim().isEmpty()) {
            return propertyBundleId;
        }

        String envBundleId = System.getenv("IOS_BUNDLE_ID");
        if (envBundleId != null && !envBundleId.trim().isEmpty()) {
            return envBundleId;
        }

        return "com.vamapps.The-Coach";
    }
}
