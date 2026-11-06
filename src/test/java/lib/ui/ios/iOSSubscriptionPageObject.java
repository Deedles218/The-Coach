package lib.ui.ios;

import io.appium.java_client.HasSettings;
import io.qameta.allure.Step;
import lib.Platform;
import lib.ui.SubscriptionPageObject;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.Map;

public class iOSSubscriptionPageObject extends SubscriptionPageObject {
    private static final String PAYWALL_MARKER =
            "xpath://XCUIElementTypeStaticText[contains(translate(@name, "
                    + "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),\"THE COACH'S HERE TO HELP\")]";
    private static final String TOP_PLAN =
            "xpath://XCUIElementTypeButton[@visible='true' and "
                    + ".//XCUIElementTypeStaticText[@name='One Month Plan']]";
    private static final String BOTTOM_PLAN =
            "xpath://XCUIElementTypeButton[@visible='true' and "
                    + ".//XCUIElementTypeStaticText[@name='One Year Plan']]";
    private static final String CONTINUE_BUTTON = "id:CONTINUE";
    private static final String RESTORE_BUTTON = "id:RESTORE";
    private static final String DISCLOSURE =
            "xpath://XCUIElementTypeStaticText[contains(@name,'Subscription renews automatically.') "
                    + "and contains(@name,'Privacy Policy') and contains(@name,'Terms of Service')]";
    private static final String TODAY_TAB = "id:Today";
    private static final String PREMIUM_MARKER = "id:PREMIUM SUBSCRIBER";
    private static final String SANDBOX_ENVIRONMENT =
            "xpath://XCUIElementTypeStaticText[@visible='true' and (contains(@name,'Environment: Sandbox') "
                    + "or @name='For testing purposes only. You will not be charged for confirming this purchase.')]";
    private static final String PURCHASE_SUCCESS = "id:Your purchase was successful.";
    private static final String STORE_PASSWORD = "xpath://XCUIElementTypeSecureTextField[@visible='true']";
    private static final String TESTFLIGHT_AUTH_NOTICE =
            "xpath://XCUIElementTypeStaticText[@visible='true' and contains(@name,"
                    + "'Beta testers are not charged for this In-App purchase')]";

    private static final String[] PURCHASE_SHEET_MARKERS = new String[]{
            "id:Subscribe",
            "id:Buy",
            SANDBOX_ENVIRONMENT,
            "xpath://XCUIElementTypeSheet[@visible='true']",
            "xpath://XCUIElementTypeStaticText[@visible='true' and ("
                    + "contains(@name,'Confirm Your Subscription') "
                    + "or contains(@name,'Double Click to Subscribe') "
                    + "or contains(@name,'Environment: Sandbox'))]"
    };
    private static final String[] PURCHASE_CONFIRM_BUTTONS = new String[]{
            "id:Subscribe",
            "id:Buy",
            "id:Confirm",
            "xpath://XCUIElementTypeButton[@visible='true' and @label='Subscribe']"
    };
    private static final By TERMS_DESTINATION = By.xpath(
            "//XCUIElementTypeWebView[@visible='true'] "
                    + "| //XCUIElementTypeButton[@visible='true' and @name='Done'] "
                    + "| //XCUIElementTypeNavigationBar[@visible='true' and ("
                    + "contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'privacy') "
                    + "or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'terms'))] "
                    + "| //XCUIElementTypeApplication[@visible='true' and @name='Safari']"
    );
    private static final By RESTORE_RESULT = By.xpath(
            "//XCUIElementTypeAlert[@visible='true'] "
                    + "| //XCUIElementTypeStaticText[@visible='true' and ("
                    + "contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'restor') "
                    + "or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no active') "
                    + "or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'not found') "
                    + "or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'expired')) "
                    + "and not(ancestor::XCUIElementTypeButton[@name='RESTORE'])]"
    );

    public iOSSubscriptionPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Verify TestFlight subscription paywall is fully loaded")
    public void assertPaywallIsDisplayed() {
        createWait(30).withMessage("iOS subscription paywall title is not displayed\n")
                .until(webDriver -> isElementVisible(PAYWALL_MARKER) || isElementVisible(TODAY_TAB));
        Assert.assertTrue(
                "Active StoreKit entitlement bypassed the onboarding paywall. Clear the Sandbox Apple "
                        + "Account purchase history or use a fresh sandbox account before this case.",
                isElementVisible(PAYWALL_MARKER)
        );
        waitForElementPresent(TOP_PLAN, "Top One Month Plan is not displayed", 20);
        waitForElementPresent(BOTTOM_PLAN, "Bottom One Year Plan is not displayed", 20);
        waitForElementEnabled(CONTINUE_BUTTON, "Paywall Continue button is not enabled", 15);
    }

