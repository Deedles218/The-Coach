package lib.ui.ios;

import io.appium.java_client.HasOnScreenKeyboard;
import lib.EmailEnforcementFixture.Stage;
import lib.ui.EmailReleaseScreen;
import org.junit.Assert;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Quotes;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

/** User-visible release flow, with iOS controls for the shared scenarios. */
public final class IOSEmailReleasePageObject extends iOSCoachFlowPageObject implements EmailReleaseScreen {
    private static final String EMAIL = "xpath://XCUIElementTypeTextField[@visible='true']";
    private static final String ENGLISH_TITLE = "xpath://XCUIElementTypeStaticText[@visible='true' and "
            + "translate(normalize-space(@name),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')="
            + "'ENTER YOUR EMAIL TO SYNC YOUR PROGRESS']";
    private static final String CLOSE = "id:CloseRoundBlack";
    private static final String LATER = "id:REMIND ME LATER";
    private static final String DAILY = "xpath://XCUIElementTypeOther[@name='DailyDaySwitcherView']"
            + "//XCUIElementTypeStaticText[@visible='true' and string-length(@name)>0]";
    private static final String WARNING = "id:Keep your progress safe";
    private static final String SALE_OFFER = "id:Early Black Friday Deal";
    private static final String SALE_CLOSE = "xpath://XCUIElementTypeStaticText[@name='Early Black Friday Deal'"
            + " and @visible='true']/../XCUIElementTypeButton[not(@name) and @visible='true']";
    private static final String ACCESS_WARNING =
            "id:Without an email, you may lose access to your account if you get signed out.";
    private static final String OTP = "xpath://XCUIElementTypeStaticText[@visible='true' and ("
            + "starts-with(@name,'An email with a code has been sent to:')"
            + " or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'correo')"
            + " and contains(@name,'@'))]";
    private static final String OTP_INPUTS = OTP + "/following-sibling::XCUIElementTypeOther[1]"
            + "//XCUIElementTypeOther[count(XCUIElementTypeStaticText)=4]";
    private static final String COUNTDOWN = "xpath://XCUIElementTypeButton[@visible='true'"
            + " and starts-with(translate(@name,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'RESEND')]";
    private static final String OTP_ERROR = "id:Email not found. Contact support below.";
    private static final String RESEND = "id:RESEND CODE";
    private static final String KEYBOARD = "xpath://XCUIElementTypeKeyboard[@visible='true']";
    private static final String CONFIRMATION_GRAPHIC = "xpath://XCUIElementTypeImage[@visible='true'"
            + " and not(@name='WhiteLogo' or @name='EnterEmailBackground')]";
    private static final Pattern REMAINING = Pattern.compile(".*\\b(?:[1-9]|[1-5][0-9]|60)\\s*s?$");
    private final boolean spanish;
    private final iOSOnboardingPageObject onboarding;

    public IOSEmailReleasePageObject(RemoteWebDriver driver, String language, iOSOnboardingPageObject onboarding) {
        super(driver);
        this.spanish = "es".equals(language);
        this.onboarding = onboarding;
    }

