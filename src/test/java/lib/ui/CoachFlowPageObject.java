package lib.ui;

import io.appium.java_client.HidesKeyboard;
import io.qameta.allure.Step;
import lib.Platform;
import lib.ui.factories.DailyPlanPageObjectFactory;
import org.junit.Assert;
import org.openqa.selenium.Alert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

abstract public class CoachFlowPageObject extends MainPageObject {
    protected static final String
            TEST_ID_TODAY_TAB = "id:tab_today",
            TEST_ID_TODAY_SELECTED = "id:tab_today_selected",
            TEST_ID_EXPLORE_TAB = "id:tab_explore",
            TEST_ID_EXPLORE_SELECTED = "id:tab_explore_selected",
            TEST_ID_SHOP_TAB = "id:tab_shop",
            TEST_ID_SHOP_SELECTED = "id:tab_shop_selected",
            TEST_ID_PROFILE_SCREEN = "id:profile_screen",
            TEST_ID_PROFILE_ACCOUNT_SETTINGS = "id:profile_account_settings",
            TEST_ID_PROFILE_SUPPORT = "id:profile_support",
            TEST_ID_PROFILE_WORKBOOK = "id:profile_my_workbook",
            TEST_ID_PROFILE_FAQ = "id:profile_faq",
            TEST_ID_PROFILE_TERMS = "id:profile_terms",
            TEST_ID_PROFILE_LOGOUT = "id:profile_logout",
            TEST_ID_PROFILE_LOGOUT_CONFIRM = "id:profile_logout_confirm",
            TEST_ID_PROFILE_BROWSER_CLOSE = "id:profile_browser_close",
            TEST_ID_START_SCREEN = "id:start_screen",
            TEST_ID_START_BUTTON = "id:start_button",
            TEST_ID_LOGIN_BUTTON = "id:login_button",
            TEST_ID_LOGIN_SCREEN = "id:login_screen",
            TEST_ID_LOGIN_EMAIL = "id:login_email",
            TEST_ID_LOGIN_CONTINUE = "id:login_continue",
            TEST_ID_OTP_SCREEN = "id:otp_screen",
            TEST_ID_OTP_RESEND = "id:otp_resend",
            TEST_ID_EXISTING_ACCOUNT_LOGIN = "id:auth_existing_account_login",
            TEST_ID_SEND_SECURITY_CODE = "id:auth_send_security_code",
            TEST_ID_LOGIN_VALIDATION_ERROR = "id:login_email_error",
            TEST_ID_NOTIFICATION_CLOSE = "id:push_permission_close",
            TEST_ID_CONNECT_EMAIL_LATER = "id:connect_email_later",
            TEST_ID_LOADING = "id:loading_indicator";

    protected static String
            TAB_TODAY,
            TAB_TODAY_SELECTED,
            TAB_EXPLORE,
            TAB_EXPLORE_SELECTED,
            TAB_SHOP,
            TAB_SHOP_SELECTED,
            SHOP_CONTENT_MARKER,
            SHOP_CONTENT_FALLBACK,
            TAB_FEED,
            FEED_SCREEN_TITLE,
            FEED_DEPRECATION_POPUP_TITLE,
            FEED_DEPRECATION_POPUP_CONFIRM_BUTTON,
            PROFILE_BUTTON,
            PROFILE_BUTTON_FEMALE,
            PROFILE_BUTTON_LEGACY,
            PROFILE_SCREEN,
            ANONYMOUS_PROFILE_MARKER,
            ANONYMOUS_PROFILE_CONNECT_BUTTON,
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
            EXISTING_ACCOUNT_LOGIN_BUTTON,
            SEND_SECURITY_CODE_BUTTON,
            LOGIN_VALIDATION_ERROR,
            OTP_CONFIRMATION_TITLE,
            OTP_CONFIRMATION_CLOSE_BUTTON,
            POST_AUTH_ONBOARDING_MARKER,
            CLOSE_LOGIN_BUTTON,
            NOTIFICATION_PROMPT_TITLE,
            NOTIFICATION_PROMPT_ALLOW_BUTTON,
            NOTIFICATION_PROMPT_CLOSE_BUTTON,
            SYSTEM_NOTIFICATION_PERMISSION_ALLOW_BUTTON,
            SYSTEM_NOTIFICATION_PERMISSION_ALLOW_BUTTON_FALLBACK,
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
        this.closeKegelExerciseFlowIfPresent();

