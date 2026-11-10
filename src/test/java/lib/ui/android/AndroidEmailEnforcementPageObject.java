package lib.ui.android;

import io.qameta.allure.Step;
import lib.EmailEnforcementFixture;
import lib.EmailEnforcementFixture.Stage;
import lib.Platform;
import lib.ui.EmailEnforcementScreen;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Quotes;

import java.time.Duration;
import java.util.Arrays;

/** Daily's enforcement form, deliberately independent of generic popup dismissal. */
public final class AndroidEmailEnforcementPageObject extends AndroidCoachFlowPageObject implements EmailEnforcementScreen {
    public static final String FORM_TITLE = "ENTER YOUR EMAIL TO SYNC YOUR PROGRESS AND SETTINGS";
    private final EmailEnforcementFixture fixture;
    private final String prefix = Platform.getInstance().getAndroidAppPackage() + ":id/";

    public AndroidEmailEnforcementPageObject(RemoteWebDriver driver, EmailEnforcementFixture fixture) {
        super(driver);
        this.fixture = fixture;
    }

    private String id(String name) { return "id:" + prefix + name; }
    private String text(String value) { return "xpath://*[@text=" + Quotes.escape(value) + "]"; }
    private String title() { return text(FORM_TITLE); }

    public void waitForLaunchEntry() {
        waitForFirstElementPresent(new String[]{title(), id("clPaywallContainer"), id("btnClose"),
                id("tvHeader"), id("nav_graph_daily"), "xpath://android.webkit.WebView"},
                "Android launch did not reach a known destination", 30);
    }

    public void reachDailyWithoutDismissingRequest() {
        waitForLaunchEntry();
        new AndroidOnboardingPageObject(driver).closePaywallsAndPopupsUntil(() ->
                isElementVisible(title()) || isElementVisible(id("tvDialogtitle")));
    }

    @Step("Verify Android email stage: {stage}")
    public void assertStage(Stage stage) {
        waitForElementVisible(title(), "Daily email form is absent", 30);
        waitForElementVisible(text(fixture.required("stage." + stage + ".message")), "Stage copy is absent", 10);
        waitForElementVisible(id("edtEmail"), "Email input is absent", 10);
        waitForElementVisible(id("btnContinue"), "Continue is absent", 10);
        Assert.assertEquals("Unexpected cross availability at " + stage, stage.dismissible(),
                isElementVisible(id("btnNavigateUp")));
        Assert.assertFalse("Legacy bypass is exposed", isElementVisible(text("CONTINUE WITHOUT EMAIL")));
        if (!stage.dismissible()) {
            Assert.assertFalse("Mandatory form exposes Remind me later", isElementVisible(text("REMIND ME LATER")));
            for (String tab : new String[]{"nav_graph_daily", "nav_graph_explore", "nav_graph_shop"}) {
                Assert.assertFalse("Content tab is accessible through mandatory form", isElementVisible(id(tab)));
            }
        }
        // Optional Remind is checked by COA-9696/later, so a missing action does not mask kill/navigation results.
    }

