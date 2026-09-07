package lib.ui;

import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Cross-platform subscription paywall contract. Store confirmations are
 * deliberately separated from plan selection so a test cannot create a
 * transaction unless the caller has explicitly enabled the test store.
 */
public abstract class SubscriptionPageObject extends MainPageObject {
    public static final String EXPECTED_AUTO_RENEWAL_DISCLOSURE =
            "Subscription renews automatically. Cancel anytime. "
                    + "Privacy Policy & Terms of Service";

    public enum PlanPosition {
        TOP,
        BOTTOM
    }

    public SubscriptionPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Step("Verify subscription paywall is fully loaded")
    public abstract void assertPaywallIsDisplayed();

    @Step("Select {position} subscription plan")
    public abstract void selectPlan(PlanPosition position);

    @Step("Start purchase for selected subscription plan")
    public abstract void startPurchaseAndWaitForStore();

    @Step("Confirm purchase in the platform test store")
    public abstract void confirmTestPurchase();

    @Step("Verify subscription purchase completed")
    public abstract void assertPurchaseCompleted();

    @Step("Open Terms & Privacy from the paywall")
    public abstract void openTermsAndPrivacy();

    @Step("Verify Terms & Privacy destination opened")
    public abstract void assertTermsAndPrivacyOpened();

    @Step("Verify paywall auto-renewal disclosure")
    public abstract void assertAutoRenewalDisclosure();

    @Step("Verify Restore is visible and enabled")
    public abstract void assertRestoreButtonIsAvailable();

    @Step("Tap Restore and verify an explicit result")
    public abstract void tapRestoreAndAssertResult();

    /** Compatibility wrappers for the pre-existing StoreKit suite. */
    public void openSelectedSubscriptionPeriod() {
        selectPlan(PlanPosition.BOTTOM);
    }

    public void startStoreKitPurchase() {
        startPurchaseAndWaitForStore();
    }

    public void confirmStoreKitPurchase() {
        confirmTestPurchase();
    }

    public void restorePurchases() {
        assertRestoreButtonIsAvailable();
        tapRestoreAndAssertResult();
    }

    @Step("Verify paid content is available after test-store outcome")
    public void assertPremiumAccessIsDisplayed() {
        WebElement premiumMarker = waitForFirstElementPresent(
                new String[]{
                        "id:PREMIUM SUBSCRIBER",
                        "id:Today",
                        "id:com.vamapps.thecoach:id/nav_graph_daily"
                },
                "Premium access marker is not displayed after purchase/restore",
                30
        );
        Assert.assertTrue("Premium access marker must be visible", premiumMarker.isDisplayed());
    }

    protected String normalizedText(WebElement element) {
        if (element == null) {
            return "";
        }
        String[] attributes = new String[]{"text", "name", "label", "value", "content-desc"};
        for (String attribute : attributes) {
            try {
                String value = element.getAttribute(attribute);
                if (value != null && !value.trim().isEmpty()) {
                    return normalizeWhitespace(value);
                }
            } catch (Exception ignored) {
                // Attribute availability differs between UiAutomator2/XCUITest.
            }
        }
        try {
            return normalizeWhitespace(element.getText());
        } catch (Exception ignored) {
            return "";
        }
    }

    protected String normalizeWhitespace(String value) {
        return value == null ? "" : value.replace('\u00a0', ' ').trim().replaceAll("\\s+", " ");
    }
}