        if (this.isElementPresent(TEST_ID_START_SCREEN) || this.isElementPresent(START_SCREEN_TITLE)) {
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

        if (this.isElementPresent(TEST_ID_PROFILE_SCREEN) || this.isElementPresent(PROFILE_SCREEN)) {
            this.logOut();
            return;
        }

        if (this.isElementPresent(AUTHORIZED_DASHBOARD_MARKER)
                || this.isElementPresent(TEST_ID_TODAY_TAB)
                || this.isElementPresent(TAB_TODAY)) {
            this.openToday();
            this.openProfile();
            this.logOut();
            return;
        }

        this.waitForStartScreen();
    }

    // Do not annotate methods that receive credentials: Allure records method
    // arguments automatically, which would put the email/OTP into the report.
    public void ensureExistingProgressUserIsLoggedIn(String email, String otpCode) {
        this.ensureLoggedOutOnStartScreen();
        this.loginWithEmailAndOtp(email, otpCode);
    }

    public void loginWithEmailAndOtp(String email, String otpCode) {
        this.openLoginFlow();
        this.typeLoginEmail(email);
        this.assertLoginContinueButtonIsEnabled();
        this.submitLoginEmail();
        this.assertOtpScreenIsDisplayedForEmail(email);
        this.typeSecurityCode(otpCode);
        this.completeAuthorizationAfterSecurityCode();
    }

    @Step("Complete authorization after entering the security code")
    public void completeAuthorizationAfterSecurityCode() {
        this.waitForAuthorizedDashboard();
        this.waitForLoadingToDisappearIfPresent(TEST_ID_LOADING, "Authorization loading indicator is still displayed", 30);
        this.closePdfGuideUpsellIfPresent();
        this.closeNotificationPromptIfPresent();
        this.openToday();
    }

    @Step("Verify that the authorized dashboard is displayed")
    public void assertAuthorizedDashboardIsDisplayed() {
        this.waitForFirstElementPresent(
                new String[]{AUTHORIZED_DASHBOARD_MARKER, TEST_ID_TODAY_TAB, TAB_TODAY},
                "Expected authorized The Coach dashboard before starting Feed/logout flow",
                15
        );
    }

    @Step("Wait for authorized dashboard")
    public void waitForAuthorizedDashboard() {
        this.waitForFirstElementPresent(
                new String[]{AUTHORIZED_DASHBOARD_MARKER, TEST_ID_TODAY_TAB, TAB_TODAY},
                "Authorized dashboard did not open",
                30
        );
    }

