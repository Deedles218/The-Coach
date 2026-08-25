package lib.ui.android;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import lib.Platform;
import lib.ui.CoachFlowPageObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.Map;

/**
 * Android navigation and authentication contract for The Coach 1.40.x.
 *
 * The OTP control is a custom Android view rather than an EditText. The
 * implementation therefore prefers Appium's mobile:type command and keeps a
 * native key-event fallback for emulators where the custom view does not
 * expose a writable value.
 */
public class AndroidCoachFlowPageObject extends CoachFlowPageObject {
    private static final String APP_PACKAGE = "com.vamapps.thecoach";

    static {
        TAB_TODAY = "id:" + APP_PACKAGE + ":id/nav_graph_daily";
        TAB_TODAY_SELECTED = "xpath://android.widget.FrameLayout[@resource-id='" + APP_PACKAGE + ":id/nav_graph_daily' and @selected='true']";
        TAB_EXPLORE = "id:" + APP_PACKAGE + ":id/nav_graph_explore";
        TAB_EXPLORE_SELECTED = "xpath://android.widget.FrameLayout[@resource-id='" + APP_PACKAGE + ":id/nav_graph_explore' and @selected='true']";
        TAB_SHOP = "id:" + APP_PACKAGE + ":id/nav_graph_shop";
        TAB_SHOP_SELECTED = "xpath://android.widget.FrameLayout[@resource-id='" + APP_PACKAGE + ":id/nav_graph_shop' and @selected='true']";
        SHOP_CONTENT_MARKER = "id:" + APP_PACKAGE + ":id/shop_screen";
        SHOP_CONTENT_FALLBACK = "id:" + APP_PACKAGE + ":id/nav_graph_shop";

        START_SCREEN_TITLE = "xpath://android.widget.TextView[@resource-id='" + APP_PACKAGE + ":id/tvTitle' and contains(@text,'FEEL THE NEW LEVEL')]";
        START_BUTTON = "id:" + APP_PACKAGE + ":id/btnSignInAnonymous";
        LOGIN_BUTTON = "id:" + APP_PACKAGE + ":id/btnLogin";
        LOGIN_SCREEN_TITLE = "xpath://android.widget.TextView[@resource-id='" + APP_PACKAGE + ":id/tvTitleText' and contains(@text,'ENTER THE EMAIL')]";
        LOGIN_EMAIL_INPUT = "id:" + APP_PACKAGE + ":id/edtEmail";
        LOGIN_CONTINUE_BUTTON = "id:" + APP_PACKAGE + ":id/btnContinue";
        OTP_SCREEN_TITLE = "xpath://android.widget.TextView[@resource-id='" + APP_PACKAGE + ":id/tvTitleText' and @text='ENTER SECURITY CODE']";
        OTP_EMAIL_SENT_TEXT = "id:" + APP_PACKAGE + ":id/tvSubTitleText";
        OTP_RESEND_CODE_BUTTON = "id:" + APP_PACKAGE + ":id/tvResend";
        OTP_CODE_INPUT = "id:" + APP_PACKAGE + ":id/smsVerifyView";
        POST_AUTH_ONBOARDING_MARKER = TAB_EXPLORE;
        CLOSE_LOGIN_BUTTON = "id:" + APP_PACKAGE + ":id/btnNavigateUp";
        AUTHORIZED_DASHBOARD_MARKER = TAB_EXPLORE;

        PROFILE_BUTTON = null;
        PROFILE_BUTTON_FEMALE = null;
        PROFILE_BUTTON_LEGACY = null;
        PROFILE_SCREEN = null;
        PROFILE_SUBSCREEN_BACK_BUTTON = CLOSE_LOGIN_BUTTON;
        ONBOARDING_GOALS_TITLE = "xpath://android.webkit.WebView[contains(@text,'What do you want to achieve')]";
        ONBOARDING_BACK_BUTTON = CLOSE_LOGIN_BUTTON;
    }

    public AndroidCoachFlowPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    public void ensureExistingProgressUserIsLoggedIn(String email, String otpCode) {
        if (isAuthorizedDashboardDisplayed()) {
            // Keep the current authenticated destination. Android Explore
            // tests may arrive here from Today or from a restored detail
            // surface and will select Explore explicitly afterwards.
            return;
        }

        ensureLoggedOutOnStartScreen();
        loginWithEmailAndOtp(email, otpCode);
    }

