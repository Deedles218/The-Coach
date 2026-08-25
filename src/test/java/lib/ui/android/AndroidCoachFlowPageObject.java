package lib.ui.android;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.qameta.allure.Step;
import lib.Platform;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.factories.DailyPlanPageObjectFactory;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.junit.Assert;

import java.util.HashMap;
import java.util.List;
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
    private static final String ANDROID_POST_AUTH_QUESTIONNAIRE_MARKER =
            "id:" + APP_PACKAGE + ":id/tvAnswerNum";

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
        OTP_CONFIRMATION_TITLE = "id:" + APP_PACKAGE + ":id/tvConfirmText";
        OTP_CONFIRMATION_CLOSE_BUTTON = "id:" + APP_PACKAGE + ":id/btnClose";
        POST_AUTH_ONBOARDING_MARKER = TAB_EXPLORE;
        CLOSE_LOGIN_BUTTON = "id:" + APP_PACKAGE + ":id/btnNavigateUp";
        AUTHORIZED_DASHBOARD_MARKER = TAB_EXPLORE;

        PROFILE_BUTTON = "id:" + APP_PACKAGE + ":id/btnAction";
        PROFILE_BUTTON_FEMALE = null;
        PROFILE_BUTTON_LEGACY = null;
        PROFILE_SCREEN = "id:" + APP_PACKAGE + ":id/rvSettings";
        ANONYMOUS_PROFILE_MARKER =
                "xpath://android.widget.TextView[@resource-id='" + APP_PACKAGE + ":id/tvMainTitleStr' and contains(@text,'Connect your email')]";
        ANONYMOUS_PROFILE_CONNECT_BUTTON = "id:" + APP_PACKAGE + ":id/btnConnect";
        PROFILE_PROGRESS_EXERCISES = "id:" + APP_PACKAGE + ":id/tvExercisesText";
        PROFILE_PROGRESS_LESSONS = "id:" + APP_PACKAGE + ":id/tvLessonsText";
        PROFILE_PROGRESS_STREAK = "id:" + APP_PACKAGE + ":id/tvStreakText";
        PROFILE_PROGRAM_SETTINGS_TITLE =
                "xpath://android.widget.TextView[@text='Program settings']";
        PROFILE_PERSONALIZATION_BUTTON =
                "xpath://android.widget.TextView[@resource-id='" + APP_PACKAGE + ":id/tvTitle' and @text='Personalization']";
        PROFILE_ACCOUNT_SETTINGS_BUTTON =
                "xpath://android.widget.TextView[@resource-id='" + APP_PACKAGE + ":id/tvTitle' and @text='Account settings']";
        PROFILE_SUPPORT_BUTTON =
                "xpath://android.widget.TextView[@resource-id='" + APP_PACKAGE + ":id/tvTitle' and @text='Support']";
        PROFILE_MY_WORKBOOK_BUTTON =
                "xpath://android.widget.TextView[@resource-id='" + APP_PACKAGE + ":id/tvTitle' and @text='My workbook']";
        PROFILE_FAQ_BUTTON =
                "xpath://android.widget.TextView[@resource-id='" + APP_PACKAGE + ":id/tvTitle' and @text='FAQ']";
        PROFILE_TERMS_BUTTON =
                "xpath://android.widget.TextView[contains(@text,'Terms') and contains(@text,'privacy')]";
        PROFILE_BACKED_BY_SCIENCE_BUTTON = null;
        PROFILE_ACCOUNT_SETTINGS_SCREEN = null;
        PROFILE_SUPPORT_SCREEN = null;
        PROFILE_FAQ_SCREEN = null;
        PROFILE_MY_WORKBOOK_SCREEN = null;
        PROFILE_BROWSER_CLOSE_BUTTON = null;
        PROFILE_SUBSCREEN_BACK_BUTTON = CLOSE_LOGIN_BUTTON;
        LOG_OUT_BUTTON =
                "xpath://android.widget.TextView[@resource-id='" + APP_PACKAGE + ":id/tvText' and @text='Log Out']";
        LOG_OUT_CONFIRM_TITLE = "id:" + APP_PACKAGE + ":id/tvDialogtitle";
        LOG_OUT_CONFIRM_BUTTON = "id:" + APP_PACKAGE + ":id/btnYes";
        ONBOARDING_GOALS_TITLE =
                "xpath://android.widget.TextView[@resource-id='" + APP_PACKAGE + ":id/tvAcive' and contains(@text,'WHAT DO YOU WANT TO')]";
        ONBOARDING_BACK_BUTTON = "id:" + APP_PACKAGE + ":id/ivBackArrow";
    }

    public AndroidCoachFlowPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    public void ensureExistingProgressUserIsLoggedIn(String email, String otpCode) {
        closeAuthorizedTransientSurfaceIfPresent();
        if (isAuthorizedDashboardDisplayed()) {
            // Android also exposes the same three bottom tabs for an
            // anonymous/onboarding user. Verify the account state through
            // Profile before treating the tabs as an authenticated session.
            if (!isAnonymousSessionDisplayed()) {
                // Keep the current authenticated destination. Android
                // Explore tests may arrive here from Today or from a restored
                // detail surface and will select Explore explicitly
                // afterwards.
                return;
            }
        }

        ensureLoggedOutOnStartScreen();
        loginWithEmailAndOtp(email, otpCode);
    }

    @Override
    public void ensureLoggedOutOnStartScreen() {
        activateAppIfPossible();

        if (isElementPresent(PROFILE_SCREEN)) {
            if (isAnonymousProfileDisplayed()) {
                resetAnonymousAndroidAppToWelcome();
                return;
            }
            logOut();
            return;
        }

        closeAuthorizedTransientSurfaceIfPresent();

        if (isAuthorizedDashboardDisplayed()) {
            // The Profile affordance is rendered in the Today header. An
            // authenticated noReset session may be left on Explore by the
            // previous test, where the same accessibility id is absent.
            openToday();
            openProfile();
            logOut();
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
    @Step("Verify Android Profile smoke content")
    public void assertProfileScreenIsDisplayed() {
        assertProfileSmokeContentIsDisplayed();
    }

    @Override
    @Step("Open Android Profile from the top of Today")
    public void openProfile() {
        scrollTodayToTopIfPresent();
        super.openProfile();
    }

    @Override
    public void swipeUp(int timeOfSwipe) {
        try {
            Dimension size = driver.manage().window().getSize();
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("left", 0);
            args.put("top", 0);
            args.put("width", size.getWidth());
            args.put("height", size.getHeight());
            args.put("direction", "up");
            args.put("percent", 0.8);
            ((JavascriptExecutor) driver).executeScript("mobile: swipeGesture", args);
        } catch (WebDriverException swipeGestureUnavailable) {
            // Appium 2 UiAutomator2 exposes swipeGesture, while older local
            // drivers may only expose an element-scoped scroll gesture.
            scrollSettingsIfPresent();
        }
    }

    @Override
    @Step("Verify Android Profile metrics and settings")
    public void assertProfileSmokeContentIsDisplayed() {
        waitForElementPresent(PROFILE_SCREEN, "Android Profile screen is not displayed", 10);
        waitForElementPresent(PROFILE_PROGRESS_EXERCISES, "Completed exercises metric is not displayed", 10);
        waitForElementPresent(PROFILE_PROGRESS_LESSONS, "Completed lessons metric is not displayed", 10);
        waitForElementPresent(PROFILE_PROGRESS_STREAK, "Streak metric is not displayed", 10);
        waitForElementPresent(PROFILE_PROGRAM_SETTINGS_TITLE, "Program settings block is not displayed", 10);
        waitForElementPresent(PROFILE_PERSONALIZATION_BUTTON, "Personalization item is not displayed", 10);
        scrollSettingsToBottom();
        waitForElementPresent(PROFILE_ACCOUNT_SETTINGS_BUTTON, "Account settings item is not displayed", 10);
        waitForElementPresent(PROFILE_SUPPORT_BUTTON, "Support item is not displayed", 10);
    }

    @Override
    @Step("Log out from Android Profile")
    public void logOut() {
        scrollSettingsToBottom();
        waitForElementAndClick(LOG_OUT_BUTTON, "Cannot tap Android Log Out button", 10);
        confirmLogoutIfNeeded();
        waitForStartScreen();
    }

    @Override
    @Step("Return from Android Login to the auth entry screen")
    public void returnFromLoginFlowToStartScreen() {
        waitForElementAndClick(CLOSE_LOGIN_BUTTON, "Cannot close Android Login flow", 10);
        // On a fresh Android install the Login back action first reveals the
        // first onboarding question. The onboarding back action then returns
        // to the actual Welcome screen. Normalize both transitions so the
        // smoke assertion covers the intended auth entry point.
        waitForFirstElementPresent(
                new String[]{START_SCREEN_TITLE, ONBOARDING_GOALS_TITLE},
                "Android Login did not return to Welcome or the onboarding entry screen",
                15
        );
        if (isElementPresent(ONBOARDING_GOALS_TITLE)) {
            waitForElementAndClick(
                    ONBOARDING_BACK_BUTTON,
                    "Cannot return from the Android onboarding entry screen",
                    10
            );
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
        closeOtpConfirmationIfPresent();
        waitForFirstElementPresent(
                new String[]{TAB_TODAY, TAB_EXPLORE, ANDROID_POST_AUTH_QUESTIONNAIRE_MARKER},
                "Android authorization did not reach the dashboard or the post-auth questionnaire",
                30
        );
        if (isElementPresent(ANDROID_POST_AUTH_QUESTIONNAIRE_MARKER)) {
            Assert.fail(
                    "Android authorization is blocked by the post-auth questionnaire; "
                            + "complete the Android onboarding fixture before running authorized smoke checks"
            );
        }
        waitForAuthorizedDashboard();
        waitForLoadingToDisappearIfPresent(TEST_ID_LOADING, "Android authorization loading indicator is still displayed", 30);
        openToday();
    }

    @Override
    public void openLoginFlow() {
        if (isElementPresent(LOGIN_SCREEN_TITLE)) {
            return;
        }

        if (isAnonymousProfileDisplayed()) {
            waitForElementAndClick(
                    ANONYMOUS_PROFILE_CONNECT_BUTTON,
                    "Cannot open Android Login from the anonymous Profile",
                    10
            );
            waitForElementPresent(
                    LOGIN_SCREEN_TITLE,
                    "Android Login form did not open from the anonymous Profile",
                    10
            );
            return;
        }

        super.openLoginFlow();
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

    private void closeOtpConfirmationIfPresent() {
        try {
            waitForElementPresent(
                    OTP_CONFIRMATION_TITLE,
                    "Android email confirmation screen is not displayed",
                    15
            );
            waitForElementAndClick(
                    OTP_CONFIRMATION_CLOSE_BUTTON,
                    "Cannot close Android email confirmation screen",
                    10
            );
            waitForElementNotPresent(
                    OTP_CONFIRMATION_TITLE,
                    "Android email confirmation screen is still displayed",
                    20
            );
        } catch (TimeoutException ignored) {
            // Builds that transition directly to the dashboard do not show
            // the confirmation sheet.
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

    private boolean isAnonymousSessionDisplayed() {
        if (isAnonymousProfileDisplayed()) {
            return true;
        }

        if (!isAuthorizedDashboardDisplayed()) {
            return false;
        }

        // The anonymous Today/Explore shell has the same bottom navigation as
        // the authorized shell. Open Profile once and inspect the explicit
        // Connect affordance; this avoids silently reporting a no-op login as
        // a green authorization check.
        try {
            openToday();
            openProfile();
            boolean anonymous = isAnonymousProfileDisplayed();
            if (isElementPresent(PROFILE_SCREEN)) {
                waitForElementAndClick(
                        CLOSE_LOGIN_BUTTON,
                        "Cannot close Android Profile after checking auth state",
                        10
                );
                waitForFirstElementPresent(
                        new String[]{TAB_TODAY, TAB_EXPLORE},
                        "Android main tabs did not return after checking auth state",
                        10
                );
            }
            return anonymous;
        } catch (WebDriverException ignored) {
            return false;
        }
    }

    private boolean isAnonymousProfileDisplayed() {
        return isElementPresent(ANONYMOUS_PROFILE_CONNECT_BUTTON)
                || isElementPresent(ANONYMOUS_PROFILE_MARKER);
    }

    private void resetAnonymousAndroidAppToWelcome() {
        if (!(driver instanceof io.appium.java_client.InteractsWithApps)) {
            Assert.fail("Android anonymous Profile has no Logout action and the driver cannot reinstall the test app");
        }

        String appPath = Platform.getInstance().getAndroidAppPath();
        Assert.assertTrue(
                "Android anonymous Profile has no Logout action; provide -Dandroid.app=... for clean auth isolation",
                appPath != null && !appPath.trim().isEmpty()
        );

        io.appium.java_client.InteractsWithApps apps =
                (io.appium.java_client.InteractsWithApps) driver;
        String packageName = Platform.getInstance().getAndroidAppPackage();
        if (apps.isAppInstalled(packageName) && !apps.removeApp(packageName)) {
            Assert.fail("Could not remove the Android app while normalizing anonymous auth state");
        }
        apps.installApp(appPath);
        activateAppIfPossible();
        waitForStartScreen();
    }

    private void scrollTodayToTopIfPresent() {
        List<WebElement> scrollViews = driver.findElements(By.id(APP_PACKAGE + ":id/rvQuestions"));
        for (WebElement scrollView : scrollViews) {
            try {
                Map<String, Object> args = new HashMap<String, Object>();
                args.put("elementId", ((RemoteWebElement) scrollView).getId());
                args.put("direction", "up");
                args.put("percent", 1.0);
                ((JavascriptExecutor) driver).executeScript("mobile: scrollGesture", args);
                return;
            } catch (WebDriverException ignored) {
                // The screen may be re-rendered while the list is located.
            }
        }
    }

    private void scrollSettingsIfPresent() {
        List<WebElement> scrollViews = driver.findElements(By.id(APP_PACKAGE + ":id/rvSettings"));
        for (WebElement scrollView : scrollViews) {
            try {
                Map<String, Object> args = new HashMap<String, Object>();
                args.put("elementId", ((RemoteWebElement) scrollView).getId());
                args.put("direction", "up");
                args.put("percent", 0.8);
                ((JavascriptExecutor) driver).executeScript("mobile: scrollGesture", args);
                return;
            } catch (WebDriverException ignored) {
                // The settings list can be re-rendered during a swipe.
            }
        }
    }

    private void scrollSettingsToBottom() {
        for (int attempt = 0; attempt < 10; attempt++) {
            List<WebElement> scrollViews = driver.findElements(By.id(APP_PACKAGE + ":id/rvSettings"));
            for (WebElement scrollView : scrollViews) {
                try {
                    Map<String, Object> args = new HashMap<String, Object>();
                    args.put("elementId", ((RemoteWebElement) scrollView).getId());
                    args.put("direction", "down");
                    args.put("percent", 0.95);
                    ((JavascriptExecutor) driver).executeScript("mobile: scrollGesture", args);
                    break;
                } catch (WebDriverException ignored) {
                    // Retry after the settings list has been re-rendered.
                }
            }
            if (isElementPresent(LOG_OUT_BUTTON)) {
                return;
            }
        }
    }

    /**
     * noReset Android sessions can start on a native detail screen or on the
     * Kegel start screen left by the previous test. Close only those surfaces;
     * the Today header's btnAction is the Profile button and must never be
     * treated as an Explore detail close action.
     */
    private void closeAuthorizedTransientSurfaceIfPresent() {
        if (isElementPresent(PROFILE_SCREEN)
                || isElementPresent(START_SCREEN_TITLE)
                || isElementPresent(LOGIN_SCREEN_TITLE)
                || isElementPresent(OTP_SCREEN_TITLE)) {
            return;
        }

        try {
            DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
            dailyPlan.closeKegelExerciseFlowIfPresent();
        } catch (Exception ignored) {
            // The current screen is not a Kegel surface.
        }

        if (!isAuthorizedDashboardDisplayed()
                && isElementPresent("xpath://android.webkit.WebView")) {
            try {
                waitForElementAndClick(
                        "id:" + APP_PACKAGE + ":id/btnClose",
                        "Cannot close the previous Android WebView",
                        5
                );
            } catch (Exception ignored) {
                // The WebView may already have been dismissed by navigation.
            }
        }

        if (!isAuthorizedDashboardDisplayed()
                && isElementPresent(CLOSE_LOGIN_BUTTON)) {
            try {
                waitForElementAndClick(
                        CLOSE_LOGIN_BUTTON,
                        "Cannot close the previous Android detail screen",
                        5
                );
            } catch (Exception ignored) {
                // The surface may have disappeared during a re-render.
            }
        }
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