    private String title() {
        return spanish ? "xpath://XCUIElementTypeStaticText[@visible='true' and contains("
                + "translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sincroniz') and contains("
                + "translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'progreso')]"
                : ENGLISH_TITLE;
    }
    private String continueButton() { return spanish ? "id:CONTINUAR" : "id:CONTINUE"; }
    private String remindLater() {
        return spanish ? "xpath://XCUIElementTypeButton[@visible='true' and contains(translate(@name,"
                + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'más tarde')]" : LATER;
    }
    private String support() {
        return "xpath://XCUIElementTypeButton[@visible='true' and translate(@name,"
                + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='" + (spanish ? "soporte" : "support") + "']";
    }
    private String existingTitle() {
        return spanish ? "id:HEMOS ENCONTRADO UNA CUENTA EXISTENTE" : "id:WE’VE FOUND AN EXISTING ACCOUNT";
    }
    private String confirmed() { return spanish ? "id:¡Tu correo ha sido confirmado!" : "id:Your Email is confirmed!"; }

    public void startFromPurchaseLogin() {
        createWait(30).withMessage("iOS did not reach the Start screen or restore a profile")
                .until(ignored -> !closeSaleOfferIfPresent()
                        && (isElementVisible(WELCOME_START_BUTTON) || isDailyVisible()));
        if (isDailyVisible()) ensureLoggedOutOnStartScreen();
        openStartupLogin();
        waitForElementAndClick("id:PLEASE START HERE", "Please Start Here is unavailable", 15);
    }

    @Override public void openStartupLogin() {
        closeSaleOfferIfPresent();
        onboarding.assertFreshStartIsDisplayed();
        openLoginFlow();
    }

    private void waitForLaunchEntry() {
        waitForFirstElementPresent(new String[]{SALE_OFFER, title(), "id:RESTORE", TAB_TODAY,
                "id:subscription_paywall", "xpath://XCUIElementTypeWebView", "id:SpecialOfferClose"},
                "iOS launch did not reach a known destination", 30);
    }

    public int closeOffersBeforeEmailOrDaily() {
        int paywalls = 0;
        do {
            waitForLaunchEntry();
            paywalls += onboarding.closePaywallsAndPopupsUntil(() -> isElementVisible(title())
                    || isElementVisible(WARNING) || isElementVisible(SALE_OFFER));
        } while (closeSaleOfferIfPresent());
        return paywalls;
    }

    private boolean closeSaleOfferIfPresent() {
        if (!isElementVisible(SALE_OFFER)) return false;
        waitForElementAndClick(SALE_CLOSE, "Cannot close the Black Friday offer", 10);
        waitForElementNotVisible(SALE_OFFER, "The Black Friday offer stayed open", 10);
        return true;
    }

    @Override public void reachDailyWithoutDismissingRequest() { closeOffersBeforeEmailOrDaily(); }
    @Override public boolean isDailyVisible() { return !isElementVisible(title()) && isElementVisible(DAILY); }
    @Override public void waitForDaily() {
        waitForElementVisible(DAILY, "Daily content did not load", 30);
    }

    @Override public void assertStage(Stage stage) {
        waitForElementVisible(title(), "Daily email form is absent", 30);
        waitForElementVisible(EMAIL, "Daily email form has no input", 10);
        waitForElementVisible(continueButton(), "Daily email form has no Continue", 10);
        Assert.assertEquals("Unexpected cross availability at " + stage, stage.dismissible(), isElementVisible(CLOSE));
        Assert.assertEquals("Unexpected Remind me later availability at " + stage, stage.dismissible(),
                isElementVisible(remindLater()) || isElementVisible(LATER));
        if (stage.dismissible()) {
            waitForElementEnabled(CLOSE, "Cross is disabled", 8);
            waitForElementEnabled(remindLater(), "Remind me later is disabled", 8);
        } else assertContentBlocked();
        if (!spanish && (stage == Stage.E2 || stage == Stage.E3)) {
            waitForElementVisible(ACCESS_WARNING, "The access warning is absent", 8);
        }
        Assert.assertFalse("Legacy bypass is exposed", isElementVisible("id:CONTINUE WITHOUT EMAIL"));
    }

    @Override public void assertPreviouslyDisplayedFirstRequest() {
        waitForElementVisible(title(), "The previous build did not show the first request", 20);
        waitForElementEnabled(CLOSE, "The previous build did not show an optional request", 8);
        Assert.assertFalse("The previous build showed the second-request warning",
                isElementVisible(ACCESS_WARNING) || isElementVisible(WARNING));
    }

    @Override public void assertMandatoryCannotBeDismissed(Stage stage) {
        Assert.assertFalse("A mandatory form is required", stage.dismissible());
        assertStage(stage);
    }

    private void assertContentBlocked() {
        for (String tab : new String[]{TAB_TODAY, TAB_EXPLORE, TAB_SHOP}) {
            for (WebElement item : driver.findElements(getLocatorByString(tab))) {
                Assert.assertFalse("Content is accessible through the mandatory form",
                        item.isDisplayed() && "true".equalsIgnoreCase(item.getAttribute("hittable")));
            }
        }
    }

    @Override public void dismiss(Stage stage, String action) {
        Assert.assertTrue("Only optional requests can be dismissed", stage.dismissible());
        Assert.assertTrue("Expected cross or later", "cross".equals(action) || "later".equals(action));
        waitForElementAndClick("cross".equals(action) ? CLOSE : remindLater(), "Optional dismissal is unavailable", 10);
        if (stage == Stage.E2) {
            waitForFirstElementPresent(new String[]{WARNING, DAILY}, "E2 did not reach its warning or Daily", 15);
            if (isElementVisible(WARNING)) {
                waitForElementVisible("id:Next time you open the app, adding an email will be required. Add it now to avoid any interruption.",
                        "E2 warning does not explain the mandatory next request", 8);
                waitForElementEnabled("id:ADD EMAIL NOW", "E2 warning cannot open the form", 8);
                waitForElementAndClick("id:MAYBE NEXT TIME", "Cannot postpone from the E2 warning", 8);
                waitForElementNotVisible(WARNING, "E2 warning remained open", 8);
            }
        }
        waitForElementNotVisible(title(), "Optional request did not close", 10);
        waitForDaily();
        assertNoRequest();
    }

    @Override public void assertNoRequest() {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        createWait(5).until(ignored -> {
            Assert.assertFalse("Email request or OTP appeared over Daily",
                    isElementVisible(title()) || isElementVisible(OTP) || isElementVisible(WARNING));
            return System.nanoTime() >= deadline;
        });
    }

    @Override public void revisitDaily() {
        waitForElementAndClick(TAB_EXPLORE, "Explore is unavailable", 10);
        waitForElementVisible(TAB_EXPLORE_SELECTED, "Explore did not open", 20);
        waitForElementAndClick(TAB_TODAY, "Today is unavailable", 10);
        waitForDaily();
        assertNoRequest();
    }

    @Override public void enterEmail(String email) {
        typeLoginEmail(email);
        waitForElementEnabled(continueButton(), "Valid email did not enable Continue", 10);
    }

    @Override public void submit() {
        waitForElementAndClick(continueButton(), "Email Continue is unavailable", 10);
    }

    @Override public void assertSingleOtp(String email) {
        String send = spanish ? "id:ENVIAR CÓDIGO" : "id:SEND CODE";
        waitForFirstElementPresent(new String[]{OTP, OTP_ERROR, existingTitle(), send},
                "Email submission did not open OTP", 25);
        Assert.assertFalse("The free test address was rejected",
                isElementVisible(OTP_ERROR) || isElementVisible(existingTitle()));
        if (!isElementVisible(OTP) && isElementVisible(send)) {
            waitForElementAndClick(send, "Cannot request the verification code", 10);
        }
        String expected = email.toLowerCase(Locale.ROOT);
        createWait(20).withMessage("OTP does not identify the submitted address").until(ignored ->
                driver.findElements(getLocatorByString(OTP)).stream().anyMatch(element ->
                        element.isDisplayed() && element.getText().toLowerCase(Locale.ROOT).contains(expected)));
        Assert.assertEquals("Multiple OTP screens are visible", 1, driver.findElements(getLocatorByString(OTP)).size());
        Assert.assertFalse("Email entry remains over OTP", isElementVisible(EMAIL));
        waitForElementVisible(changeEmail(), "Change Email is unavailable", 10);
    }

    @Override public void waitForResendReady() {
        waitForElementEnabled(RESEND, "Resend Code did not become available", 75);
    }

    @Override public long resendLinkingCode(String email) {
        WebElement resend = waitForElementEnabled(RESEND, "Resend Code is unavailable", 5);
        long actionBoundary = System.currentTimeMillis();
        resend.click();
        createWait(12).withMessage("Resend Code did not restart the countdown").until(ignored ->
                driver.findElements(getLocatorByString(COUNTDOWN)).stream().anyMatch(element ->
                        element.isDisplayed() && REMAINING.matcher(element.getAttribute("label").trim()).matches()));
        assertSingleOtp(email);
        return actionBoundary;
    }

    @Override public void typeSecurityCode(String code) {
        for (int i = 0; i < code.length(); i++) pressDigit(code.charAt(i));
    }

    private void pressDigit(char digit) {
        waitForElementAndClick(KEYBOARD + "//XCUIElementTypeKey[@name=" + Quotes.escape(String.valueOf(digit)) + "]",
                "The numeric keyboard did not expose the requested test key", 10);
    }

    @Override public void enterOtpWithLayoutCheck(String code) {
        Assert.assertTrue("Expected a four-digit test code", code != null && code.matches("[0-9]{4}"));
        WebElement input = waitForElementVisible(OTP_INPUTS, "OTP boxes are not visible", 10);
        if (!keyboardShown()) input.click();
        pressDigit(code.charAt(0));
        Assert.assertTrue("The keyboard closed during code entry", keyboardShown());
        assertSupportBelow(OTP_INPUTS);
        typeSecurityCode(code.substring(1));
    }

    @Override public void assertConfirmationLayout() {
        waitForElementVisible(confirmed(), "Email confirmation did not succeed", 30);
        createWait(8).withMessage("The OTP keyboard remains visible").until(ignored -> !keyboardShown());
        assertSupportBelow(confirmed());
        String graphic = System.getProperty("coach.emailRelease.iosConfirmationIcon", CONFIRMATION_GRAPHIC);
        assertSupportBelow(graphic);
    }

    private void assertSupportBelow(String locator) {
        Rectangle result = waitForElementVisible(locator, "Verification result is not visible", 8).getRect();
        Rectangle footer = waitForElementVisible(support(), "Support is not visible", 8).getRect();
        Assert.assertTrue("Support overlaps the verification controls or result", footer.y >= result.y + result.height);
    }

    private boolean keyboardShown() { return ((HasOnScreenKeyboard) driver).isKeyboardShown(); }

    @Override public void assertEmptyEmail() {
        String value = waitForElementVisible(EMAIL, "Email field is absent", 10).getAttribute("value");
        Assert.assertTrue("Email field retained an address",
                value == null || value.isEmpty() || "example@mail.com".equals(value));
        assertElementDisabled(continueButton(), "Empty email enabled Continue", 8);
    }

    private String changeEmail() {
        return spanish ? "xpath://XCUIElementTypeButton[@visible='true' and starts-with(translate(@name,"
                + "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'CAMBIAR')]"
                : "id:CHANGE EMAIL";
    }

    @Override public void changeEmailAndCheckValidation(Stage stage) {
        waitForElementAndClick(changeEmail(), "Change Email is unavailable", 10);
        assertStage(stage);
        assertEmptyEmail();
        typeLoginEmail("not-an-email");
        assertElementDisabled(continueButton(), "Invalid email enabled Continue", 8);
        Assert.assertFalse("Invalid email advanced to OTP", isElementVisible(OTP));
        assertStage(stage);
    }

    @Override public void openEmailSupport() {
        waitForElementAndClick(support(), "Support is unavailable", 10);
        waitForElementVisible("id:Select a category for your inquiry", "Support destination did not open", 30);
    }

    @Override public void returnFromEmailSupport(Stage stage) {
        waitForElementAndClick("id:Back", "Cannot return from Support", 10);
        assertStage(stage);
    }

    @Override public void finishConfirmation() {
        waitForElementVisible(confirmed(), "Email confirmation did not succeed", 25);
        waitForElementAndClick(spanish ? "id:CERRAR" : "id:CLOSE", "Cannot close confirmation", 10);
        reachDailyWithoutDismissingRequest();
        waitForDaily();
        assertNoRequest();
    }

    @Override public void assertProfileEmail(String email) {
        openProfile();
        assertAuthenticatedProfileForEmail(email);
        waitForElementAndClick(TAB_TODAY, "Cannot return from Profile to Today", 10);
        waitForDaily();
    }

    private String loginAction() {
        return "xpath://XCUIElementTypeButton[@visible='true' and (@name='LOGIN' or @name='INICIAR SESIÓN'"
                + " or @name='INICIA SESIÓN' or @name='Iniciar sesión' or @name='ACCEDER' or @name='ENTRAR')]";
    }

    @Override public void assertExistingAccount(String email) {
        waitForElementVisible(existingTitle(), "Occupied email did not open its account screen", 25);
        String expected = email.toLowerCase(Locale.ROOT);
        createWait(10).withMessage("The existing-account screen does not identify the submitted address").until(ignored ->
                driver.findElements(getLocatorByString("xpath://XCUIElementTypeStaticText[@visible='true' and contains(@name,'@')]"))
                        .stream().anyMatch(element -> expected.equals(element.getText().trim().toLowerCase(Locale.ROOT))));
        waitForElementEnabled(loginAction(), "Existing-account login is unavailable", 10);
        Assert.assertFalse("Occupied email bypassed Login", isElementVisible(OTP) || isElementVisible(confirmed()));
    }

    @Override public void returnFromExistingAccount(Stage stage) {
        waitForElementAndClick(spanish ? "id:CANCELAR" : "id:CANCEL", "Cannot leave the existing-account screen", 10);
        assertStage(stage);
    }

    @Override public void assertSpanishExistingAccountCopy() {
        String question = waitForElementVisible("xpath://XCUIElementTypeStaticText[@visible='true'"
                + " and contains(@name,'?')]", "Existing-account question is absent", 10).getText();
        String action = waitForElementEnabled(loginAction(), "Existing-account login action is unavailable", 10).getAttribute("label");
        EmailReleaseScreen.assertSpanishLoginCopy(question, action);
    }

    @Override public boolean hasOnboardingGoals() { return isElementVisible(ONBOARDING_GOALS_TITLE); }

    @Override public void finishStartupLogin() {
        waitForFirstElementPresent(new String[]{ONBOARDING_GOALS_TITLE, TAB_TODAY, title(),
                confirmed(), "id:RESTORE", "id:subscription_paywall"}, "Startup OTP did not advance", 30);
        if (isElementVisible(confirmed())) {
            waitForElementAndClick("id:CLOSE", "Cannot close login confirmation", 10);
            waitForFirstElementPresent(new String[]{ONBOARDING_GOALS_TITLE, TAB_TODAY, title(), "id:RESTORE"},
                    "Login confirmation did not advance", 25);
        }
    }

}
