package tests;

import lib.ui.android.AndroidOnboardingPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

public class AndroidOnboardingNotificationUnitTests {
    private static class OfflineDriver extends RemoteWebDriver { OfflineDriver() { super(); } }
    private static class Screen extends AndroidOnboardingPageObject {
        final boolean samsung;
        boolean prompt = true, welcome;
        int clicks;
        String message = "Allow The Coach to send you notifications?";
        final WebElement deny = new RemoteWebElement();
        Screen(boolean samsung) { super(new OfflineDriver()); this.samsung = samsung; }
        boolean dismiss() { return dismissOptionalNotificationPermissionIfPresent(); }
        @Override protected WebElement firstDisplayedEnabled(By locator) {
            if (!prompt) return null;
            String id = locator.toString();
            if (id.endsWith("/permission_message")) return new RemoteWebElement() {
                @Override public String getText() { return message; }
            };
            return id.endsWith(samsung ? "/permission_deny_and_dont_ask_again_button" : "/permission_deny_button") ? deny : null;
        }
        @Override protected void clickAndWaitForStateChange(WebElement element, long timeout) {
            Assert.assertSame(deny, element); clicks++; prompt = false;
        }
        @Override public boolean isElementVisible(String locator) {
            Assert.assertTrue(locator.endsWith("/btnSignInAnonymous")); return welcome;
        }
    }
    @Test public void samsungNotificationPromptIsDismissedBeforeWelcome() {
        Screen screen = new Screen(true); Assert.assertTrue(screen.dismiss());
        Assert.assertEquals(1, screen.clicks); Assert.assertFalse(screen.prompt);
    }
    @Test public void standardAndroidDenyIsAlsoSupported() {
        Screen screen = new Screen(false); Assert.assertTrue(screen.dismiss()); Assert.assertEquals(1, screen.clicks);
    }
    @Test public void welcomeBehindTheSystemWindowDoesNotCauseThePromptToBeIgnored() {
        Screen screen = new Screen(true); screen.welcome = true;
        screen.assertFreshStartIsDisplayed(); Assert.assertEquals(1, screen.clicks);
    }
    @Test public void cleanWelcomeDoesNotClickAnUnrelatedControl() {
        Screen screen = new Screen(true); screen.prompt = false; screen.welcome = true;
        screen.assertFreshStartIsDisplayed(); Assert.assertEquals(0, screen.clicks);
    }
    @Test public void unexpectedCameraPermissionIsNotDismissedAsNotifications() {
        Screen screen = new Screen(true); screen.message = "Allow access to your camera?";
        try { screen.dismiss(); Assert.fail("Unexpected permission was accepted"); }
        catch (AssertionError expected) { Assert.assertTrue(expected.getMessage().contains("Unexpected Android permission")); }
        Assert.assertEquals(0, screen.clicks);
    }
}
