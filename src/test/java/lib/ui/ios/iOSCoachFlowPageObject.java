package lib.ui.ios;

import lib.ui.CoachFlowPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSCoachFlowPageObject extends CoachFlowPageObject {
    static {
        TAB_FEED = "id:Feed";
        FEED_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[@name='Feed']";
        FEED_DEPRECATION_POPUP_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Feed will be removed')]";
        FEED_DEPRECATION_POPUP_CONFIRM_BUTTON = "id:Got it";

        PROFILE_BUTTON = "id:WomanProfileImage";
        PROFILE_SCREEN = "xpath://XCUIElementTypeNavigationBar[@name='The_Coach.UserProfileView']";
        LOG_OUT_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Log Out']";
        LOG_OUT_CONFIRM_TITLE = "xpath://XCUIElementTypeStaticText[@name='Log out of your account?']";
        LOG_OUT_CONFIRM_BUTTON = "id:YES";

        START_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Feel the new level')]";
        START_BUTTON = "id:GET STARTED";
        LOGIN_BUTTON = "id:LOG IN";

        LOGIN_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Enter the mail that is linked to your account')]";
        LOGIN_EMAIL_INPUT = "xpath://XCUIElementTypeTextField";
        LOGIN_CONTINUE_BUTTON = "id:CONTINUE";
        OTP_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[@name='ENTER SECURITY CODE']";
        // The OTP control is custom; digits are entered via visible iOS keyboard keys.
        OTP_CODE_INPUT = "xpath://XCUIElementTypeTextField";
        POST_AUTH_ONBOARDING_MARKER = "xpath://XCUIElementTypeStaticText[@name='What do you want to achieve?']";
        CLOSE_LOGIN_BUTTON = "id:CloseRoundBlack";

        ONBOARDING_GOALS_TITLE = "xpath://XCUIElementTypeStaticText[@name='What do you want to achieve?']";
        ONBOARDING_BACK_BUTTON = "id:ic outline chevron left";

        AUTHORIZED_DASHBOARD_MARKER = "id:WomanProfileImage";
    }

    public iOSCoachFlowPageObject(RemoteWebDriver driver) {
        super(driver);
    }
}
