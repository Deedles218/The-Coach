package lib.ui;

import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * App-owned subscription paywall actions. The StoreKit suite is deliberately
 * isolated from the regular smoke suite because its state is external to the
 * app and may create a sandbox transaction.
 */
abstract public class SubscriptionPageObject extends MainPageObject {
    protected static String
            PAYWALL_MARKER,
            PAYWALL_MARKER_FALLBACK,
            START_FREE_TRIAL_BUTTON,
            START_FREE_TRIAL_BUTTON_FALLBACK,
            SELECTED_PLAN_BUTTON,
            RESTORE_PURCHASES_BUTTON,
            RESTORE_PURCHASES_BUTTON_FALLBACK,
            PREMIUM_ACCESS_MARKER,
            RESTORE_SUCCESS_MARKER,
            RESTORE_SUCCESS_MARKER_FALLBACK,
            RESTORE_RESULT_MARKER,
            STOREKIT_CONFIRM_BUTTON;

    public SubscriptionPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Step("Verify subscription paywall is displayed")
    public void assertPaywallIsDisplayed() {
        this.waitForFirstElementPresent(
                new String[]{PAYWALL_MARKER, PAYWALL_MARKER_FALLBACK},
                "Subscription paywall is not displayed",
                20
        );
        this.waitForFirstElementPresent(
                new String[]{START_FREE_TRIAL_BUTTON, START_FREE_TRIAL_BUTTON_FALLBACK},
                "Start free trial button is not displayed on subscription paywall",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{RESTORE_PURCHASES_BUTTON, RESTORE_PURCHASES_BUTTON_FALLBACK},
                "Restore Purchases action is not displayed on subscription paywall",
                10
        );
    }

    @Step("Open the selected subscription period from Start free trial")
    public void openSelectedSubscriptionPeriod() {
        this.waitForFirstElementAndClick(
                new String[]{START_FREE_TRIAL_BUTTON, START_FREE_TRIAL_BUTTON_FALLBACK},
                "Cannot tap Start free trial on subscription paywall",
                10
        );
        this.waitForElementPresent(
                SELECTED_PLAN_BUTTON,
                "Selected subscription period is not displayed after tapping Start free trial",
                15
        );
    }

    @Step("Start StoreKit purchase for the selected period")
    public void startStoreKitPurchase() {
        this.waitForElementAndClick(
                SELECTED_PLAN_BUTTON,
                "Cannot tap the selected subscription period",
                10
        );
        this.waitForElementPresent(
                STOREKIT_CONFIRM_BUTTON,
                "StoreKit purchase confirmation did not appear",
                20
        );
    }

    @Step("Confirm StoreKit sandbox purchase")
    public void confirmStoreKitPurchase() {
        this.waitForElementAndClick(
                STOREKIT_CONFIRM_BUTTON,
                "Cannot confirm StoreKit sandbox purchase",
                10
        );
    }

    @Step("Restore purchases in StoreKit sandbox")
    public void restorePurchases() {
        this.waitForFirstElementAndClick(
                new String[]{RESTORE_PURCHASES_BUTTON, RESTORE_PURCHASES_BUTTON_FALLBACK},
                "Cannot tap Restore Purchases on subscription paywall",
                10
        );
        this.waitForFirstElementPresent(
                new String[]{PREMIUM_ACCESS_MARKER, RESTORE_SUCCESS_MARKER, RESTORE_SUCCESS_MARKER_FALLBACK, RESTORE_RESULT_MARKER},
                "Restore Purchases did not produce a success or an explicit StoreKit result",
                30
        );
    }

    @Step("Verify paid content is available after StoreKit outcome")
    public void assertPremiumAccessIsDisplayed() {
        WebElement premiumMarker = this.waitForElementPresent(
                PREMIUM_ACCESS_MARKER,
                "Premium access marker is not displayed after StoreKit purchase/restore",
                20
        );
        Assert.assertTrue("Premium access marker must be visible", premiumMarker.isDisplayed());
    }
}