    @Override
    @Step("Select {position} iOS subscription plan")
    public void selectPlan(PlanPosition position) {
        String planLocator = position == PlanPosition.TOP ? TOP_PLAN : BOTTOM_PLAN;
        WebElement plan = waitForElementEnabled(
                planLocator,
                "Cannot select " + position + " iOS subscription plan",
                15
        );
        plan.click();
        String selectedLocator = planLocator
                + "/XCUIElementTypeImage[@name='ic_glyph_check' and @visible='true']";
        waitForElementPresent(
                selectedLocator,
                position + " iOS subscription plan did not become selected",
                10
        );
    }

    @Override
    @Step("Open iOS StoreKit sandbox purchase sheet")
    public void startPurchaseAndWaitForStore() {
        waitForElementAndClick(CONTINUE_BUTTON, "Cannot tap Continue on iOS paywall", 15);
        inSystemPurchaseWindow(() -> waitForFirstElementPresent(
                PURCHASE_SHEET_MARKERS,
                "iOS StoreKit purchase sheet did not open",
                30
        ));
    }

    @Override
    @Step("Confirm iOS StoreKit sandbox purchase")
    public void confirmTestPurchase() {
        Assert.assertTrue("Confirming a test purchase requires explicit Sandbox and purchase flags",
                Platform.getInstance().isStoreKitSandboxEnabled()
                        && Platform.getInstance().isStoreKitPurchaseAllowed());
        inSystemPurchaseWindow(() -> {
            waitForElementPresent(SANDBOX_ENVIRONMENT,
                    "Purchase was not confirmed: the native sheet does not identify a test purchase", 10);
            waitForFirstElementAndClick(
                PURCHASE_CONFIRM_BUTTONS,
                "Cannot confirm StoreKit sandbox purchase. A real-device side-button or biometric confirmation may require manual test-lab support.",
                20
            );
        });
    }

    private void inSystemPurchaseWindow(Runnable action) {
        Object previous = activeApplication();
        try {
            activeApplication("com.apple.springboard");
            action.run();
        } finally {
            activeApplication(previous == null ? "auto" : previous);
        }
    }

    @Step("Verify selected StoreKit subscription explicitly offers a free trial")
    public void assertFreeTrialIsOffered() {
        inSystemPurchaseWindow(() -> {
            try {
                WebElement offer = waitForElementVisible(
                    "xpath://XCUIElementTypeStaticText[@visible='true' and ("
                            + "contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'free trial')"
                            + " or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'days free')"
                            + " or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'week free')"
                            + " or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'month free'))]",
                    "The selected native StoreKit offer has no verified free trial; do not substitute a regular purchase", 10);
                io.qameta.allure.Allure.addAttachment("Selected native StoreKit trial offer", normalizedText(offer));
            } catch (RuntimeException | AssertionError failure) {
                // Preserve the native offer before dismissing this unconfirmed
                // test purchase. Do not leave StoreKit blocking the next case.
                try {
                    io.qameta.allure.Allure.addAttachment("Unverified trial offer before dismissal", "image/png",
                            new java.io.ByteArrayInputStream(driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES)), "png");
                } catch (RuntimeException artifactFailure) {
                    failure.addSuppressed(artifactFailure);
                }
                try {
                    if (isElementVisible(SANDBOX_ENVIRONMENT) || isElementVisible(TESTFLIGHT_AUTH_NOTICE)) {
                        waitForElementAndClick("id:dismiss", "Cannot dismiss the unconfirmed test-store offer", 5);
                        waitForElementNotVisible(SANDBOX_ENVIRONMENT, "Test-store offer stayed visible", 10);
                    }
                } catch (RuntimeException | AssertionError cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                throw failure;
            }
        });
    }

    @Override
    @Step("Verify explicit premium subscriber status in iOS Profile")
    public void assertPremiumAccessIsDisplayed() {
        waitForElementVisible(PREMIUM_MARKER,
                "Profile does not show the Premium Subscriber status after the test purchase", 30);
    }

    protected Object activeApplication() {
        return ((HasSettings) driver).getSettings().get("defaultActiveApplication");
    }

    protected void activeApplication(Object application) {
        ((HasSettings) driver).setSetting("defaultActiveApplication", application);
    }

