package lib.ui.ios;

import lib.ui.CoachFlowPageObject;
import lib.ui.KegelCasePageObject;
import lib.Platform;
import lib.PostAuthQuestionnaireException;
import java.io.IOException;
import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.Alert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class iOSCoachFlowPageObject extends CoachFlowPageObject {
    private boolean preparingExistingProgressAccount;
    private static final String EXPLORE_WELCOME = "id:Welcome to Explore ✨";
    private static final String EXPLORE_WELCOME_CONFIRM = "id:SOUNDS GREAT";
    private static final String PROGRAM_DISCOVERY_HINT = "id:Tap to explore more programs";
    private static final String ANONYMOUS_ACCOUNT_NETWORK_ERROR =
            "xpath://XCUIElementTypeStaticText[@visible='true' and contains(@name,'Cannot create anonymous user')"
                    + " and contains(@name,'Network error')]";
    static final String MASTERCLASS_PROMO_TITLE = "id:Master the Art of Sensual Touch";
    public static final String WELCOME_START_BUTTON =
            "xpath://XCUIElementTypeButton[@name='START NOW' or @name='Get Started']";
    public static final String WELCOME_LOGIN_BUTTON =
            "xpath://XCUIElementTypeButton[@name=\"I'VE ALREADY PURCHASED\" or @name='Log in']";

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
        PROFILE_SUPPORT_SCREEN = "id:SupportTeamView";
        PROFILE_SUPPORT_FAQ_BUTTON = "xpath://XCUIElementTypeStaticText[@name='FAQ']";
        PROFILE_MY_WORKBOOK_BUTTON = "xpath://XCUIElementTypeStaticText[@name='My workbook']";
        PROFILE_MY_WORKBOOK_SCREEN = "id:ADD TO MY PROGRAM";
        PROFILE_MY_WORKBOOK_CLOSE_BUTTON = "id:PDFGuideUpsellCloseImageFemale";
        PROFILE_FAQ_BUTTON = "xpath://XCUIElementTypeStaticText[@name='FAQ']";
        PROFILE_FAQ_SCREEN = "xpath://XCUIElementTypeWebView//XCUIElementTypeOther[@name='FAQ']"
                + "//XCUIElementTypeButton[@name='How do I use The Coach app?']";
        PROFILE_TERMS_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Terms & privacy policy' or @name='Terms and Privacy Policy']";
        PROFILE_TERMS_SCREEN = "xpath://XCUIElementTypeStaticText[@name='The Coach — Terms of Service']";
        PROFILE_BROWSER_CLOSE_BUTTON = "xpath://XCUIElementTypeOther[@name='TopBrowserBar']"
                + "//XCUIElementTypeButton[@name='Close' or @name='Закрыть']"
                + " | //XCUIElementTypeButton[@name='Close' or @name='Done' or @name='Закрыть'"
                + " or @name='profile_browser_close']";
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
        START_BUTTON = WELCOME_START_BUTTON;
        LOGIN_BUTTON = WELCOME_LOGIN_BUTTON;

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
        LOGIN_ACCOUNT_VARIANT_ERROR = "xpath://XCUIElementTypeStaticText[contains(@name, 'regular Coach app') "
                + "or contains(@label, 'regular Coach app')]";
        POST_AUTH_ONBOARDING_MARKER = "xpath://XCUIElementTypeStaticText[@name='What do you want to achieve?']";
        CLOSE_LOGIN_BUTTON = "id:CloseRoundBlack";
        NOTIFICATION_PROMPT_TITLE = "xpath://XCUIElementTypeStaticText[@name='Allow notifications to stay on track' "
                + "or @name='Let The Coach support you']";
        NOTIFICATION_PROMPT_ALLOW_BUTTON = "xpath://XCUIElementTypeButton[@name='push_permission_allow' "
                + "or @name='REMIND ME TO PRACTICE']";
        NOTIFICATION_PROMPT_CLOSE_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Allow notifications to stay on track']/../XCUIElementTypeButton[1] "
                + "| //XCUIElementTypeButton[@name='MAYBE LATER']";
        SYSTEM_NOTIFICATION_PERMISSION_ALLOW_BUTTON = "id:Allow";
        SYSTEM_NOTIFICATION_PERMISSION_ALLOW_BUTTON_FALLBACK = "xpath://XCUIElementTypeAlert//XCUIElementTypeButton[@name='Allow']";
        CONNECT_EMAIL_PROMPT_TITLE = "xpath://XCUIElementTypeStaticText[@name='Connect you email to save the progress.' or @name='Connect your email to save the progress.']";
        CONNECT_EMAIL_PROMPT_LATER_BUTTON = "id:LATER";
        PDF_GUIDE_UPSELL_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'ADD THE WORKBOOK')]";
        PDF_GUIDE_UPSELL_PRODUCT_TITLE = "id:E-workbook";
        PDF_GUIDE_UPSELL_BUY_BUTTON = "id:ADD TO MY PROGRAM";
        PDF_GUIDE_UPSELL_CLOSE_BUTTON = "xpath://XCUIElementTypeButton[@name='PDFGuideUpsellCloseImage' or @name='PDFGuideUpsellCloseImageFemale']";

        ONBOARDING_GOALS_TITLE = "xpath://XCUIElementTypeStaticText[@name='What do you want to achieve?' "
                + "or @name='WHAT DO YOU WANT TO ACHIEVE?']";
        ONBOARDING_BACK_BUTTON = "id:ic outline chevron left";

        AUTHORIZED_DASHBOARD_MARKER = "xpath://XCUIElementTypeButton[(@name='UserProfileImage' or @name='WomanProfileImage') and @visible='true'] | //XCUIElementTypeButton[@name='Today' and @visible='true']";
    }

    public iOSCoachFlowPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Open the ready iOS login form from Welcome")
    public void openLoginFlow() {
        tapHittableControl(new String[]{TEST_ID_LOGIN_BUTTON, LOGIN_BUTTON}, "Welcome Login is not hittable", 10);
        createWait(10).withMessage("Login tap did not open the email form")
                .until(ignored -> isLoginFormReady());
    }

    protected boolean isLoginFormReady() {
        return isElementVisible(LOGIN_SCREEN_TITLE) && isElementVisible(LOGIN_EMAIL_INPUT);
    }

    protected void tapHittableControl(String[] locators, String message, long timeout) {
        WebElement control = createWait(timeout).withMessage(message).until(ignored -> {
            for (String locator : locators) {
                for (WebElement element : driver.findElements(getLocatorByString(locator))) {
                    try {
                        if (element.isDisplayed() && isElementEnabled(element)
                                && "true".equalsIgnoreCase(element.getAttribute("hittable"))) return element;
                    } catch (StaleElementReferenceException rerendered) {
                        // Reacquire the control during the existing explicit wait.
                    }
                }
            }
            return null;
        });
        tapNativeCenter(control);
    }

    private void tapNativeCenter(WebElement control) {
        Rectangle bounds = control.getRect();
        Map<String, Object> tap = new HashMap<>();
        tap.put("x", bounds.x + bounds.width / 2);
        tap.put("y", bounds.y + bounds.height / 2);
        ((JavascriptExecutor) driver).executeScript("mobile: tap", tap);
    }

    @Override
    public void typeLoginEmail(String email) {
        WebElement input = waitForFirstElementEnabled(
                new String[]{TEST_ID_LOGIN_EMAIL, LOGIN_EMAIL_INPUT},
                "Cannot find an enabled iOS login email field", 10);
        input.click();
        input.clear();
        input.sendKeys(email);
        createWait(10).withMessage("iOS login email field did not retain the entered value")
                .until(ignored -> email.equals(input.getAttribute("value")));
        hideKeyboardIfPossible();
    }

    // No @Step: the argument is a credential and must not enter Allure parameters.
    public void assertAuthenticatedProfileForEmail(String email) {
        String nickname = email.substring(0, email.indexOf('@'));
        waitForElementVisible("xpath://XCUIElementTypeStaticText[@name="
                        + org.openqa.selenium.support.ui.Quotes.escape(nickname) + "]",
                "The configured account identity did not restore in Profile", 30);
        Assert.assertFalse("Profile is still anonymous after OTP; restore the account fixture",
                isElementVisible("id:CONFIRM") || isElementVisible("id:CONNECT"));
    }

    @Override
    public void logOut() {
        // mobile: swipe can target the tabs instead of this native collection.
        // Keep the drag inside Profile, above the persistent tab bar.
        for (int attempt = 0; attempt < 4 && !isElementVisible(LOG_OUT_BUTTON)
                && !isElementVisible(TEST_ID_PROFILE_LOGOUT); attempt++) {
            Rectangle header = waitForElementVisible(PROFILE_SCREEN, "Profile is not open before logout", 10).getRect();
            Rectangle tab = waitForElementVisible(TAB_TODAY, "Profile tab bar is missing", 10).getRect();
            int top = header.y + header.height;
            int bottom = tab.y;
            Assert.assertTrue("Profile has no scrollable viewport above the tab bar", bottom > top);
            Map<String, Object> drag = new HashMap<>();
            drag.put("duration", .5);
            drag.put("fromX", driver.manage().window().getSize().width / 2);
            drag.put("toX", driver.manage().window().getSize().width / 2);
            drag.put("fromY", top + (int) ((bottom - top) * .85));
            drag.put("toY", top + (int) ((bottom - top) * .15));
            ((JavascriptExecutor) driver).executeScript("mobile: dragFromToForDuration", drag);
        }
        super.logOut();
    }

    @Override
    @Step("Request native notification permission from the iOS in-app screen")
    public void allowNotificationPrompt() {
        assertNotificationPromptIsDisplayed();
        waitForElementAndClick(NOTIFICATION_PROMPT_ALLOW_BUTTON,
                "Cannot request notification permission from the in-app screen", 10);
        // iOS keeps the in-app screen underneath the native permission alert.
        // Handle that native alert before evaluating any subsequent app state.
    }

    @Override
    @Step("Verify and allow the native iOS Notifications alert")
    public String allowSystemNotificationPermission() {
        Alert alert = createWait(15).withMessage("Native iOS notification permission alert did not appear")
                .until(ExpectedConditions.alertIsPresent());
        String text = alert.getText();
        String normalized = text == null ? "" : text.toLowerCase(Locale.US);
        Assert.assertTrue("Native alert is not a Notifications permission request",
                normalized.contains("notification") || normalized.contains("уведомлен"));
        Map<String, Object> args = new HashMap<>();
        args.put("action", "getButtons");
        List<?> buttons = (List<?>) ((JavascriptExecutor) driver).executeScript("mobile: alert", args);
        String allow = buttons.contains("Allow") ? "Allow" : buttons.contains("Разрешить") ? "Разрешить" : null;
        Assert.assertNotNull("Native notification alert has no supported Allow button", allow);
        args.put("action", "accept");
        args.put("buttonLabel", allow);
        ((JavascriptExecutor) driver).executeScript("mobile: alert", args);
        createWait(10).withMessage("Native notification permission alert did not close")
                .until(ExpectedConditions.not(ExpectedConditions.alertIsPresent()));
        return text;
    }

    @Override
    public void ensureLoggedOutOnStartScreen() {
        activateAppIfPossible();
        closeProfileBrowserIfPresent();
        if (isElementVisible(LOG_OUT_CONFIRM_TITLE)) {
            confirmLogoutIfNeeded();
        }
        // A cold simulator launch can render the subscription error after the
        // first optional-popup check. Wait for an actionable entry surface.
        createWait(30).withMessage("No actionable screen after launching Coach").until(ignored -> {
            Assert.assertFalse("iOS startup is blocked by an anonymous-account network error; check network access",
                    isElementVisible(ANONYMOUS_ACCOUNT_NETWORK_ERROR));
            return isElementVisible(START_SCREEN_TITLE) || isElementVisible(TEST_ID_START_SCREEN)
                        || isElementVisible(TAB_TODAY) || isElementVisible(PROFILE_SCREEN)
                        || isElementVisible(CLOSE_LOGIN_BUTTON) || isElementVisible(ONBOARDING_BACK_BUTTON)
                        || isElementVisible(PDF_GUIDE_UPSELL_TITLE) || isElementVisible(NOTIFICATION_PROMPT_TITLE)
                        || isElementVisible(CONNECT_EMAIL_PROMPT_TITLE)
                        || new iOSOnboardingPageObject(driver).isPaywallVisible()
                        || isElementVisible(EXPLORE_WELCOME)
                        || isElementVisible(PROGRAM_DISCOVERY_HINT)
                        || isElementVisible("id:ProgramSelectionCardView")
                        || isElementVisible(MASTERCLASS_PROMO_TITLE)
                        || new iOSDailyPlanPageObject(driver).isDailyLessonVisible()
                        || new iOSDailyPlanPageObject(driver).isKegelExerciseFlowVisible()
                        || isElementVisible(PROFILE_FAQ_SCREEN) || isElementVisible(PROFILE_TERMS_SCREEN)
                        || isProfileBrowserVisible()
                        || isElementVisible("id:Get an access for your partner")
                        || isElementVisible("id:CoachProgramSettingsIcon")
                        || isElementVisible("id:navBarRoundBack");
        });
        closeProgramScreensIfPresent();
        super.ensureLoggedOutOnStartScreen();
    }

    @Override
    public void cleanupForNextTest() {
        closeProgramScreensIfPresent();
        if (Boolean.getBoolean("coach.ios.quizProfileLogin")) {
            quizProfileFlow().ensureLoggedOutOnObservedStart(this);
            return;
        }
        super.cleanupForNextTest();
    }

    @Override
    public void ensureExistingProgressUserIsLoggedIn(String email, String otpCode) {
        preparingExistingProgressAccount = true;
        try {
            if (Boolean.getBoolean("coach.ios.quizProfileLogin")) {
                KegelCasePageObject flow = quizProfileFlow();
                Assert.assertTrue("Quiz/Profile login requires an observed quiz-profile fixture",
                        flow.usesQuizProfileLogin());
                flow.loginExistingAccount(this, email, otpCode);
                return;
            }
            super.ensureExistingProgressUserIsLoggedIn(email, otpCode);
        } finally {
            preparingExistingProgressAccount = false;
        }
    }

    @Override
    public void waitForAuthorizedDashboard() {
        boolean prepareQuestionnaire = preparingExistingProgressAccount
                && Boolean.getBoolean("coach.ios.preparePostAuthQuestionnaire");
        createWait(30).withMessage("Authorization opened neither Today nor the goal questionnaire")
                .until(ignored -> {
                    closePostAuthPopupsIfPresent();
                    return isAuthorizedDashboardVisible() || isElementVisible(ONBOARDING_GOALS_TITLE);
                });
        if (isElementVisible(ONBOARDING_GOALS_TITLE) && !prepareQuestionnaire) {
            throw new PostAuthQuestionnaireException("Existing-progress fixture requires goal selection after OTP. "
                    + "Prepare the account and verify re-login restores Today before running regression; "
                    + "the questionnaire was not completed automatically by the test.");
        }
        if (!prepareQuestionnaire) {
            return;
        }

        if (isElementVisible(ONBOARDING_GOALS_TITLE)) {
            String goal = System.getProperty("coach.ios.postAuthGoal");
            Assert.assertTrue("Configure the approved new-account goal with -Dcoach.ios.postAuthGoal",
                    goal != null && !goal.trim().isEmpty());
            iOSOnboardingPageObject onboarding = new iOSOnboardingPageObject(driver);
            onboarding.selectGoal(goal);
            onboarding.completeQuestionnaire();
            onboarding.closePaywallsAndPopups();
            onboarding.waitForToday();
        }
        // The in-app permission screen can arrive after Today first renders.
        closePdfGuideUpsellIfPresent();
        closeNotificationPromptIfPresent();
        closeConnectEmailPromptIfPresent();
        createWait(30).withMessage("Authorized dashboard did not open").until(ignored -> {
            closePostAuthPopupsIfPresent();
            return isAuthorizedDashboardVisible();
        });
    }

    private void revealProfileItem(String preferred, String fallback) {
        if (!isElementVisible(preferred) && !isElementVisible(fallback)) {
            for (int swipe = 0; swipe < 4 && !isElementVisible(PROFILE_PROGRESS_EXERCISES); swipe++) {
                Map<String, Object> arguments = new HashMap<>();
                arguments.put("direction", "down");
                ((JavascriptExecutor) driver).executeScript("mobile: swipe", arguments);
            }
        }
        swipeUpToFindFirstVisibleElement(new String[]{preferred, fallback},
                "Profile action did not become visible", 4);
    }

    @Override
    public void openSupportFromProfile() {
        revealProfileItem(TEST_ID_PROFILE_SUPPORT, PROFILE_SUPPORT_BUTTON);
        super.openSupportFromProfile();
    }

    @Override
    public void openFaqFromProfile() {
        revealProfileItem(TEST_ID_PROFILE_FAQ, PROFILE_FAQ_BUTTON);
        super.openFaqFromProfile();
    }

    @Override
    public void openTermsFromProfile() {
        revealProfileItem(TEST_ID_PROFILE_TERMS, PROFILE_TERMS_BUTTON);
        try {
            super.openTermsFromProfile();
        } catch (TimeoutException failure) {
            if (isElementVisible("id:TopBrowserBar")) {
                String loading = "xpath://XCUIElementTypeOther"
                        + "[starts-with(@name, 'BrowserView?IsPageLoaded=false&')]";
                String state = isElementVisible(loading)
                        ? "the web document is still loading"
                        : "the expected Terms content is not visible";
                throw new TimeoutException("Terms browser opened, but " + state
                        + "; expected Terms content within 20 seconds. See the browser screenshot and page source.",
                        failure);
            }
            throw failure;
        }
    }

    protected boolean isAuthorizedDashboardVisible() {
        // Persistent tabs also exist beneath auth/navigation overlays. Require
        // rendered Today content and its Profile control before leaving OTP.
        return !isElementVisible(OTP_SCREEN_TITLE) && !isElementVisible(LOGIN_SCREEN_TITLE)
                && !isElementVisible(START_SCREEN_TITLE)
                && (isElementVisible(PROFILE_BUTTON) || isElementVisible(PROFILE_BUTTON_FEMALE)
                    || isElementVisible(PROFILE_BUTTON_LEGACY))
                && isElementVisible("id:DailyPlanItem");
    }

    private void closePostAuthPopupsIfPresent() {
        closeMasterclassPromoIfPresent();
        if (isElementVisible(NOTIFICATION_PROMPT_TITLE)) closeNotificationPromptIfPresent();
        if (isElementVisible(PDF_GUIDE_UPSELL_TITLE)) closePdfGuideUpsellIfPresent();
    }

    private KegelCasePageObject quizProfileFlow() {
        try {
            return new KegelCasePageObject(driver);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read the observed Quiz/Profile login fixture", failure);
        }
    }

    private void closeProgramScreensIfPresent() {
        closeProfileBrowserIfPresent();
        closeProgramDiscoveryHintIfPresent();
        closeProgramSelectorIfPresent();
        closeMasterclassPromoIfPresent();
        iOSDailyPlanPageObject daily = new iOSDailyPlanPageObject(driver);
        daily.closeDailyLessonIfPresent();
        daily.closeCompletedPracticeIfPresent();
        if (daily.isKegelExerciseFlowVisible()) daily.closeKegelExerciseFlowIfPresent();
        if (isElementVisible(EXPLORE_WELCOME)) dismissExploreWelcomeIfPresent();
        closePartnerPromoIfPresent();
        iOSOnboardingPageObject onboarding = new iOSOnboardingPageObject(driver);
        if (onboarding.isPaywallVisible()) {
            onboarding.closePaywallsAndPopups();
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
    @Step("Close the iOS Profile browser and verify return to Profile")
    public void closeProfileBrowser() {
        dismissProfileBrowser();
        assertProfileScreenIsDisplayed();
    }

    @Step("Dismiss an observed iOS browser overlay before session recovery")
    protected void closeProfileBrowserIfPresent() {
        if (!isProfileBrowserVisible()) return;
        String browser = isElementVisible("id:TopBrowserBar")
                ? "id:TopBrowserBar" : PROFILE_BROWSER_CLOSE_BUTTON;
        try {
            dismissProfileBrowser();
        } catch (TimeoutException closeFailure) {
            if (!isElementVisible(browser)) return;
            restartAfterBrowserDismissalFailure();
            waitForElementNotVisible(browser, "Browser survived the recovery restart", 10);
        }
    }

    private void dismissProfileBrowser() {
        String browser = isElementVisible("id:TopBrowserBar")
                ? "id:TopBrowserBar" : PROFILE_BROWSER_CLOSE_BUTTON;
        RemoteWebElement close = (RemoteWebElement) waitForElementEnabled(PROFILE_BROWSER_CLOSE_BUTTON,
                "Cannot dismiss the Profile browser overlay", 5);
        Rectangle bounds = close.getRect();
        Map<String, Object> tap = new HashMap<>();
        tap.put("elementId", close.getId());
        tap.put("x", bounds.width / 2.0);
        tap.put("y", bounds.height / 2.0);
        ((JavascriptExecutor) driver).executeScript("mobile: tap", tap);
        waitForElementNotVisible(browser, "Profile browser overlay did not close", 10);
    }

    private boolean isProfileBrowserVisible() {
        return isElementVisible("id:TopBrowserBar")
                || (isElementVisible(PROFILE_BROWSER_CLOSE_BUTTON)
                    && ((isElementVisible("id:URL") && isElementVisible("id:OpenInSafariButton"))
                        || isElementVisible(PROFILE_FAQ_SCREEN) || isElementVisible(PROFILE_TERMS_SCREEN)));
    }

    @Step("Recover from an unresponsive browser by restarting Coach without clearing its data")
    private void restartAfterBrowserDismissalFailure() {
        Map<String, Object> args = new HashMap<>();
        args.put("bundleId", Platform.getInstance().getIOSBundleId());
        ((JavascriptExecutor) driver).executeScript("mobile: terminateApp", args);
        ((JavascriptExecutor) driver).executeScript("mobile: activateApp", args);
    }

    @Override
    public void dismissExploreWelcomeIfPresent() {
        createWait(10).withMessage("Neither Explore selection nor its introduction appeared").until(ignored ->
                isElementVisible(EXPLORE_WELCOME) || isElementVisible(TAB_EXPLORE_SELECTED)
                        || isElementVisible(TEST_ID_EXPLORE_SELECTED));
        if (isElementVisible(EXPLORE_WELCOME)) {
            waitForElementAndClick(EXPLORE_WELCOME_CONFIRM, "Cannot acknowledge the Explore introduction", 10);
            waitForElementNotVisible(EXPLORE_WELCOME, "Explore introduction did not close", 10);
        }
    }

    @Override
    protected void assertProfileVariantContentIsDisplayed() {
        if (lib.Platform.getInstance().getIOSBundleId().endsWith("The-Coach-for-her")) {
            // For Her 2.0.33 has account actions in place of the male program/personalization block.
            waitForElementPresent(PROFILE_ACCOUNT_SETTINGS_BUTTON, "For Her Account Settings is absent", 10);
            waitForElementPresent(PROFILE_TERMS_BUTTON, "For Her Terms & privacy policy is absent", 10);
        } else {
            super.assertProfileVariantContentIsDisplayed();
        }
    }

    @Override
    public void openProfile() {
        closeProgramDiscoveryHintIfPresent();
        closeProgramSelectorIfPresent();
        closeMasterclassPromoIfPresent();
        closePartnerPromoIfPresent();
        tapHittableControl(new String[]{PROFILE_BUTTON, PROFILE_BUTTON_FEMALE, PROFILE_BUTTON_LEGACY},
                "Profile control is not hittable", 10);
        waitForElementVisible(PROFILE_SCREEN, "Profile screen did not open", 10);
        createWait(30).withMessage("Profile container opened but its content did not load")
                .until(ignored -> isProfileContentReady());
    }

    protected boolean isProfileContentReady() {
        return isElementVisible(PROFILE_SCREEN)
                && (isElementVisible(PROFILE_PREMIUM_BADGE) || isElementVisible(PROFILE_PROGRESS_LESSONS)
                    || isElementVisible(PROFILE_ACCOUNT_SETTINGS_BUTTON)
                    || isElementVisible("id:CONFIRM") || isElementVisible("id:CONNECT"));
    }

    @Override
    public void openToday() {
        closeProgramDiscoveryHintIfPresent();
        closeProgramSelectorIfPresent();
        closeMasterclassPromoIfPresent();
        closePartnerPromoIfPresent();
        super.openToday();
        closeProgramDiscoveryHintIfPresent();
        closeProgramSelectorIfPresent();
    }

    private void closeProgramDiscoveryHintIfPresent() {
        if (!isElementVisible(PROGRAM_DISCOVERY_HINT)) return;
        String confirm = "xpath://XCUIElementTypeStaticText[@name='Tap to explore more programs' and @visible='true']"
                + "/../XCUIElementTypeButton[@name='OK' and @visible='true']";
        waitForElementAndClick(confirm, "Cannot close the program discovery hint", 10);
        createWait(10).withMessage("Program discovery hint stayed visible")
                .until(ignored -> !isElementVisible(PROGRAM_DISCOVERY_HINT));
    }

    private void closeProgramSelectorIfPresent() {
        if (isElementVisible("id:ProgramSelectionCardView")) {
            new iOSProgramSelectorPageObject(driver).swipeSelectorDown();
        }
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

    @Step("Close optional Sensual Touch masterclass promo if displayed")
    public void closeMasterclassPromoIfPresent() {
        if (!isElementVisible(MASTERCLASS_PROMO_TITLE)) return;
        // This observed modal exposes an unnamed X beside its title and CTA.
        String close = "xpath://XCUIElementTypeStaticText[@name='Master the Art of Sensual Touch']"
                + "/../XCUIElementTypeButton[not(@name) and @visible='true']";
        waitForElementAndClick(close, "Cannot close the optional masterclass promo", 10);
        waitForElementNotVisible(MASTERCLASS_PROMO_TITLE, "Masterclass promo remains visible", 10);
    }
}
