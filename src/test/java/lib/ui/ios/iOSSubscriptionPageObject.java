package lib.ui.ios;

import io.qameta.allure.Step;
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
            "xpath://XCUIElementTypeStaticText[contains(@name,\"THE COACH'S HERE TO HELP\")]";
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

    private static final String[] PURCHASE_SHEET_MARKERS = new String[]{
            "id:Subscribe",
            "id:Buy",
            "xpath://XCUIElementTypeSheet[@visible='true']",
            "xpath://XCUIElementTypeStaticText[@visible='true' and ("
                    + "contains(@name,'Confirm Your Subscription') "
                    + "or contains(@name,'Double Click to Subscribe') "
                    + "or contains(@name,'Environment: Sandbox'))]"
    };
    private static final String[] PURCHASE_CONFIRM_BUTTONS = new String[]{
            "id:Subscribe",
            "id:Buy",
            "id:Confirm"
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
        waitForFirstElementPresent(
                PURCHASE_SHEET_MARKERS,
                "iOS StoreKit purchase sheet did not open",
                30
        );
    }

    @Override
    @Step("Confirm iOS StoreKit sandbox purchase")
    public void confirmTestPurchase() {
        waitForFirstElementAndClick(
                PURCHASE_CONFIRM_BUTTONS,
                "Cannot confirm StoreKit sandbox purchase. A real-device side-button or biometric confirmation may require manual test-lab support.",
                20
        );
    }

    @Override
    @Step("Verify iOS premium access after purchase")
    public void assertPurchaseCompleted() {
        waitForFirstElementPresent(
                new String[]{TODAY_TAB, PREMIUM_MARKER},
                "iOS purchase did not close the paywall and expose premium content",
                45
        );
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
    }

    private boolean termsDestinationIsVisible() {
        return !driver.findElements(TERMS_DESTINATION).isEmpty();
    }
}
