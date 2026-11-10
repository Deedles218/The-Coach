package lib.ui.ios;

import io.qameta.allure.Step;
import lib.EmailEnforcementFixture;
import lib.EmailEnforcementFixture.Stage;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Quotes;

import java.time.Duration;
import java.io.ByteArrayInputStream;
import java.util.*;

/** COA-9222 entry is the Daily form. Profile/Welcome login never substitutes for it. */
public final class iOSEmailEnforcementPageObject extends iOSCoachFlowPageObject implements lib.ui.EmailEnforcementScreen {
    private final EmailEnforcementFixture fixture;
    private static final String EMAIL = "xpath://XCUIElementTypeTextField";
    private static final String CONTINUE = "id:CONTINUE";
    private static final String CLOSE = "id:CloseRoundBlack";
    private static final String LATER = button("REMIND ME LATER");
    private static final String WITHOUT_EMAIL = button("CONTINUE WITHOUT EMAIL");
    private static final String OTP = "xpath://XCUIElementTypeStaticText[starts-with(@name,"
            + " 'An email with a code has been sent to:')]";

    public iOSEmailEnforcementPageObject(RemoteWebDriver driver, EmailEnforcementFixture fixture) {
        super(driver);
        this.fixture = fixture;
    }

    @Step("Wait for the app to leave the launch screen before handling paywalls")
    public void waitForLaunchEntry() {
        waitForFirstElementPresent(new String[]{title(), "id:RESTORE", "id:Today",
                "xpath://XCUIElementTypeWebView"}, "App launch did not reach a known screen", 30);
    }

    @Step("Verify email enforcement stage: {stage}")
    public void assertStage(Stage stage) {
        waitForElementVisible(title(), "Daily email form is absent", 30);
        waitForElementVisible("id:" + fixture.required("stage." + stage + ".message"),
                "Email form has the wrong stage text", 10);
        waitForElementVisible(EMAIL, "Daily email form has no input", 10);
        waitForElementVisible(CONTINUE, "Daily email form has no Continue control", 10);
        Assert.assertEquals("Unexpected cross availability at " + stage, stage.dismissible(), isElementVisible(CLOSE));
        Assert.assertEquals("Unexpected Remind me later availability at " + stage, stage.dismissible(), isElementVisible(LATER));
        Assert.assertFalse("New enforcement must not expose the legacy bypass", isElementVisible(WITHOUT_EMAIL));
        if (!stage.dismissible()) assertContentBlocked();
    }

    @Step("Verify mandatory email form blocks content")
    public void assertContentBlocked() {
        for (String tab : new String[]{"id:Today", "id:Explore", "id:Shop"}) {
            for (WebElement element : driver.findElements(getLocatorByString(tab))) {
                Assert.assertFalse("Content tab is hittable through the mandatory form",
                        element.isDisplayed() && "true".equalsIgnoreCase(element.getAttribute("hittable")));
            }
        }
    }

    @Step("Dismiss an optional email request")
    public void dismiss(Stage stage, String action) {
        Assert.assertTrue("A mandatory stage cannot be dismissed", stage.dismissible());
        Assert.assertTrue("Dismiss action must be cross or later", Arrays.asList("cross", "later").contains(action));
        waitForElementAndClick("cross".equals(action) ? CLOSE : LATER, "Optional email dismissal is unavailable", 10);
        waitForElementNotVisible(title(), "Optional email form did not close", 10);
        if (stage == Stage.E2) confirmSecondStageWarning();
        waitForDaily();
    }

    @Step("Wait for rendered Daily content")
    public void waitForDaily() {
        waitForElementVisible(fixture.required("daily.content"), "Daily content did not load", 30);
    }

    @Step("Verify the email form remains absent")
    public void assertNoRequest() {
        final long until = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        createWait(5).pollingEvery(Duration.ofMillis(200)).until(ignored -> {
            Assert.assertFalse("Email request reappeared", isElementVisible(title()));
            Assert.assertFalse("A duplicate OTP screen remains", isElementVisible(OTP));
            return System.nanoTime() >= until;
        });
    }

    @Step("Revisit Daily within the current process")
    public void revisitDaily() {
        // Direct tab navigation preserves the measured form; openToday() dismisses legacy prompts.
        waitForElementAndClick(fixture.value("otherTab.control", "id:Explore"), "The other tab is unavailable", 10);
        waitForElementVisible(fixture.required("otherTab.content"), "The other tab did not render", 20);
        waitForElementAndClick("id:Today", "Today tab is unavailable", 10);
        waitForDaily();
        assertNoRequest();
    }

