package lib.ui.ios;

import lib.ui.CoachFlowPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSCoachFlowPageObject extends CoachFlowPageObject {
    static {
        TAB_TODAY = "id:Today";
        TAB_TODAY_SELECTED = "xpath://XCUIElementTypeButton[@name='Today' and @value='1']";
        TAB_EXPLORE = "id:Explore";
        TAB_EXPLORE_SELECTED = "xpath://XCUIElementTypeButton[@name='Explore' and @value='1']";
        TAB_SHOP = "id:Shop";
        TAB_SHOP_SELECTED = "xpath://XCUIElementTypeButton[@name='Shop' and @value='1']";
        SHOP_CONTENT_MARKER = "id:shop_screen";
        // The Store link is actual rendered content in the observed native
        // accessibility tree. Neither alternative accepts the tab itself.
        SHOP_CONTENT_FALLBACK = "xpath://XCUIElementTypeLink[@name='The Coach Store' and @visible='true']";
        TAB_FEED = "id:Feed";
        FEED_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[@name='Feed']";
        FEED_DEPRECATION_POPUP_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Feed will be removed')]";
        FEED_DEPRECATION_POPUP_CONFIRM_BUTTON = "id:Got it";

        // Preferred app-side contract: profile_button. Existing accessibility
        // names remain temporary fallbacks until the app ships that id.
        PROFILE_BUTTON = "id:profile_button";
        PROFILE_BUTTON_FEMALE = "id:WomanProfileImage";
        PROFILE_BUTTON_LEGACY = "id:UserProfileImage";
        PROFILE_SCREEN = "xpath://XCUIElementTypeNavigationBar[contains(@name, 'UserProfileView')] | //XCUIElementTypeStaticText[@name='Account Settings']";
        PROFILE_PREMIUM_BADGE = "id:PREMIUM SUBSCRIBER";
        PROFILE_PROGRESS_EXERCISES = "xpath://XCUIElementTypeStaticText[contains(@name, 'Exercise') and contains(@name, 'completed')]";
        PROFILE_PROGRESS_LESSONS = "xpath://XCUIElementTypeStaticText[contains(@name, 'Lessons') and contains(@name, 'finished')]";
        PROFILE_PROGRESS_STREAK = "xpath://XCUIElementTypeStaticText[contains(@name, 'Longest') and contains(@name, 'streak')]";
        PROFILE_PROGRESS_CHARTS = "xpath://XCUIElementTypeStaticText[contains(@name, 'Progress Charts')]";
        PROFILE_PROGRAM_SETTINGS_TITLE = "xpath://XCUIElementTypeStaticText[@name='Program settings' or @name='Program Settings']";
        PROFILE_PERSONALIZATION_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Personalization']";
        PROFILE_MEAL_PLAN_BUTTON = "xpath://XCUIElementTypeStaticText[contains(@name, 'Meal Plan')]";
        PROFILE_WORKOUT_PLAN_BUTTON = "xpath://XCUIElementTypeStaticText[contains(@name, 'Workout Plan')]";
        PROFILE_SUPPLEMENTS_BUTTON = "xpath://XCUIElementTypeStaticText[contains(@name, 'My Supplements')]";
        PROFILE_ACCOUNT_SETTINGS_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Account Settings']";
        PROFILE_ACCOUNT_SETTINGS_SCREEN = "xpath://XCUIElementTypeStaticText[contains(@name, 'Please enter your email')]";
        PROFILE_BACKED_BY_SCIENCE_BUTTON = "xpath://XCUIElementTypeStaticText[contains(@name, 'Backed by Science') or contains(@name, 'Backed By Science')]";
        PROFILE_SUPPORT_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Support']";
        PROFILE_SUPPORT_SCREEN = "xpath://XCUIElementTypeStaticText[@name='Hello!']";
        PROFILE_SUPPORT_FAQ_BUTTON = "xpath://XCUIElementTypeStaticText[@name='FAQ']";
        PROFILE_MY_WORKBOOK_BUTTON = "xpath://XCUIElementTypeStaticText[@name='My workbook']";
        PROFILE_MY_WORKBOOK_SCREEN = "id:ADD TO MY PROGRAM";
        PROFILE_MY_WORKBOOK_CLOSE_BUTTON = "id:PDFGuideUpsellCloseImageFemale";
        PROFILE_FAQ_BUTTON = "xpath://XCUIElementTypeStaticText[@name='FAQ']";
        PROFILE_FAQ_SCREEN = "id:How to use the app?";
        PROFILE_TERMS_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Terms & privacy policy' or @name='Terms and Privacy Policy']";
        PROFILE_TERMS_SCREEN = "xpath://XCUIElementTypeStaticText[@name='The Coach — Terms of Service']";
        PROFILE_BROWSER_CLOSE_BUTTON = "id:Close";
        PROFILE_SUBSCREEN_BACK_BUTTON = "xpath://XCUIElementTypeButton[@name='BackButton' or @name='Daily Plan' or @name='Today' or @name='Back']";
        DELETE_ACCOUNT_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Delete my account' or @name='Delete My Data' or @name='Delete my data']";
        DELETE_ACCOUNT_CONFIRM_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'It will be impossible to restore the progress')]";
        DELETE_ACCOUNT_CANCEL_BUTTON = "id:CANCEL";
        LOG_OUT_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Log Out']";
        LOG_OUT_CONFIRM_TITLE = "xpath://XCUIElementTypeStaticText[@name='Log out of your account?' "
                + "or starts-with(@name, 'Your account is currently anonymous because')]";
        LOG_OUT_CONFIRM_BUTTON = "xpath://*[@name='YES'] "
                + "| //XCUIElementTypeButton[@name='LOG OUT' and @visible='true']";

        START_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Feel the new level') or contains(@name, 'FEEL THE NEW LEVEL')]";
        START_BUTTON = "id:START NOW";
        LOGIN_BUTTON = "id:I'VE ALREADY PURCHASED";

        LOGIN_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Enter the mail that is linked to your account') or contains(@name, 'ENTER THE MAIL THAT IS LINKED')]";
        LOGIN_EMAIL_INPUT = "xpath://XCUIElementTypeTextField";
        LOGIN_CONTINUE_BUTTON = "id:CONTINUE";
        OTP_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[@name='ENTER SECURITY CODE']";
        OTP_EMAIL_SENT_TEXT = "xpath://XCUIElementTypeStaticText[contains(@name, '{EMAIL}')]";
        OTP_RESEND_CODE_BUTTON = "id:RESEND CODE";
        // The OTP control is custom; digits are entered via visible iOS keyboard keys.
        OTP_CODE_INPUT = "xpath://XCUIElementTypeTextField";
        // CONNECT + an existing email renders this identifier (not Welcome login).
        EXISTING_ACCOUNT_LOGIN_BUTTON = "id:LOGIN";
        SEND_SECURITY_CODE_BUTTON = "xpath://XCUIElementTypeButton[translate(@name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')='SEND CODE']";
        LOGIN_VALIDATION_ERROR = "xpath://XCUIElementTypeStaticText[contains(@name, 'valid email') or contains(@label, 'valid email') or contains(@name, 'email is not') or contains(@label, 'email is not')]";
        POST_AUTH_ONBOARDING_MARKER = "xpath://XCUIElementTypeStaticText[@name='What do you want to achieve?']";
        CLOSE_LOGIN_BUTTON = "id:CloseRoundBlack";
        NOTIFICATION_PROMPT_TITLE = "xpath://XCUIElementTypeStaticText[@name='Allow notifications to stay on track']";
        NOTIFICATION_PROMPT_ALLOW_BUTTON = "id:push_permission_allow";
        NOTIFICATION_PROMPT_CLOSE_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Allow notifications to stay on track']/../XCUIElementTypeButton[1]";
        SYSTEM_NOTIFICATION_PERMISSION_ALLOW_BUTTON = "id:Allow";
        SYSTEM_NOTIFICATION_PERMISSION_ALLOW_BUTTON_FALLBACK = "xpath://XCUIElementTypeAlert//XCUIElementTypeButton[@name='Allow']";
        CONNECT_EMAIL_PROMPT_TITLE = "xpath://XCUIElementTypeStaticText[@name='Connect you email to save the progress.' or @name='Connect your email to save the progress.']";
        CONNECT_EMAIL_PROMPT_LATER_BUTTON = "id:LATER";
        PDF_GUIDE_UPSELL_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'ADD THE WORKBOOK')]";
        PDF_GUIDE_UPSELL_PRODUCT_TITLE = "id:E-workbook";
        PDF_GUIDE_UPSELL_BUY_BUTTON = "id:ADD TO MY PROGRAM";
        PDF_GUIDE_UPSELL_CLOSE_BUTTON = "id:PDFGuideUpsellCloseImage";

        ONBOARDING_GOALS_TITLE = "xpath://XCUIElementTypeStaticText[@name='What do you want to achieve?']";
        ONBOARDING_BACK_BUTTON = "id:ic outline chevron left";

        AUTHORIZED_DASHBOARD_MARKER = "xpath://XCUIElementTypeButton[(@name='UserProfileImage' or @name='WomanProfileImage') and @visible='true'] | //XCUIElementTypeButton[@name='Today' and @visible='true']";
    }

    public iOSCoachFlowPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    public void ensureLoggedOutOnStartScreen() {
        activateAppIfPossible();
        if (isElementVisible(LOG_OUT_CONFIRM_TITLE)) {
            confirmLogoutIfNeeded();
        }
        // A cold simulator launch can render the subscription error after the
        // first optional-popup check. Wait for an actionable entry surface.
        createWait(30).withMessage("No actionable screen after launching Coach").until(ignored ->
                isElementVisible(START_SCREEN_TITLE) || isElementVisible(TEST_ID_START_SCREEN)
                        || isElementVisible(TAB_TODAY) || isElementVisible(PROFILE_SCREEN)
                        || isElementVisible(CLOSE_LOGIN_BUTTON) || isElementVisible(ONBOARDING_BACK_BUTTON)
                        || isElementVisible(PDF_GUIDE_UPSELL_TITLE) || isElementVisible(NOTIFICATION_PROMPT_TITLE)
                        || isElementVisible(CONNECT_EMAIL_PROMPT_TITLE)
                        || isElementVisible("id:subscription_error_illustration")
                        || isElementVisible("id:SpecialOfferClose")
                        || isElementVisible("id:Get an access for your partner")
                        || isElementVisible("id:CoachProgramSettingsIcon")
                        || isElementVisible("id:navBarRoundBack"));
        closeProgramScreensIfPresent();
        super.ensureLoggedOutOnStartScreen();
    }

    @Override
    public void cleanupForNextTest() {
        closeProgramScreensIfPresent();
        super.cleanupForNextTest();
    }

    private void closeProgramScreensIfPresent() {
        closePartnerPromoIfPresent();
        if (isElementVisible("id:subscription_error_illustration") || isElementVisible("id:SpecialOfferClose")) {
            createWait(25).withMessage("Cannot dismiss the optional subscription screens before cleanup").until(webDriver -> {
                closePartnerPromoIfPresent();
                if (isElementVisible("id:subscription_error_illustration")) {
                    waitForElementAndClick("id:ic_outline_close", "Cannot close unavailable subscription options", 5);
                    return false;
                }
                if (isElementVisible("id:SpecialOfferClose")) {
                    waitForElementAndClick("id:SpecialOfferClose", "Cannot close the optional special offer", 5);
                    return false;
                }
                if (isElementVisible(NOTIFICATION_PROMPT_TITLE)) {
                    waitForElementAndClick(NOTIFICATION_PROMPT_CLOSE_BUTTON, "Cannot close the optional notification prompt", 5);
                    return false;
                }
                return isElementVisible("id:Today") || isElementVisible(START_SCREEN_TITLE);
            });
        }
        String settings = "xpath://XCUIElementTypeNavigationBar[@name='Settings' and @visible='true']";
        if (isElementVisible("id:Restore")
                && (isElementVisible("id:SettingsCheckBoxInactive") || isElementVisible("id:SettingsCheckBoxActive"))) {
            // Current removed-exercises header has an unnamed close button.
            waitForElementAndClick("xpath://XCUIElementTypeButton[not(@name) and @visible='true' and number(@y)<100]",
                    "Cannot close Removed exercises before session cleanup", 5);
            waitForElementNotPresent("id:Restore", "Removed exercises did not close", 10);
        }
        if (isElementVisible(settings)) {
            waitForElementAndClick("id:navBarRoundBack", "Cannot leave program Settings", 5);
            waitForElementPresent("id:CoachProgramSettingsIcon", "Program detail did not reappear", 10);
        }
        if (isElementVisible("id:CoachProgramSettingsIcon")) {
            waitForElementAndClick("id:CloseRoundBlack", "Cannot leave program detail", 5);
            waitForElementPresent("id:Today", "Main tabs did not reappear", 10);
        }
    }

    @Override
    public void openProfile() {
        closePartnerPromoIfPresent();
        super.openProfile();
    }

    @Override
    public void openToday() {
        closePartnerPromoIfPresent();
        super.openToday();
    }

    public void closePartnerPromoIfPresent() {
        String title = "id:Get an access for your partner";
        if (!isElementVisible(title)) return;
        // Observed 2026-09-19: the only unnamed button in this modal is its X.
        // Scope to the promo container rather than clicking an arbitrary close.
        String close = "xpath://XCUIElementTypeStaticText[@name='Get an access for your partner']"
                + "/../XCUIElementTypeButton[not(@name) and @visible='true']";
        waitForElementAndClick(close, "Cannot close optional partner promo", 10);
        waitForElementNotVisible(title, "Partner promo remains visible", 10);
    }
}
