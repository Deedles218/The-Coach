package lib.ui.ios;

import io.qameta.allure.Step;
import lib.Platform;
import org.junit.Assert;
import org.openqa.selenium.remote.RemoteWebDriver;

/** Store outcome only: preserve Recommendation instead of advancing to Today. */
public final class IOSProgramPurchasePageObject extends iOSSubscriptionPageObject {
    private static final String NO_CHARGE = "xpath://XCUIElementTypeStaticText[@visible='true' and ("
            + "@name='For testing purposes only. You will not be charged for confirming this purchase.'"
            + " or @name='Только для тестирования. При подтверждении этой покупки средства списаны не будут.'"
            + " or contains(@name,'Environment: Sandbox'))]";
    private static final String AUTH_NOTICE = "xpath://XCUIElementTypeStaticText[@visible='true' and ("
            + "contains(@name,'Beta testers are not charged for this In-App purchase')"
            + " or contains(@name,'Ее стоимость не будет удерживаться с тестировщиков бета-версии.'))]";
    private static final String PASSWORD = "xpath://XCUIElementTypeSecureTextField[@visible='true']";
    private static final String SUCCESS = "xpath://XCUIElementTypeStaticText[@visible='true' and ("
            + "@name='Your purchase was successful.' or @name='Покупка успешно завершена.')]";

    public IOSProgramPurchasePageObject(RemoteWebDriver driver) { super(driver); }

    private void inStore(Runnable action) {
        Object previous = activeApplication();
        try { activeApplication("com.apple.springboard"); action.run(); }
        finally { activeApplication(previous == null ? "auto" : previous); }
    }

    @Override @Step("Open the explicitly identified test-store purchase sheet")
    public void startPurchaseAndWaitForStore() {
        waitForElementAndClick("id:CONTINUE", "Cannot start the prepared paywall purchase", 15);
        inStore(() -> waitForElementVisible(NO_CHARGE, "The cancellable test-store purchase sheet did not open", 30));
    }

    @Step("Cancel the explicitly identified test-store purchase")
    public void cancelTestPurchase() {
        inStore(() -> {
            waitForElementVisible(NO_CHARGE, "Native purchase does not identify the test store", 15);
            waitForElementAndClick("id:dismiss", "Test-store cancel is unavailable", 10);
            waitForElementNotVisible(NO_CHARGE, "Test-store sheet remained after cancellation", 15);
        });
    }

    @Override @Step("Confirm the explicitly identified test-store purchase")
    public void confirmTestPurchase() {
        Assert.assertTrue("A test transaction requires the existing explicit purchase flags",
                Platform.getInstance().isStoreKitSandboxEnabled() && Platform.getInstance().isTestPurchaseAllowed());
        inStore(() -> {
            waitForElementVisible(NO_CHARGE, "Native purchase does not identify the test store", 15);
            waitForFirstElementAndClick(new String[]{"id:footer", "id:Subscribe", "id:Buy", "id:Confirm"},
                    "Test-store confirmation is unavailable", 15);
        });
    }

    @Step("Wait for successful test purchase and stop at Recommendation")
    public void waitForRecommendationAfterSuccess(String purchasedPaywall) {
        boolean[] passwordSubmitted = {false};
        boolean[] storeConfirmed = {false};
        createWait(120).withMessage("A successful test purchase did not open Recommendation before Today").until(ignored -> {
            inStore(() -> {
                if (isElementVisible(PASSWORD)) {
                    Assert.assertFalse("The test store rejected its supplied credential", passwordSubmitted[0]);
                    Assert.assertTrue("Unexpected purchase authentication prompt", isElementVisible(AUTH_NOTICE));
                    String password = System.getenv("COACH_STOREKIT_PASSWORD");
                    Assert.assertTrue("Supply store authentication through the approved secret runner",
                            password != null && !password.isEmpty());
                    // No annotated step receives credentials, and no source is captured here.
                    try {
                        waitForElementPresent(PASSWORD, "Store credential field disappeared", 5).sendKeys(password);
                    } catch (RuntimeException credentialFailure) {
                        // Native sendKeys exceptions can echo their input. Keep
                        // the stage/type without retaining that unsafe cause.
                        throw new IllegalStateException("Native test-store credential entry failed: "
                                + credentialFailure.getClass().getSimpleName());
                    }
                    new iOSCoachFlowPageObject(driver).tapHittableControl(new String[]{"id:Sign In", "id:Войти"},
                            "Cannot authorize the test-store transaction", 5);
                    passwordSubmitted[0] = true;
                    waitForElementNotVisible(PASSWORD, "Test-store authentication did not finish", 20);
                }
                if (isElementVisible(SUCCESS)) {
                    waitForElementAndClick("id:OK", "Cannot close test-store success", 5);
                    storeConfirmed[0] = true;
                }
            });
            String recommendation = "id:" + IOSProgramSelectionPageObject.RECOMMENDATION_TITLE;
            if (isElementVisible(recommendation)) return true;
            Assert.assertFalse("Purchase skipped Recommendation and opened Today", isElementVisible("id:Today"));
            // StoreKit and receipt processing can leave the purchased paywall
            // visible under a spinner. Closing it is not a successful purchase.
            if (!storeConfirmed[0] || isElementVisible(purchasedPaywall)) return false;
            new iOSOnboardingPageObject(driver).closePaywallsWithoutConsumingSlides(new String[]{
                    IOSProgramSelectionPageObject.RECOMMENDATION_TITLE, "Your plan is ready", "Today"});
            Assert.assertFalse("Purchase skipped Recommendation and opened Summary", isElementVisible("id:Your plan is ready"));
            return isElementVisible(recommendation);
        });
    }
}