    @Override
    @Step("Verify iOS premium access after purchase")
    public void assertPurchaseCompleted() {
        final boolean[] passwordSubmitted = {false};
        final boolean[] storeConfirmed = {false};
        createWait(45).withMessage("iOS purchase did not close the paywall and expose premium content")
                .until(ignored -> {
                    inSystemPurchaseWindow(() -> {
                        if (isElementVisible(STORE_PASSWORD) && isElementVisible("id:Sign In")) {
                            Assert.assertFalse("TestFlight rejected the supplied Apple Account password", passwordSubmitted[0]);
                            Assert.assertTrue("Unexpected store authentication prompt", isElementVisible(TESTFLIGHT_AUTH_NOTICE));
                            Assert.assertTrue("Store authentication requires explicit test-purchase authorization",
                                    Platform.getInstance().isStoreKitSandboxEnabled()
                                            && Platform.getInstance().isStoreKitPurchaseAllowed());
                            String password = System.getenv("COACH_STOREKIT_PASSWORD");
                            Assert.assertTrue("TestFlight requires COACH_STOREKIT_PASSWORD from the test secret store",
                                    password != null && !password.isEmpty());
                            // Do not pass credentials through annotated helpers or Allure step parameters.
                            waitForElementPresent(STORE_PASSWORD, "TestFlight password input disappeared", 5).sendKeys(password);
                            waitForElementAndClick("id:Sign In", "Cannot authorize the test purchase", 5);
                            passwordSubmitted[0] = true;
                            waitForElementNotPresent(STORE_PASSWORD, "TestFlight did not accept the password", 20);
                        }
                        if (isElementVisible(PURCHASE_SUCCESS)) {
                            waitForElementAndClick("id:OK", "Cannot acknowledge successful test purchase", 5);
                            storeConfirmed[0] = true;
                        }
                    });
                    return storeConfirmed[0] || isElementVisible(TODAY_TAB) || isElementVisible(PREMIUM_MARKER);
                });
        // A successful purchase may lead to a conditional workbook upsell before Today.
        iOSOnboardingPageObject onboarding = new iOSOnboardingPageObject(driver);
        onboarding.closePaywallsAndPopups();
        onboarding.waitForToday();
    }

    @Override
    @Step("Open iOS paywall Terms & Privacy")
    public void openTermsAndPrivacy() {
        WebElement disclosure = waitForElementEnabled(
                DISCLOSURE,
                "Paywall Terms & Privacy disclosure is not available",
                15
        );
        disclosure.click();
        try {
            createWait(3).until(webDriver -> termsDestinationIsVisible());
            return;
        } catch (TimeoutException ignored) {
            // The current TestFlight build exposes the two links only as one
            // attributed StaticText. Tap the lower text row relative to that
            // semantic element until app-side accessibility IDs are added.
        }

        Rectangle rectangle = disclosure.getRect();
        Map<String, Object> tap = new HashMap<String, Object>();
        tap.put("x", rectangle.getX() + rectangle.getWidth() / 2);
        tap.put("y", rectangle.getY() + Math.max(1, (rectangle.getHeight() * 4) / 5));
        ((JavascriptExecutor) driver).executeScript("mobile: tap", tap);
    }

    @Override
    @Step("Verify iOS Terms & Privacy destination")
    public void assertTermsAndPrivacyOpened() {
        createWait(20).withMessage("iOS Terms & Privacy destination did not open\n")
                .until(webDriver -> termsDestinationIsVisible());
    }

    @Override
    @Step("Verify exact iOS auto-renewal disclosure")
    public void assertAutoRenewalDisclosure() {
        WebElement disclosure = waitForElementPresent(
                DISCLOSURE,
                "iOS auto-renewal disclosure is missing",
                15
        );
        Assert.assertEquals(
                "Unexpected iOS subscription auto-renewal disclosure",
                EXPECTED_AUTO_RENEWAL_DISCLOSURE,
                normalizedText(disclosure)
        );
    }

    @Override
    @Step("Verify iOS Restore button")
    public void assertRestoreButtonIsAvailable() {
        WebElement restore = waitForElementEnabled(
                RESTORE_BUTTON,
                "iOS Restore button is not visible and enabled",
                15
        );
        Assert.assertEquals("Unexpected iOS Restore label", "RESTORE", normalizedText(restore));
    }

    @Override
    @Step("Tap iOS Restore and verify response")
    public void tapRestoreAndAssertResult() {
        waitForElementAndClick(RESTORE_BUTTON, "Cannot tap iOS Restore", 15);
        createWait(35).withMessage("iOS Restore did not produce restored access or an explicit result\n")
                .until(webDriver -> isElementVisible(TODAY_TAB)
                        || isElementVisible(PREMIUM_MARKER)
                        || !driver.findElements(RESTORE_RESULT).isEmpty());
        java.util.Set<String> observed = new java.util.LinkedHashSet<>();
        for (WebElement result : driver.findElements(RESTORE_RESULT)) {
            if (result.isDisplayed()) observed.add(normalizedText(result));
        }
        if (isElementVisible(PREMIUM_MARKER)) observed.add("Premium Subscriber is visible");
        if (isElementVisible(TODAY_TAB)) observed.add("Today tab is visible");
        String evidence = lib.TestData.sanitizeSensitiveData(String.join("\n", observed))
                .replaceAll("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", "[account]");
        io.qameta.allure.Allure.addAttachment("Observed iOS Restore result", evidence);
    }

    private boolean termsDestinationIsVisible() {
        return !driver.findElements(TERMS_DESTINATION).isEmpty();
    }
}
