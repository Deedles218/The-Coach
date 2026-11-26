package lib.ui.android;

import io.appium.java_client.HasOnScreenKeyboard;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.android.nativekey.PressesKey;
import io.qameta.allure.Step;
import lib.EmailEnforcementFixture;
import lib.EmailEnforcementFixture.Stage;
import lib.Platform;
import lib.ui.EmailEnforcementScreen;
import lib.ui.EmailReleaseScreen;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.Quotes;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Daily's enforcement form, deliberately independent of generic popup dismissal. */
public final class AndroidEmailEnforcementPageObject extends AndroidCoachFlowPageObject
        implements EmailEnforcementScreen, EmailReleaseScreen {
    public static final String FORM_TITLE = "ENTER YOUR EMAIL TO SYNC YOUR PROGRESS AND SETTINGS";
    private static final String NOTIFICATION_DENY =
            "id:com.android.permissioncontroller:id/permission_deny_button";
    private static final Pattern COUNTDOWN = Pattern.compile(".*\\b(?:[1-9]|[1-5][0-9]|60)$");
    private final EmailEnforcementFixture fixture;
    private final String prefix = Platform.getInstance().getAndroidAppPackage() + ":id/";
    private final ReleaseCopy release;

    public AndroidEmailEnforcementPageObject(RemoteWebDriver driver, EmailEnforcementFixture fixture) {
        this(driver, fixture, null);
    }

    public static AndroidEmailEnforcementPageObject forRelease(RemoteWebDriver driver, String language) {
        return new AndroidEmailEnforcementPageObject(driver, null,
                ReleaseCopy.valueOf(language.toUpperCase(Locale.ROOT)));
    }

    private AndroidEmailEnforcementPageObject(RemoteWebDriver driver, EmailEnforcementFixture fixture,
                                              ReleaseCopy release) {
        super(driver);
        this.fixture = fixture;
        this.release = release;
    }

    private String id(String name) { return "id:" + prefix + name; }
    private String text(String value) { return "xpath://*[@text=" + Quotes.escape(value) + "]"; }
    private String title() { return text(release == null ? FORM_TITLE : release.formTitle); }
    private String remindLater() {
        return release == null ? text("REMIND ME LATER") : id("tvRemindLater");
    }

    public void waitForLaunchEntry() {
        waitForFirstElementPresent(new String[]{title(), id("clPaywallContainer"), id("btnClose"),
                id("tvHeader"), id("nav_graph_daily"), NOTIFICATION_DENY,
                "id:com.android.systemui:id/immersive_cling_title", "xpath://android.webkit.WebView"},
                "Android launch did not reach a known destination", 30);
    }

    public void reachDailyWithoutDismissingRequest() { closeOffersBeforeEmailOrDaily(); }

    public int closeOffersBeforeEmailOrDaily() {
        waitForLaunchEntry();
        return new AndroidOnboardingPageObject(driver).closePaywallsAndPopupsUntil(() ->
                isElementVisible(title()) || isElementVisible(id("tvDialogtitle")));
    }

    @Step("Open I’ve Already Purchased and follow the Please Start Here link")
    public void startFromPurchaseLogin() {
        openStartupLogin();
        WebElement link = waitForElementVisible(id("tvDontHaveLogin"), "Please Start Here is absent", 15);
        String label = link.getText();
        Assert.assertTrue("The login form did not show the Please Start Here link",
                label.toUpperCase(Locale.ROOT).endsWith("PLEASE START HERE"));
        // Only the final line of the two-line TextView is a clickable span.
        Rectangle bounds = link.getRect();
        Map<String, Integer> tap = new HashMap<>();
        tap.put("x", bounds.x + bounds.width / 2);
        tap.put("y", bounds.y + bounds.height * 3 / 4);
        driver.executeScript("mobile: clickGesture", tap);
        waitForElementVisible(id("tvAcive"), "Please Start Here did not open onboarding goals", 20);
    }

    public void waitForWelcome() {
        waitForFirstElementPresent(new String[]{id("btnSignInAnonymous"), NOTIFICATION_DENY},
                "Fresh installation did not reach Welcome", 30);
        if (isElementVisible(NOTIFICATION_DENY)) {
            waitForElementAndClick(NOTIFICATION_DENY, "Cannot dismiss the notification prompt", 8);
            waitForElementNotVisible(NOTIFICATION_DENY, "Notification prompt did not close", 8);
        }
        waitForElementVisible(id("btnSignInAnonymous"), "Welcome did not finish loading", 15);
    }

    public void openStartupLogin() {
        waitForWelcome();
        waitForElementAndClick(id("btnLogin"), "I’ve Already Purchased is unavailable", 15);
        waitForElementVisible(id("tvDontHaveLogin"), "Startup login did not open", 15);
    }

    @Step("Verify Android email stage: {stage}")
    public void assertStage(Stage stage) {
        waitForElementVisible(title(), "Daily email form is absent", 30);
        if (release == null) {
            waitForElementVisible(text(fixture.required("stage." + stage + ".message")), "Stage copy is absent", 10);
        } else if (stage == Stage.E2 || stage == Stage.E3) {
            WebElement warning = waitForElementVisible(id("tvLoseAccessWarning"),
                    "The access warning must be visible on the email form at " + stage, 8);
            Assert.assertFalse("The access warning has no text at " + stage, warning.getText().trim().isEmpty());
        } else if (stage == Stage.E1) {
            Assert.assertFalse("The access warning appeared before the second request",
                    isElementVisible(id("tvLoseAccessWarning")));
        }
        waitForElementVisible(id("edtEmail"), "Email input is absent", 10);
        waitForElementVisible(id("btnContinue"), "Continue is absent", 10);
        Assert.assertEquals("Unexpected cross availability at " + stage, stage.dismissible(),
                isElementVisible(id("btnNavigateUp")));
        Assert.assertFalse("Legacy bypass is exposed", isElementVisible(text("CONTINUE WITHOUT EMAIL")));
        if (release != null && stage.dismissible()) {
            waitForElementEnabled(id("btnNavigateUp"), "The optional form cannot be closed", 8);
            waitForElementEnabled(remindLater(), "Remind Me Later is unavailable at " + stage, 8);
        }
        if (!stage.dismissible()) {
            Assert.assertFalse("Mandatory form exposes Remind me later", isElementVisible(remindLater()));
            for (String tab : new String[]{"nav_graph_daily", "nav_graph_explore", "nav_graph_shop"}) {
                Assert.assertFalse("Content tab is accessible through mandatory form", isElementVisible(id(tab)));
            }
        }
        // Optional Remind is checked by COA-9696/later, so a missing action does not mask kill/navigation results.
    }

    public void assertMandatoryCannotBeDismissed(Stage stage) {
        Assert.assertFalse("The Back check requires a mandatory form", stage.dismissible());
        pressBack();
        assertStage(stage);
    }

    public boolean isDailyVisible() { return isElementVisible(TAB_TODAY_SELECTED); }

    public void assertPreviouslyDisplayedFirstRequest() {
        waitForElementVisible(title(), "The previous APK did not show its first email request", 20);
        waitForElementEnabled(id("btnNavigateUp"), "The previous APK did not show an optional request", 8);
        Assert.assertFalse("The previous APK is already on a later request",
                isElementVisible(id("tvLoseAccessWarning")));
    }

    @Step("Dismiss Android optional email using {action}")
    public void dismiss(Stage stage, String action) {
        Assert.assertTrue("Mandatory stage cannot be dismissed", stage.dismissible());
        Assert.assertTrue("Unknown dismissal action", Arrays.asList("cross", "later").contains(action));
        if ("later".equals(action)) {
            Assert.assertTrue("Optional email form has no Remind me later action", isElementVisible(remindLater()));
        }
        waitForElementAndClick("cross".equals(action) ? id("btnNavigateUp") : remindLater(),
                "Optional email dismissal is unavailable: " + action, 8);
        waitForElementNotVisible(title(), "Email form did not close", 10);
        if (stage == Stage.E2 && release == null) {
            waitForElementVisible(text(fixture.required("stage.E2.warning.title")), "E2 warning is absent", 10);
            waitForElementVisible(text(fixture.required("stage.E2.warning.message")), "E2 warning copy is absent", 10);
            waitForElementEnabled(fixture.required("stage.E2.warning.add"), "Link email is unavailable", 10);
            waitForElementAndClick(fixture.required("stage.E2.warning.later"), "Warning cannot be postponed", 10);
            waitForElementNotVisible(text(fixture.required("stage.E2.warning.title")), "E2 warning did not close", 10);
        }
        waitForDaily();
        assertNoRequest();
    }

    public void waitForDaily() {
        if (release == null) {
            waitForElementVisible(fixture.required("daily.content"), "Daily content did not load", 25);
            return;
        }
        waitForElementVisible(TAB_TODAY_SELECTED, "Today is not the selected tab", 25);
        waitForFirstElementPresent(new String[]{id("tvDaysNumber"), id("tvModuleName")},
                "Daily content did not load", 25);
    }

    public void assertNoRequest() {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        createWait(5).pollingEvery(Duration.ofMillis(250)).until(ignored -> {
            Assert.assertFalse("Email request reappeared", isElementVisible(title()));
            Assert.assertFalse("Duplicate OTP remains", isElementVisible(id("smsVerifyView")));
            Assert.assertFalse("Unexpected legacy email dialog covers Daily", isElementVisible(id("tvDialogtitle")));
            return System.nanoTime() >= deadline;
        });
    }

    public void revisitDaily() {
        waitForElementAndClick(id("nav_graph_explore"), "Explore tab is absent", 10);
        waitForElementVisible(release == null ? fixture.required("otherTab.content") : TAB_EXPLORE_SELECTED,
                "Explore did not open", 20);
        waitForElementAndClick(id("nav_graph_daily"), "Today tab is absent", 10);
        waitForDaily();
        assertNoRequest();
    }

    public void enterEmail(String email) {
        if (release != null) {
            WebElement input = waitForElementVisible(id("edtEmail"), "Email input is absent", 10);
            input.clear();
        }
        typeLoginEmail(email);
        Assert.assertTrue("Email input was lost", email.equals(
                waitForElementVisible(id("edtEmail"), "Email input is absent", 10).getAttribute("text")));
        waitForElementEnabled(id("btnContinue"), "Valid email did not enable Continue", 10);
    }

    public void submit() { waitForElementAndClick(id("btnContinue"), "Continue is unavailable", 10); }

    public void assertSingleOtp(String email) {
        waitForElementVisible(id("smsVerifyView"), "Email submission did not open OTP", 25);
        String expected = email.toLowerCase(Locale.ROOT);
        createWait(10).withMessage("OTP does not identify the submitted email").until(ignored -> driver
                .findElements(By.id(prefix + "tvSubTitleText")).stream().anyMatch(element ->
                        element.isDisplayed() && element.getText().toLowerCase(Locale.ROOT).contains(expected)));
        waitForElementVisible(id("tvResend"), "Resend control is absent", 10);
        waitForElementVisible(id("tvChangeEmail"), "Change email is absent", 10);
        Assert.assertEquals("Duplicate OTP controls", 1, driver.findElements(getLocatorByString(id("smsVerifyView"))).size());
        Assert.assertFalse("Email input remains over OTP", isElementVisible(id("edtEmail")));
    }

    // Keep the test address out of Allure step parameters.
    public void waitForResendReady() {
        waitForElementEnabled(text(release.resend), "Resend Code did not become available", 75);
    }

    public long resendLinkingCode(String email) {
        WebElement resend = waitForElementEnabled(text(release.resend), "Resend Code is unavailable", 5);
        long actionBoundary = System.currentTimeMillis();
        resend.click();
        createWait(12).withMessage("Resend Code did not restart the OTP countdown").until(ignored -> driver
                .findElements(By.id(prefix + "tvResend")).stream().anyMatch(element ->
                        element.isDisplayed() && COUNTDOWN.matcher(element.getText().trim()).matches()));
        assertSingleOtp(email);
        return actionBoundary;
    }

    // Keep the test code out of Allure step parameters.
    public void enterOtpWithLayoutCheck(String code) {
        Assert.assertTrue("Expected a four-digit test code", code != null && code.matches("[0-9]{4}"));
        WebElement input = waitForElementVisible(id("smsVerifyView"), "OTP field is absent", 10);
        if (!keyboardShown()) {
            input.click();
            if (!keyboardShown()) {
                driver.executeScript("mobile: clickGesture",
                        Collections.singletonMap("elementId", ((RemoteWebElement) input).getId()));
            }
        }
        createWait(8).withMessage("The numeric keyboard did not open").until(ignored -> keyboardShown());
        pressOtpDigit(code.charAt(0));
        Assert.assertTrue("The numeric keyboard closed during code entry", keyboardShown());
        WebElement support = waitForElementVisible(id("tvSupportText"), "Support is not visible above the keyboard", 8);
        Rectangle fields = waitForElementVisible(id("smsVerifyView"), "OTP fields are absent", 8).getRect();
        Assert.assertTrue("Support overlaps the OTP fields",
                support.getRect().y >= fields.y + fields.height);
        for (int index = 1; index < code.length(); index++) pressOtpDigit(code.charAt(index));
    }

    private void pressOtpDigit(char digit) {
        ((PressesKey) driver).pressKey(new KeyEvent(AndroidKey.valueOf("DIGIT_" + digit)));
    }

    @Step("Verify the OTP confirmation and that the keyboard has closed")
    public void assertConfirmationLayout() {
        waitForElementVisible(id("tvConfirmText"), "Email confirmation did not succeed", 30);
        createWait(8).withMessage("The OTP keyboard is still visible after confirmation")
                .until(ignored -> !keyboardShown());
        Rectangle icon = waitForElementVisible(id("ivConfirmOk"), "Confirmation icon is absent", 8).getRect();
        Rectangle support = waitForElementVisible(id("tvSupportText"), "Confirmation Support is absent", 8).getRect();
        Assert.assertTrue("Support overlaps the confirmation icon", support.y >= icon.y + icon.height);
    }

    public void changeEmailAndCheckValidation(Stage stage) {
        waitForElementAndClick(id("tvChangeEmail"), "Change email is unavailable", 10);
        assertStage(stage);
        assertEmptyEmail();
        typeLoginEmail("not-an-email");
        if (release != null) {
            assertElementDisabled(id("btnContinue"), "An invalid email enabled Continue", 5);
        } else if (isElementEnabled(waitForElementVisible(id("btnContinue"), "Continue is absent", 5))) {
            submit();
            assertLoginValidationErrorIsDisplayed();
        }
        Assert.assertFalse("Invalid email advanced to OTP", isElementVisible(id("smsVerifyView")));
        assertStage(stage);
    }

    public void assertEmptyEmail() {
        String value = waitForElementVisible(id("edtEmail"), "Changed email input is absent", 10).getAttribute("text");
        String placeholder = release == null ? fixture.required("email.placeholder")
                : waitForElementVisible(id("tvEnterEmail"), "Email field label is absent", 8).getText();
        Assert.assertTrue("Change email retained the old address", value == null || value.isEmpty()
                || placeholder.equals(value));
        assertElementDisabled(id("btnContinue"), "Empty email enabled Continue", 5);
    }

    public void openEmailSupport() {
        hideKeyboardIfPossible();
        if (release != null) {
            WebElement support = waitForElementVisible(id("tvSupportText"), "Email Support is unavailable", 10);
            Rectangle bounds = support.getRect();
            // The underlined Support span occupies the trailing part of the last line.
            Map<String, Integer> tap = new HashMap<>();
            tap.put("x", bounds.x + bounds.width * 2 / 3);
            tap.put("y", bounds.y + bounds.height * 3 / 4);
            driver.executeScript("mobile: clickGesture", tap);
            waitForElementVisible(id("wvContent"), "Support did not open its in-app destination", 25);
            return;
        }
        waitForElementAndClick(fixture.required("form.support"), "Email Support is unavailable", 10);
        waitForElementVisible(fixture.required("support.destination"), "Support did not open its real destination", 25);
    }

    public void returnFromEmailSupport(Stage stage) {
        if (release == null) driver.navigate().back();
        else pressBack();
        waitForElementNotVisible(release == null ? fixture.required("support.destination") : id("wvContent"),
                "Support did not close", 10);
        assertStage(stage);
    }

    public void finishConfirmation() {
        waitForElementVisible(release == null ? text("Your Email is confirmed!") : id("tvConfirmText"),
                "Email confirmation did not succeed", 25);
        waitForElementAndClick(id("btnClose"), "Cannot close successful confirmation", 10);
        if (release != null) reachDailyWithoutDismissingRequest();
        waitForDaily();
        assertNoRequest();
    }

    public void assertProfileEmail(String email) {
        openProfile();
        assertAuthenticatedProfileForEmail(email);
        returnToMainScreenFromProfile();
        waitForDaily();
    }

    public void assertExistingAccount(String email) {
        waitForElementVisible(text(release == null ? "WE’VE FOUND AN EXISTING ACCOUNT" : release.existingAccount),
                "Occupied email did not offer Login", 25);
        if (release == null) {
            waitForElementVisible(text("DO YOU WANT TO LOG IN WITH THIS EMAIL?"), "Existing-account explanation is absent", 10);
            waitForElementEnabled(text("LOGIN"), "Existing-account Login is unavailable", 10);
        } else {
            waitForElementVisible(id("tvSubTitleText"), "Existing-account explanation is absent", 10);
            String action = waitForElementEnabled(id("btnContinue"), "Existing-account Login is unavailable", 10).getText();
            if (release == ReleaseCopy.EN) Assert.assertEquals("Incorrect existing-account action", "LOGIN", action);
        }
        Assert.assertFalse("Occupied email bypassed Login", isElementVisible(id("smsVerifyView")));
        Assert.assertFalse("Occupied email was linked", isElementVisible(id("tvConfirmText")));
    }

    @Step("Use Android Back on the existing-account screen and return to email entry")
    public void returnFromExistingAccount(Stage stage) {
        pressBack();
        assertStage(stage);
    }

    @Step("Verify the existing-account question and Login button are in Spanish")
    public void assertSpanishExistingAccountCopy() {
        String question = waitForElementVisible(id("tvSubTitleText"), "Login question is absent", 10).getText();
        String button = waitForElementEnabled(id("btnContinue"), "Login button is unavailable", 10).getText();
        EmailReleaseScreen.assertSpanishLoginCopy(question, button);
    }

    public boolean hasOnboardingGoals() { return isElementVisible(id("tvAcive")); }

    public void finishStartupLogin() {
        waitForFirstElementPresent(new String[]{id("tvConfirmText"), id("tvAcive"), id("nav_graph_daily"),
                title(), id("btnClose"), id("clPaywallContainer")},
                "Startup login did not leave the OTP screen", 30);
        if (isElementVisible(id("tvConfirmText"))) {
            waitForElementAndClick(id("btnClose"), "Cannot close the successful login confirmation", 10);
            waitForFirstElementPresent(new String[]{id("tvAcive"), id("nav_graph_daily"), title(),
                    id("clPaywallContainer"), id("btnClose")}, "Login confirmation did not advance", 25);
        }
    }

    private void pressBack() {
        ((PressesKey) driver).pressKey(new KeyEvent(AndroidKey.BACK));
    }

    private boolean keyboardShown() { return ((HasOnScreenKeyboard) driver).isKeyboardShown(); }

    private enum ReleaseCopy {
        EN(FORM_TITLE, "RESEND CODE", "WE’VE FOUND AN EXISTING ACCOUNT"),
        ES("INGRESA TU CORREO PARA SINCRONIZAR TU PROGRESO Y CONFIGURACIÓN", "REENVIAR CÓDIGO",
                "HEMOS ENCONTRADO UNA CUENTA EXISTENTE"),
        FR("ENTREZ VOTRE E-MAIL POUR SYNCHRONISER VOTRE PROGRESSION ET VOS PARAMÈTRES",
                "RENVOYER LE CODE", ""),
        DE("GIB DEINE E-MAIL-ADRESSE EIN, UM DEINEN FORTSCHRITT UND DEINE EINSTELLUNGEN ZU SYNCHRONISIEREN",
                "CODE ERNEUT SENDEN", ""),
        IT("INSERISCI LA TUA EMAIL PER SINCRONIZZARE I TUOI PROGRESSI E LE IMPOSTAZIONI",
                "INVIA DI NUOVO IL CODICE", "");

        private final String formTitle;
        private final String resend;
        private final String existingAccount;

        ReleaseCopy(String formTitle, String resend, String existingAccount) {
            this.formTitle = formTitle;
            this.resend = resend;
            this.existingAccount = existingAccount;
        }
    }

    public void submitRepeatedly() { throw new UnsupportedOperationException("COA-9415: not required for automation"); }
    public void assertLegacyOutcome() { throw new UnsupportedOperationException("COA-9697: not required for automation"); }
    public void assertDailyUnavailable() { throw new UnsupportedOperationException("COA-9698: not required for automation"); }
}