    @Step("Open Today screen")
    public void openToday() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_TODAY_TAB, TAB_TODAY},
                "Cannot find and tap Today tab",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_TODAY_SELECTED, TAB_TODAY_SELECTED},
                "Today tab is not selected",
                10
        );
        this.waitForLoadingToDisappearIfPresent(TEST_ID_LOADING, "Today loading indicator is still displayed", 20);
        this.closeConnectEmailPromptIfPresent();
    }

    @Step("Open Explore screen")
    public void openExplore() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_EXPLORE_TAB, TAB_EXPLORE},
                "Cannot find and tap Explore tab",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_EXPLORE_SELECTED, TAB_EXPLORE_SELECTED},
                "Explore tab is not selected",
                10
        );
        this.waitForLoadingToDisappearIfPresent(TEST_ID_LOADING, "Explore loading indicator is still displayed", 20);
    }

    @Step("Open Shop screen")
    public void openShop() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_SHOP_TAB, TAB_SHOP},
                "Cannot find and tap Shop tab",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_SHOP_SELECTED, TAB_SHOP_SELECTED},
                "Shop tab is not selected",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{SHOP_CONTENT_MARKER, SHOP_CONTENT_FALLBACK},
                "Shop content did not become visible after opening Shop",
                10
        );
        this.waitForLoadingToDisappearIfPresent(TEST_ID_LOADING, "Shop loading indicator is still displayed", 20);
    }

    @Step("Verify the current three-tab navigation")
    public void assertMainTabsAreDisplayed() {
        this.waitForFirstElementPresent(new String[]{TEST_ID_TODAY_TAB, TAB_TODAY}, "Today tab is not displayed", 10);
        this.waitForFirstElementPresent(new String[]{TEST_ID_EXPLORE_TAB, TAB_EXPLORE}, "Explore tab is not displayed", 10);
        this.waitForFirstElementPresent(new String[]{TEST_ID_SHOP_TAB, TAB_SHOP}, "Shop tab is not displayed", 10);
        if (TAB_FEED != null) {
            this.assertElementNotPresent(TAB_FEED, "Feed is retired and must not be displayed in the current three-tab UI");
        }
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
        this.waitForFirstElementAndClick(
                new String[]{PROFILE_BUTTON, PROFILE_BUTTON_FEMALE, PROFILE_BUTTON_LEGACY},
                "Cannot find and tap profile button by accessibility id",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_PROFILE_SCREEN, PROFILE_SCREEN},
                "Profile screen did not open",
                10
        );
        this.waitForLoadingToDisappearIfPresent(TEST_ID_LOADING, "Profile loading indicator is still displayed", 20);
    }

    @Step("Verify Profile screen content")
    public void assertProfileScreenIsDisplayed() {
        this.assertProfileSmokeContentIsDisplayed();
        this.swipeUpToFindFirstElement(
                new String[]{TEST_ID_PROFILE_TERMS, PROFILE_TERMS_BUTTON},
                "Cannot find Terms & privacy policy item on Profile screen",
                2
        );
        this.swipeUpToFindElement(DELETE_ACCOUNT_BUTTON, "Cannot find Delete my account item on Profile screen", 2);
        this.waitForElementPresent(DELETE_ACCOUNT_BUTTON, "Delete my data/account item is not displayed", 10);
        if (this.isElementPresent(PROFILE_PREMIUM_BADGE)) {
            this.waitForElementPresent(PROFILE_PREMIUM_BADGE, "Profile premium badge is not displayed", 10);
        }
    }

    @Step("Verify Profile Smoke content")
    public void assertProfileSmokeContentIsDisplayed() {
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_PROFILE_SCREEN, PROFILE_SCREEN},
                "Profile navigation bar is not displayed",
                10
        );
        this.waitForElementPresent(PROFILE_PROGRESS_EXERCISES, "Exercise Completed profile metric is not displayed", 10);
        this.waitForElementPresent(PROFILE_PROGRESS_LESSONS, "Lessons Finished profile metric is not displayed", 10);
        this.waitForElementPresent(PROFILE_PROGRESS_STREAK, "Longest Streak Days profile metric is not displayed", 10);
        this.waitForElementPresent(PROFILE_PROGRAM_SETTINGS_TITLE, "Program settings block is not displayed", 10);
        this.waitForElementPresent(PROFILE_PERSONALIZATION_BUTTON, "Personalization item is not displayed", 10);
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_PROFILE_ACCOUNT_SETTINGS, PROFILE_ACCOUNT_SETTINGS_BUTTON},
                "Account Settings item is not displayed",
                10
        );
        this.waitForElementPresent(PROFILE_BACKED_BY_SCIENCE_BUTTON, "Backed By Science item is not displayed", 10);
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_PROFILE_SUPPORT, PROFILE_SUPPORT_BUTTON},
                "Support item is not displayed",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_PROFILE_WORKBOOK, PROFILE_MY_WORKBOOK_BUTTON},
                "My workbook item is not displayed",
                10
        );
        this.swipeUpToFindFirstVisibleElement(
                new String[]{TEST_ID_PROFILE_FAQ, PROFILE_FAQ_BUTTON},
                "Cannot find FAQ below the visible Profile items",
                3
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_PROFILE_FAQ, PROFILE_FAQ_BUTTON},
                "FAQ item is not displayed",
                10
        );
    }

    @Step("Open Account Settings from Profile")
    public void openAccountSettingsFromProfile() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_PROFILE_ACCOUNT_SETTINGS, PROFILE_ACCOUNT_SETTINGS_BUTTON},
                "Cannot tap Account Settings",
                10
        );
        this.waitForElementPresent(PROFILE_ACCOUNT_SETTINGS_SCREEN, "Account Settings screen did not open", 15);
    }

    @Step("Open Support from Profile")
    public void openSupportFromProfile() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_PROFILE_SUPPORT, PROFILE_SUPPORT_BUTTON},
                "Cannot tap Support",
                10
        );
        this.waitForElementPresent(PROFILE_SUPPORT_SCREEN, "Support screen did not open", 15);
        this.waitForElementPresent(PROFILE_SUPPORT_FAQ_BUTTON, "Support FAQ entry is not visible", 10);
    }

    @Step("Open My workbook from Profile")
    public void openMyWorkbookFromProfile() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_PROFILE_WORKBOOK, PROFILE_MY_WORKBOOK_BUTTON},
                "Cannot tap My workbook",
                10
        );
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
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_PROFILE_FAQ, PROFILE_FAQ_BUTTON},
                "Cannot tap FAQ",
                10
        );
        this.waitForElementPresent(PROFILE_FAQ_SCREEN, "FAQ browser did not open", 20);
    }

    @Step("Open Terms and privacy policy from Profile")
    public void openTermsFromProfile() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_PROFILE_TERMS, PROFILE_TERMS_BUTTON},
                "Cannot tap Terms & privacy policy",
                10
        );
        this.waitForElementPresent(PROFILE_TERMS_SCREEN, "Terms browser did not open", 20);
    }

    @Step("Return to Profile screen from an internal Profile screen")
    public void returnToProfileFromSubscreen() {
        this.waitForElementAndClick(PROFILE_SUBSCREEN_BACK_BUTTON, "Cannot return to Profile screen", 10);
        this.assertProfileScreenIsDisplayed();
    }

    @Step("Close Profile browser screen")
    public void closeProfileBrowser() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_PROFILE_BROWSER_CLOSE, PROFILE_BROWSER_CLOSE_BUTTON},
                "Cannot close browser screen",
                10
        );
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
        this.swipeUpToFindFirstElement(
                new String[]{TEST_ID_PROFILE_LOGOUT, LOG_OUT_BUTTON},
                "Cannot find Log Out button on Profile screen",
                4
        );
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_PROFILE_LOGOUT, LOG_OUT_BUTTON},
                "Cannot tap Log Out button",
                10
        );
        this.confirmLogoutIfNeeded();
        this.waitForStartScreen();
    }

    @Step("Confirm logout if confirmation dialog appears")
    public void confirmLogoutIfNeeded() {
        try {
            this.waitForFirstElementPresent(
                    new String[]{TEST_ID_PROFILE_LOGOUT_CONFIRM, LOG_OUT_CONFIRM_TITLE},
                    "Logout confirmation dialog did not appear",
                    3
            );
            this.waitForFirstElementAndClick(
                    new String[]{TEST_ID_PROFILE_LOGOUT_CONFIRM, LOG_OUT_CONFIRM_BUTTON},
                    "Cannot confirm logout",
                    10
            );
        } catch (TimeoutException e) {
            if (this.isElementPresent(TEST_ID_PROFILE_LOGOUT_CONFIRM)
                    || this.isElementPresent(LOG_OUT_CONFIRM_BUTTON)) {
                this.waitForFirstElementAndClick(
                        new String[]{TEST_ID_PROFILE_LOGOUT_CONFIRM, LOG_OUT_CONFIRM_BUTTON},
                        "Cannot confirm logout",
                        10
                );
            } else {
                System.out.println("Logout confirmation dialog was not shown; continuing.");
            }
        }
    }

    @Step("Verify start screen is displayed")
    public void waitForStartScreen() {
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_START_SCREEN, START_SCREEN_TITLE},
                "Start screen title is not displayed",
                20
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_LOGIN_BUTTON, LOGIN_BUTTON},
                "Login button is not displayed on start screen",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_START_BUTTON, START_BUTTON},
                "Start button is not displayed on start screen",
                10
        );
    }

    @Step("Open login flow from start screen")
    public void openLoginFlow() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_LOGIN_BUTTON, LOGIN_BUTTON},
                "Cannot tap Login button on start screen",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_LOGIN_SCREEN, LOGIN_SCREEN_TITLE},
                "Login flow did not open after tapping Login",
                10
        );
        this.waitForLoadingToDisappearIfPresent(TEST_ID_LOADING, "Login loading indicator is still displayed", 20);
    }

    public void typeLoginEmail(String email) {
        WebElement emailInput = this.waitForFirstElementPresent(
                new String[]{TEST_ID_LOGIN_EMAIL, LOGIN_EMAIL_INPUT},
                "Cannot find email input on login screen",
                10
        );
        emailInput.sendKeys(email);
        this.hideKeyboardIfPossible();
    }

    @Step("Verify Continue button is disabled on login screen")
    public void assertLoginContinueButtonIsDisabled() {
        WebElement continueButton = this.waitForFirstElementPresent(
                new String[]{TEST_ID_LOGIN_CONTINUE, LOGIN_CONTINUE_BUTTON},
                "Continue button is not displayed on login screen",
                10
        );
        Assert.assertFalse("Login Continue button should be disabled", this.isElementEnabled(continueButton));
    }

    @Step("Verify disabled Login Continue is a no-op")
    public void assertDisabledLoginContinueIsNoOp() {
        this.tapDisabledAndAssertNoTransition(
                new String[]{TEST_ID_LOGIN_CONTINUE, LOGIN_CONTINUE_BUTTON},
                new String[]{TEST_ID_LOGIN_SCREEN, LOGIN_SCREEN_TITLE},
                new String[]{TEST_ID_OTP_SCREEN, OTP_SCREEN_TITLE},
                "Disabled Login Continue button",
                10
        );
    }

    @Step("Verify Continue button is enabled on login screen")
    public void assertLoginContinueButtonIsEnabled() {
        WebElement continueButton = this.waitForFirstElementPresent(
                new String[]{TEST_ID_LOGIN_CONTINUE, LOGIN_CONTINUE_BUTTON},
                "Continue button is not displayed on login screen",
                10
        );
        Assert.assertTrue("Login Continue button should be enabled", this.isElementEnabled(continueButton));
    }

    @Step("Submit email in login flow")
    public void submitLoginEmail() {
        this.clickOnceAndWaitForTransition(
                new String[]{TEST_ID_LOGIN_CONTINUE, LOGIN_CONTINUE_BUTTON},
                new String[]{TEST_ID_LOGIN_SCREEN, LOGIN_SCREEN_TITLE},
                new String[]{TEST_ID_OTP_SCREEN, OTP_SCREEN_TITLE},
                "Cannot submit email with a single tap",
                20
        );
        this.waitForLoadingToDisappearIfPresent(TEST_ID_LOADING, "OTP loading indicator is still displayed", 20);
    }

    /**
     * Follow the CONNECT-mail flow from Xray, not the Welcome login form.
     * COA-7949 requires the existing-account prompt and the Send code step;
     * a direct transition to OTP cannot satisfy either required intermediate step.
     */
    @Step("Submit email and open existing-account login step")
    public void submitEmailAndOpenExistingAccountLoginStep() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_LOGIN_CONTINUE, LOGIN_CONTINUE_BUTTON},
                "Cannot submit email with a single tap",
                20
        );
        this.waitForFirstElementNotPresent(
                new String[]{TEST_ID_LOGIN_SCREEN, LOGIN_SCREEN_TITLE},
                "Mail form did not close after submitting a valid email",
                20
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_EXISTING_ACCOUNT_LOGIN, EXISTING_ACCOUNT_LOGIN_BUTTON},
                "Existing-account Login step did not open after Continue",
                20
        );
    }

    @Step("Open security-code request step for an existing account")
    public void openSecurityCodeRequestStep() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_EXISTING_ACCOUNT_LOGIN, EXISTING_ACCOUNT_LOGIN_BUTTON},
                "Cannot tap Login on the existing-account step",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_SEND_SECURITY_CODE, SEND_SECURITY_CODE_BUTTON, TEST_ID_OTP_SCREEN, OTP_SCREEN_TITLE},
                "Security-code request step did not open after tapping Login",
                20
        );
        Assert.assertTrue("Required Send code step is absent: LOGIN opened OTP directly; this does not satisfy COA-7949",
                this.isElementVisible(TEST_ID_SEND_SECURITY_CODE) || this.isElementVisible(SEND_SECURITY_CODE_BUTTON));
    }

    @Step("Request security code")
    public void requestSecurityCode() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_SEND_SECURITY_CODE, SEND_SECURITY_CODE_BUTTON},
                "Cannot tap Send code",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_OTP_SCREEN, OTP_SCREEN_TITLE},
                "OTP screen did not open after tapping Send code",
                20
        );
        this.waitForLoadingToDisappearIfPresent(TEST_ID_LOADING, "OTP loading indicator is still displayed", 20);
    }

    @Step("Verify invalid-email validation error is displayed")
    public void assertLoginValidationErrorIsDisplayed() {
        try {
            this.waitForFirstElementPresent(
                    new String[]{TEST_ID_LOGIN_VALIDATION_ERROR, LOGIN_VALIDATION_ERROR},
                    "Invalid-email validation error is not displayed", 10);
        } catch (TimeoutException missingValidation) {
            throw new AssertionError("Invalid email was entered but the required validation error is not displayed", missingValidation);
        }
    }

    public void assertOtpScreenIsDisplayedForEmail(String email) {
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_OTP_SCREEN, OTP_SCREEN_TITLE},
                "Security code screen is not displayed",
                10
        );
        this.waitForElementPresent(
                OTP_EMAIL_SENT_TEXT.replace("{EMAIL}", email),
                "OTP email confirmation text is not displayed for the requested account",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_OTP_RESEND, OTP_RESEND_CODE_BUTTON},
                "Resend code button is not displayed",
                10
        );
    }

    @Step("Tap Resend code")
    public void resendSecurityCode() {
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_OTP_RESEND, OTP_RESEND_CODE_BUTTON},
                "Cannot tap Resend code",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_OTP_SCREEN, OTP_SCREEN_TITLE},
                "OTP screen disappeared after tapping Resend code",
                10
        );
    }

    public void typeSecurityCode(String code) {
        this.waitForFirstElementPresent(
                new String[]{TEST_ID_OTP_SCREEN, OTP_SCREEN_TITLE},
                "Security code screen is not displayed",
                10
        );
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
        this.waitForFirstElementAndClick(
                new String[]{TEST_ID_START_BUTTON, START_BUTTON},
                "Cannot tap Start button on start screen",
                10
        );
        this.waitForElementPresent(
                ONBOARDING_GOALS_TITLE,
                "Onboarding goals screen did not open after tapping Start",
                10
        );
        this.waitForLoadingToDisappearIfPresent(TEST_ID_LOADING, "Onboarding loading indicator is still displayed", 20);
    }

    @Step("Complete onboarding using configured accessibility ids")
    public void completeOnboardingUsingConfiguredSteps(String[] stepLocators) {
        this.runConfiguredOnboardingSteps(stepLocators);

        this.waitForFirstElementPresent(
                new String[]{AUTHORIZED_DASHBOARD_MARKER, TEST_ID_TODAY_TAB, TAB_TODAY},
                "Configured onboarding steps did not lead to the authorized Daily Plan",
                30
        );
    }

    @Step("Run configured onboarding actions")
    public void runConfiguredOnboardingSteps(String[] stepLocators) {
        Assert.assertNotNull("Onboarding locator list must not be null", stepLocators);
        Assert.assertTrue(
                "Onboarding accessibility ids are not configured. Set -Dios.onboarding.steps=id:...,... for the selected Firebase onboarding variant.",
                stepLocators.length > 0
        );

        for (String stepLocator : stepLocators) {
            this.waitForElementAndClick(
                    stepLocator,
                    "Cannot advance onboarding using configured locator " + stepLocator,
                    20
            );
        }
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
        this.waitForFirstElementNotPresent(
                new String[]{TEST_ID_PROFILE_SCREEN, PROFILE_SCREEN},
                "Profile screen is still displayed after closing",
                10
        );
        this.waitForElementPresent(AUTHORIZED_DASHBOARD_MARKER, "Main screen did not open after closing Profile", 10);
    }

    @Step("Hide keyboard if it is visible")
    public void hideKeyboardIfPossible() {
        try {
            if (driver instanceof HidesKeyboard) {
                ((HidesKeyboard) driver).hideKeyboard();
            } else {
                Map<String, Object> args = new HashMap<String, Object>();
                ((JavascriptExecutor) driver).executeScript("mobile: hideKeyboard", args);
            }
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
            this.waitForFirstElementAndClick(
                    new String[]{TEST_ID_NOTIFICATION_CLOSE, NOTIFICATION_PROMPT_CLOSE_BUTTON},
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

    @Step("Normalize Coach session for the next test")
    public void cleanupForNextTest() {
        this.activateAppIfPossible();
        this.closePdfGuideUpsellIfPresent();
        this.closeNotificationPromptIfPresent();
        this.closeConnectEmailPromptIfPresent();
        this.closeKegelExerciseFlowIfPresent();

        if (this.isElementPresent(CLOSE_LOGIN_BUTTON)) {
            this.closeVisibleAuthFlow();
            return;
        }

        if (this.isElementPresent(TEST_ID_PROFILE_SCREEN) || this.isElementPresent(PROFILE_SCREEN)) {
            this.logOut();
            return;
        }

        if (this.isElementPresent(AUTHORIZED_DASHBOARD_MARKER)
                || this.isElementPresent(TEST_ID_TODAY_TAB)
                || this.isElementPresent(TAB_TODAY)) {
            this.openToday();
            this.openProfile();
            this.logOut();
        }
    }

    private void closeKegelExerciseFlowIfPresent() {
        try {
            DailyPlanPageObjectFactory.get(driver).closeKegelExerciseFlowIfPresent();
        } catch (Exception e) {
            System.out.println("Kegel flow was not open during Coach session normalization; continuing.");
        }
    }

    @Step("Allow push notifications in the in-app permission screen")
    public void allowNotificationPrompt() {
        this.assertNotificationPromptIsDisplayed();
        this.waitForElementAndClick(
                NOTIFICATION_PROMPT_ALLOW_BUTTON,
                "Cannot allow push notifications from the in-app permission screen; app-side allow test id is required",
                10
        );
        this.waitForElementNotPresent(
                NOTIFICATION_PROMPT_TITLE,
                "In-app push permission screen is still displayed after allowing notifications",
                10
        );
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
            this.waitForFirstElementAndClick(
                    new String[]{TEST_ID_CONNECT_EMAIL_LATER, CONNECT_EMAIL_PROMPT_LATER_BUTTON},
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
        this.waitForFirstElementPresent(
                new String[]{NOTIFICATION_PROMPT_ALLOW_BUTTON, NOTIFICATION_PROMPT_CLOSE_BUTTON},
                "Notification prompt action is not displayed",
                10
        );
    }

    public boolean isNotificationPromptDisplayed() {
        return this.isElementPresent(NOTIFICATION_PROMPT_TITLE);
    }

    @Step("Allow the iOS system push permission dialog")
    public String allowSystemNotificationPermission() {
        try {
            WebDriverWait alertWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            Alert alert = alertWait.until(ExpectedConditions.alertIsPresent());
            String alertText = alert.getText();
            alert.accept();
            return alertText;
        } catch (TimeoutException webDriverAlertNotExposed) {
            WebElement systemAlertButton = this.waitForFirstElementPresent(
                    new String[]{SYSTEM_NOTIFICATION_PERMISSION_ALLOW_BUTTON, SYSTEM_NOTIFICATION_PERMISSION_ALLOW_BUTTON_FALLBACK},
                    "iOS system notification permission dialog did not appear",
                    15
            );
            String alertText = systemAlertButton.getAttribute("label");
            if (alertText == null || alertText.trim().isEmpty()) {
                alertText = systemAlertButton.getAttribute("name");
            }
            systemAlertButton.click();
            return alertText;
        }
    }

    private void closeVisibleAuthFlow() {
        this.waitForElementAndClick(CLOSE_LOGIN_BUTTON, "Cannot close login or OTP flow", 10);
    }

    private String getIOSBundleId() {
        return Platform.getInstance().getIOSBundleId();
    }
}