    @Step("Dismiss Android optional email using {action}")
    public void dismiss(Stage stage, String action) {
        Assert.assertTrue("Mandatory stage cannot be dismissed", stage.dismissible());
        Assert.assertTrue("Unknown dismissal action", Arrays.asList("cross", "later").contains(action));
        if ("later".equals(action)) {
            Assert.assertTrue("Optional email form has no Remind me later action", isElementVisible(text("REMIND ME LATER")));
        }
        waitForElementAndClick("cross".equals(action) ? id("btnNavigateUp") : text("REMIND ME LATER"),
                "Optional email dismissal is unavailable: " + action, 8);
        waitForElementNotVisible(title(), "Email form did not close", 10);
        if (stage == Stage.E2) {
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
        waitForElementVisible(fixture.required("daily.content"), "Daily content did not load", 25);
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
        waitForElementVisible(fixture.required("otherTab.content"), "Explore content did not load", 20);
        waitForElementAndClick(id("nav_graph_daily"), "Today tab is absent", 10);
        waitForDaily();
        assertNoRequest();
    }

    public void enterEmail(String email) {
        typeLoginEmail(email);
        Assert.assertEquals("Email input was lost", email,
                waitForElementVisible(id("edtEmail"), "Email input is absent", 10).getAttribute("text"));
        waitForElementEnabled(id("btnContinue"), "Valid email did not enable Continue", 10);
    }

    public void submit() { waitForElementAndClick(id("btnContinue"), "Continue is unavailable", 10); }

    public void assertSingleOtp(String email) {
        waitForElementVisible(id("smsVerifyView"), "Email submission did not open OTP", 25);
        waitForElementVisible("xpath://*[@resource-id='" + prefix + "tvSubTitleText' and contains(@text,"
                + Quotes.escape(email) + ")]", "OTP does not identify the submitted email", 10);
        waitForElementVisible(id("tvResend"), "Resend control is absent", 10);
        waitForElementVisible(id("tvChangeEmail"), "Change email is absent", 10);
        Assert.assertEquals("Duplicate OTP controls", 1, driver.findElements(getLocatorByString(id("smsVerifyView"))).size());
        Assert.assertFalse("Email input remains over OTP", isElementVisible(id("edtEmail")));
    }

    public void changeEmailAndCheckValidation(Stage stage) {
        waitForElementAndClick(id("tvChangeEmail"), "Change email is unavailable", 10);
        assertStage(stage);
        String value = waitForElementVisible(id("edtEmail"), "Changed email input is absent", 10).getAttribute("text");
        Assert.assertTrue("Change email retained the old address", value == null || value.isEmpty()
                || fixture.required("email.placeholder").equals(value));
        assertElementDisabled(id("btnContinue"), "Empty email enabled Continue", 5);
        typeLoginEmail("not-an-email");
        if (isElementEnabled(waitForElementVisible(id("btnContinue"), "Continue is absent", 5))) {
            submit();
            assertLoginValidationErrorIsDisplayed();
        }
        Assert.assertFalse("Invalid email advanced to OTP", isElementVisible(id("smsVerifyView")));
        assertStage(stage);
    }

    public void openEmailSupport() {
        hideKeyboardIfPossible();
        waitForElementAndClick(fixture.required("form.support"), "Email Support is unavailable", 10);
        waitForElementVisible(fixture.required("support.destination"), "Support did not open its real destination", 25);
    }

    public void returnFromEmailSupport(Stage stage) {
        driver.navigate().back();
        waitForElementNotVisible(fixture.required("support.destination"), "Support did not close", 10);
        assertStage(stage);
    }

    public void finishConfirmation() {
        waitForElementVisible(text("Your Email is confirmed!"), "Email confirmation did not succeed", 25);
        waitForElementAndClick(id("btnClose"), "Cannot close successful confirmation", 10);
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
        waitForElementVisible(text("WE’VE FOUND AN EXISTING ACCOUNT"), "Occupied email did not offer Login", 25);
        waitForElementVisible(text("DO YOU WANT TO LOG IN WITH THIS EMAIL?"), "Existing-account explanation is absent", 10);
        waitForElementEnabled(text("LOGIN"), "Existing-account Login is unavailable", 10);
        Assert.assertFalse("Occupied email bypassed Login", isElementVisible(id("smsVerifyView")));
        Assert.assertFalse("Occupied email was linked", isElementVisible(text("Your Email is confirmed!")));
    }

    public void submitRepeatedly() { throw new UnsupportedOperationException("COA-9415: not required for automation"); }
    public void assertLegacyOutcome() { throw new UnsupportedOperationException("COA-9697: not required for automation"); }
    public void assertDailyUnavailable() { throw new UnsupportedOperationException("COA-9698: not required for automation"); }
}