    @Override
    public void ensureLoggedOutOnStartScreen() {
        activateAppIfPossible();

        if (isAuthorizedDashboardDisplayed()) {
            return;
        }

        if (isElementPresent(START_SCREEN_TITLE) || isElementPresent(LOGIN_BUTTON)) {
            waitForStartScreen();
            return;
        }

        if (isElementPresent(CLOSE_LOGIN_BUTTON)) {
            waitForElementAndClick(CLOSE_LOGIN_BUTTON, "Cannot close Android login or OTP flow", 10);
            waitForStartScreen();
            return;
        }

        waitForStartScreen();
    }

    @Override
    public void loginWithEmailAndOtp(String email, String otpCode) {
        openLoginFlow();
        typeLoginEmail(email);
        assertLoginContinueButtonIsEnabled();
        submitLoginEmail();
        assertOtpScreenIsDisplayedForEmail(email);
        typeSecurityCode(otpCode);
        waitForAuthorizedDashboard();
        waitForLoadingToDisappearIfPresent(TEST_ID_LOADING, "Android authorization loading indicator is still displayed", 30);
        openToday();
    }

    @Override
    public void typeSecurityCode(String code) {
        if (isAuthorizedDashboardDisplayed()) {
            return;
        }

        waitForFirstElementPresent(
                new String[]{TEST_ID_OTP_SCREEN, OTP_SCREEN_TITLE},
                "Android security code screen is not displayed",
                15
        );

        WebElement codeView = null;
        try {
            codeView = waitForElementPresent(
                    OTP_CODE_INPUT,
                    "Android OTP control is not displayed",
                    8
            );
        } catch (TimeoutException ignored) {
            // Some 1.40.x builds move directly to the analysis screen after
            // the server accepts the code. The dashboard wait below handles
            // that legitimate asynchronous transition.
            if (isAuthorizedDashboardDisplayed()) {
                return;
            }
        }

        if (codeView != null) {
            try {
                codeView.click();
            } catch (WebDriverException ignored) {
                // The custom view may already own focus.
            }
        }

        if (!typeWithAppiumMobileCommand(code)) {
            if (codeView != null) {
                try {
                    codeView.sendKeys(code);
                } catch (WebDriverException ignored) {
                    // Fall through to Android key events.
                }
            }
            typeWithAndroidKeyEvents(code);
        }
    }

    @Override
    public void activateAppIfPossible() {
        try {
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).activateApp(Platform.getInstance().getAndroidAppPackage());
            }
        } catch (WebDriverException ignored) {
            // A newly created Appium session has already launched the app.
        }
    }

    private boolean isAuthorizedDashboardDisplayed() {
        return isElementPresent(TAB_TODAY) || isElementPresent(TAB_EXPLORE);
    }

    private boolean typeWithAppiumMobileCommand(String code) {
        try {
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("text", code);
            ((JavascriptExecutor) driver).executeScript("mobile: type", args);
            return true;
        } catch (WebDriverException ignored) {
            return false;
        }
    }

    private void typeWithAndroidKeyEvents(String code) {
        if (!(driver instanceof AndroidDriver)) {
            return;
        }

        AndroidDriver androidDriver = (AndroidDriver) driver;
        for (int index = 0; index < code.length(); index++) {
            AndroidKey key = keyForDigit(code.charAt(index));
            if (key != null) {
                androidDriver.pressKey(new KeyEvent(key));
            }
        }
    }

    private AndroidKey keyForDigit(char digit) {
        switch (digit) {
            case '0': return AndroidKey.DIGIT_0;
            case '1': return AndroidKey.DIGIT_1;
            case '2': return AndroidKey.DIGIT_2;
            case '3': return AndroidKey.DIGIT_3;
            case '4': return AndroidKey.DIGIT_4;
            case '5': return AndroidKey.DIGIT_5;
            case '6': return AndroidKey.DIGIT_6;
            case '7': return AndroidKey.DIGIT_7;
            case '8': return AndroidKey.DIGIT_8;
            case '9': return AndroidKey.DIGIT_9;
            default: return null;
        }
    }
}
