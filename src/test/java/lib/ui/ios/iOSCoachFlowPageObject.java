package lib.ui.ios;

import lib.ui.CoachFlowPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSCoachFlowPageObject extends CoachFlowPageObject {
    static {
        TAB_TODAY = "id:Today";
        TAB_EXPLORE = "id:Explore";
        TAB_SHOP = "id:Shop";
        TAB_FEED = "id:Feed";
        FEED_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[@name='Feed']";
        FEED_DEPRECATION_POPUP_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Feed will be removed')]";
        FEED_DEPRECATION_POPUP_CONFIRM_BUTTON = "id:Got it";

        PROFILE_BUTTON = "xpath://XCUIElementTypeButton[@name='UserProfileImage' or @name='WomanProfileImage']";
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
        LOG_OUT_CONFIRM_TITLE = "xpath://XCUIElementTypeStaticText[@name='Log out of your account?']";
        LOG_OUT_CONFIRM_BUTTON = "id:YES";

        START_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Feel the new level') or contains(@name, 'FEEL THE NEW LEVEL')]";
        START_BUTTON = "id:GET STARTED";
        LOGIN_BUTTON = "id:LOG IN";

        LOGIN_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Enter the mail that is linked to your account') or contains(@name, 'ENTER THE MAIL THAT IS LINKED')]";
        LOGIN_EMAIL_INPUT = "xpath://XCUIElementTypeTextField";
        LOGIN_CONTINUE_BUTTON = "id:CONTINUE";
        OTP_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[@name='ENTER SECURITY CODE']";
        OTP_EMAIL_SENT_TEXT = "xpath://XCUIElementTypeStaticText[contains(@name, '{EMAIL}')]";
        OTP_RESEND_CODE_BUTTON = "id:RESEND CODE";
        // The OTP control is custom; digits are entered via visible iOS keyboard keys.
        OTP_CODE_INPUT = "xpath://XCUIElementTypeTextField";
        POST_AUTH_ONBOARDING_MARKER = "xpath://XCUIElementTypeStaticText[@name='What do you want to achieve?']";
        CLOSE_LOGIN_BUTTON = "id:CloseRoundBlack";
        NOTIFICATION_PROMPT_TITLE = "xpath://XCUIElementTypeStaticText[@name='Allow notifications to stay on track']";
        NOTIFICATION_PROMPT_CLOSE_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Allow notifications to stay on track']/../XCUIElementTypeButton[1]";
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
}
