package tests;

import lib.ui.ios.iOSSubscriptionPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/** A production or unidentified StoreKit sheet must never reach confirmation. */
public class IOSSubscriptionSafetyUnitTests {
    private static class Purchase extends iOSSubscriptionPageObject {
        final boolean sandboxVisible;
        boolean confirmationClicked;
        Object activeApplication = "auto";
        Purchase(boolean sandboxVisible) {
            super(new RemoteWebDriver() { });
            this.sandboxVisible = sandboxVisible;
        }
        @Override public WebElement waitForElementPresent(String locator, String message, long timeout) {
            Assert.assertTrue("The guard must check the native environment", locator.contains("Environment: Sandbox"));
            Assert.assertTrue(locator.contains("You will not be charged"));
            Assert.assertEquals("com.apple.springboard", activeApplication);
            if (!sandboxVisible) throw new TimeoutException("Native Sandbox marker is absent");
            return null;
        }
        @Override public WebElement waitForFirstElementAndClick(String[] locators, String message, long timeout) {
            Assert.assertEquals("com.apple.springboard", activeApplication);
            confirmationClicked = true;
            return null;
        }
        @Override protected Object activeApplication() { return activeApplication; }
        @Override protected void activeApplication(Object application) { activeApplication = application; }
    }

    @Test public void enabledFlagsCannotConfirmAnUnidentifiedStoreSheet() {
        withFlags(true, () -> {
            Purchase purchase = new Purchase(false);
            try {
                purchase.confirmTestPurchase();
                Assert.fail("A sheet without native Sandbox identification was accepted");
            } catch (TimeoutException expected) {
                Assert.assertFalse(purchase.confirmationClicked);
                Assert.assertEquals("auto", purchase.activeApplication);
            }
        });
    }

    @Test public void sandboxSheetStillRequiresExplicitPurchaseAuthorization() {
        withFlags(false, () -> {
            Purchase purchase = new Purchase(true);
            try {
                purchase.confirmTestPurchase();
                Assert.fail("A Sandbox sheet bypassed the explicit opt-in");
            } catch (AssertionError expected) {
                Assert.assertFalse(purchase.confirmationClicked);
            }
        });
    }

    @Test public void authorizedSandboxSheetCanBeConfirmed() {
        withFlags(true, () -> {
            Purchase purchase = new Purchase(true);
            purchase.confirmTestPurchase();
            Assert.assertTrue(purchase.confirmationClicked);
            Assert.assertEquals("auto", purchase.activeApplication);
        });
    }

    private static final class Trial extends iOSSubscriptionPageObject {
        final boolean trial;
        boolean dismissed;
        Object application = "auto";
        Trial(boolean trial) {
            super(new RemoteWebDriver() {
                @Override public <X> X getScreenshotAs(org.openqa.selenium.OutputType<X> type) {
                    throw new UnsupportedOperationException("Diagnostic screenshot unavailable");
                }
            });
            this.trial = trial;
        }
        @Override public WebElement waitForElementVisible(String locator, String message, long timeout) {
            Assert.assertEquals("com.apple.springboard", application);
            Assert.assertTrue("The offer must be native trial terms", locator.contains("free trial"));
            if (!trial) throw new TimeoutException("Selected product has no free trial");
            return null;
        }
        @Override protected String normalizedText(WebElement ignored) { return "1 week free"; }
        @Override public boolean isElementVisible(String locator) { return locator.contains("Environment: Sandbox"); }
        @Override public WebElement waitForElementAndClick(String locator, String message, long timeout) {
            Assert.assertEquals("Only the unconfirmed test sheet may be dismissed", "id:dismiss", locator);
            dismissed = true; return null;
        }
        @Override public void waitForElementNotVisible(String locator, String message, long timeout) { }
        @Override protected Object activeApplication() { return application; }
        @Override protected void activeApplication(Object value) { application = value; }
    }

    @Test public void regularSandboxPurchaseCannotPassAsFreeTrial() {
        Trial offer = new Trial(false);
        try {
            offer.assertFreeTrialIsOffered();
            Assert.fail("Regular purchase was accepted as a trial");
        } catch (TimeoutException expected) {
            Assert.assertTrue(expected.getMessage().contains("no free trial"));
            Assert.assertTrue("Unconfirmed test purchase must be dismissed", offer.dismissed);
            Assert.assertEquals("auto", offer.application);
            Assert.assertEquals("Diagnostic failure must not replace the trial failure", 1, expected.getSuppressed().length);
        }
    }

    @Test public void explicitNativeTrialOfferDoesNotDismissOrConfirmPurchase() {
        Trial offer = new Trial(true);
        offer.assertFreeTrialIsOffered();
        Assert.assertFalse(offer.dismissed);
        Assert.assertEquals("auto", offer.application);
    }

    private void withFlags(boolean authorized, Runnable action) {
        String sandbox = System.getProperty("storekit.sandbox");
        String purchases = System.getProperty("storekit.allowPurchases");
        try {
            System.setProperty("storekit.sandbox", "true");
            System.setProperty("storekit.allowPurchases", Boolean.toString(authorized));
            action.run();
        } finally {
            restore("storekit.sandbox", sandbox);
            restore("storekit.allowPurchases", purchases);
        }
    }
    private void restore(String name, String value) {
        if (value == null) System.clearProperty(name); else System.setProperty(name, value);
    }
}