    public void enterEmail(String email) {
        typeLoginEmail(email);
        Assert.assertEquals("Daily email field lost the input", email,
                waitForElementVisible(EMAIL, "Email input is absent", 10).getAttribute("value"));
        waitForElementEnabled(CONTINUE, "A valid free email did not enable Continue", 10);
    }

    @Step("Verify the second-request warning and postpone binding")
    private void confirmSecondStageWarning() {
        waitForElementVisible("id:" + fixture.required("stage.E2.warning.title"), "E2 warning title is absent", 10);
        waitForElementVisible("id:" + fixture.required("stage.E2.warning.message"), "E2 does not warn about mandatory binding", 10);
        waitForElementEnabled(fixture.required("stage.E2.warning.add"), "E2 warning has no Add email action", 10);
        waitForElementEnabled(fixture.required("stage.E2.warning.later"), "E2 warning has no postponement action", 10);
        Assert.assertFalse("Warning must not contain an email input", isElementVisible(EMAIL));
        Assert.assertFalse("Warning must not expose OTP", isElementVisible(OTP));
        io.qameta.allure.Allure.addAttachment("E2 mandatory-binding warning", "image/png",
                new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)), "png");
        waitForElementAndClick(fixture.required("stage.E2.warning.later"), "Cannot postpone binding from E2 warning", 10);
        waitForElementNotVisible("id:" + fixture.required("stage.E2.warning.title"), "E2 warning did not close", 10);
    }

    @Step("Submit email from the Daily request")
    public void submit() { waitForElementAndClick(CONTINUE, "Daily email submission is unavailable", 10); }

    @Step("Try repeated submission during the pending response")
    public void submitRepeatedly() {
        Rectangle bounds = waitForElementEnabled(CONTINUE, "Continue is unavailable", 10).getRect();
        submit();
        String pending = fixture.required("submission.pending");
        waitForElementVisible(pending, "Fixture did not expose the pending response needed for duplicate taps", 5);
        Map<String, Object> tap = new HashMap<>();
        tap.put("x", bounds.x + bounds.width / 2);
        tap.put("y", bounds.y + bounds.height / 2);
        for (int i = 0; i < 2; i++) {
            Assert.assertTrue("Response completed before repeated taps; use the delayed-response fixture", isElementVisible(pending));
            driver.executeScript("mobile: tap", tap);
        }
    }

    public void assertSingleOtp(String email) {
        String missing = "id:Email not found. Contact support below.";
        String existing = "id:WE’VE FOUND AN EXISTING ACCOUNT";
        String confirm = "id:CONFIRM YOUR EMAIL";
        waitForFirstElementPresent(new String[]{OTP, missing, existing, confirm},
                "Email submission produced neither confirmation nor a recognized result", 20);
        Assert.assertFalse("Application rejected the free fixture email: Email not found. Contact support below.",
                isElementVisible(missing));
        Assert.assertFalse("The supposedly free fixture email already belongs to an account", isElementVisible(existing));
        if (isElementVisible(confirm)) {
            waitForElementAndClick("id:SEND CODE", "Cannot request the email confirmation code", 10);
        }
        assertOtpScreenIsDisplayedForEmail(email);
        waitForElementNotVisible(EMAIL, "Email input field remains over OTP", 10);
        int visible = 0;
        for (WebElement element : driver.findElements(getLocatorByString(OTP))) if (element.isDisplayed()) visible++;
        Assert.assertEquals("Submission produced duplicate confirmation screens", 1, visible);
    }

    @Override public void assertOtpScreenIsDisplayedForEmail(String email) {
        waitForElementVisible(OTP, "Daily email confirmation is absent", 20);
        waitForElementVisible(OTP + "[contains(@name," + Quotes.escape(email) + ")]",
                "Confirmation does not identify the submitted address", 10);
        waitForElementVisible("id:RESEND CODE", "Confirmation has no Resend code control", 10);
        waitForElementVisible("id:CHANGE EMAIL", "Confirmation has no Change email control", 10);
    }

    @Override public void typeSecurityCode(String code) {
        // This reused input has a different screen marker from Welcome/Profile authorization.
        waitForElementVisible(OTP, "Daily email confirmation is absent", 10);
        for (int i = 0; i < code.length(); i++) {
            waitForElementAndClick("id:" + code.charAt(i), "Cannot enter the confirmation code", 10);
        }
    }

    @Step("Verify Change email restores the mandatory form")
    public void changeEmailAndCheckValidation(Stage stage) {
        waitForElementAndClick(fixture.required("otp.changeEmail"), "Change email is unavailable", 10);
        assertStage(stage);
        WebElement field = waitForElementVisible(EMAIL, "Changed email input is absent", 10);
        String value = field.getAttribute("value");
        Assert.assertTrue("Change email retained the old address", value == null || value.isEmpty()
                || fixture.required("email.placeholder").equals(value));
        assertElementDisabled(CONTINUE, "Empty email must not enable Continue", 5);
        typeLoginEmail("not-an-email");
        WebElement submit = waitForElementVisible(CONTINUE, "Continue disappeared", 5);
        if (isElementEnabled(submit)) {
            submit.click();
            assertLoginValidationErrorIsDisplayed();
        }
        Assert.assertFalse("Invalid email advanced to OTP", isElementVisible(OTP));
        assertStage(stage);
    }

    @Step("Open Support from the mandatory email form")
    public void openEmailSupport() {
        hideKeyboardIfPossible();
        waitForElementAndClick(fixture.required("form.support"), "Support on the email form is unavailable", 10);
        waitForElementVisible(fixture.required("support.destination"), "Support did not open its real destination", 30);
    }

    @Step("Return from Support to the mandatory email form")
    public void returnFromEmailSupport(Stage stage) {
        waitForElementAndClick(fixture.required("support.back"), "Cannot return from Support", 10);
        waitForElementNotVisible(fixture.required("support.destination"), "Support remains over the email form", 10);
        assertStage(stage);
    }

    @Step("Close the successful email confirmation")
    public void finishConfirmation() {
        waitForElementVisible("id:Your Email is confirmed!", "Email confirmation did not succeed", 30);
        waitForElementAndClick("id:CLOSE", "Cannot close successful confirmation", 10);
        waitForDaily();
        assertNoRequest();
    }

    public void assertProfileEmail(String email) {
        openProfile();
        assertAuthenticatedProfileForEmail(email);
        waitForElementAndClick("id:Today", "Cannot return from Profile to Today", 10);
        waitForDaily();
    }

    public void assertExistingAccount(String email) {
        waitForElementVisible("id:WE’VE FOUND AN EXISTING ACCOUNT", "Occupied email did not offer the existing account", 20);
        waitForElementVisible("xpath://XCUIElementTypeStaticText[@name=" + Quotes.escape(email) + "]",
                "Existing-account offer does not identify the entered email", 10);
        waitForElementEnabled("id:LOGIN", "Existing-account Login is unavailable", 10);
        Assert.assertFalse("Occupied email bypassed the required Login", isElementVisible(OTP));
        Assert.assertFalse("Occupied email reported a successful link", isElementVisible("id:Your Email is confirmed!"));
    }

    @Step("Verify the applicable legacy flow after feature OFF")
    public void assertLegacyOutcome() {
        String outcome = fixture.required("legacy.outcome");
        if ("daily".equals(outcome)) {
            waitForElementNotVisible(title(), "Feature OFF left the mandatory form open", 15);
            waitForDaily();
        } else {
            Assert.assertEquals("Unknown expected legacy outcome", "dismissible-form", outcome);
            waitForElementAndClick(CLOSE, "Feature OFF did not restore the legacy cross", 15);
            waitForElementAndClick(WITHOUT_EMAIL, "Legacy Continue without email is unavailable", 10);
            waitForDaily();
        }
    }

    public void assertDailyUnavailable() {
        waitForElementVisible(fixture.required("daily.unavailable"), "Offline fixture did not block Daily loading", 20);
        Assert.assertFalse("Cached Daily makes this offline dataset inapplicable", isElementVisible(fixture.required("daily.content")));
        assertNoRequest();
    }

    private String title() { return "id:" + fixture.required("form.title"); }

    private static String button(String name) {
        return "xpath://XCUIElementTypeButton[translate(normalize-space(@name),"
                + "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='" + name + "']";
    }
}
