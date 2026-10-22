package tests;

import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Offline checks: restart is recovery-only and never hides a close failure. */
public class IOSBrowserRecoveryUnitTests {
    private static class Element extends RemoteWebElement {
        boolean clicked;
        Rectangle bounds = new Rectangle(16, 62, 44, 44);
        Element() { setId("native-browser-close"); }
        @Override public Rectangle getRect() { return bounds; }
        @Override public void click() { clicked = true; }
    }

    private static class Driver extends RemoteWebDriver {
        final List<String> commands = new ArrayList<>();
        Map<?, ?> tap;
        boolean tapFails;
        @Override
        public Object executeScript(String script, Object... args) {
            commands.add(script);
            if ("mobile: tap".equals(script)) {
                tap = (Map<?, ?>) args[0];
                if (tapFails) throw new WebDriverException("Native browser tap failed");
                return null;
            }
            Assert.assertEquals(lib.Platform.getInstance().getIOSBundleId(),
                    ((Map<?, ?>) args[0]).get("bundleId"));
            return null;
        }
    }

    private static class Browser extends iOSCoachFlowPageObject {
        final Element close = new Element();
        final Driver fakeDriver;
        boolean checkedGone;
        boolean checkedProfile;
        boolean stuck;
        boolean visible = true;
        boolean restartWorks = true;
        Browser(Driver driver) {
            super(driver);
            fakeDriver = driver;
        }
        void recover() { closeProfileBrowserIfPresent(); }
        @Override public WebElement waitForElementEnabled(String locator, String message, long timeout) {
            Assert.assertEquals(PROFILE_BROWSER_CLOSE_BUTTON, locator);
            return close;
        }
        @Override public boolean isElementVisible(String locator) { return visible; }
        @Override public void waitForElementNotVisible(String locator, String message, long timeout) {
            Assert.assertEquals("id:TopBrowserBar", locator);
            checkedGone = true;
            if (stuck && (!fakeDriver.commands.contains("mobile: activateApp") || !restartWorks))
                throw new TimeoutException(message);
            visible = false;
        }
        @Override public void assertProfileScreenIsDisplayed() { checkedProfile = true; }
    }

    private static class SafariBrowser extends Browser {
        SafariBrowser() { super(new Driver()); }
        @Override public boolean isElementVisible(String locator) {
            return visible && (PROFILE_BROWSER_CLOSE_BUTTON.equals(locator)
                    || "id:URL".equals(locator) || "id:OpenInSafariButton".equals(locator));
        }
        @Override public void waitForElementNotVisible(String locator, String message, long timeout) {
            if ("id:TopBrowserBar".equals(locator)) return;
            Assert.assertEquals(PROFILE_BROWSER_CLOSE_BUTTON, locator);
            checkedGone = true;
            if (stuck && (!fakeDriver.commands.contains("mobile: activateApp") || !restartWorks)) {
                throw new TimeoutException(message);
            }
            visible = false;
        }
    }

    @Test public void safariControllerWithoutTopBrowserBarIsRecovered() {
        SafariBrowser browser = new SafariBrowser();
        browser.recover();
        Assert.assertFalse(browser.visible);
        Assert.assertTrue(browser.checkedGone);
        Assert.assertEquals(java.util.Collections.singletonList("mobile: tap"), browser.fakeDriver.commands);
    }

    @Test(expected = TimeoutException.class)
    public void safariCloseStillFailsWhenOnlyTheCustomBrowserBarIsAbsent() {
        SafariBrowser browser = new SafariBrowser();
        browser.stuck = true;
        browser.closeProfileBrowser();
    }

    @Test public void stuckSafariControllerCanRestartDuringRecovery() {
        SafariBrowser browser = new SafariBrowser();
        browser.stuck = true;
        browser.recover();
        Assert.assertFalse(browser.visible);
        Assert.assertEquals(java.util.Arrays.asList("mobile: tap", "mobile: terminateApp", "mobile: activateApp"),
                browser.fakeDriver.commands);
    }

    @Test public void successfulCloseVerifiesReturnWithoutRestart() {
        Driver driver = new Driver();
        Browser browser = new Browser(driver);
        browser.closeProfileBrowser();
        Assert.assertFalse("Do not use the ineffective semantic click", browser.close.clicked);
        Assert.assertEquals(java.util.Collections.singletonList("mobile: tap"), driver.commands);
        Assert.assertEquals("native-browser-close", driver.tap.get("elementId"));
        Assert.assertEquals(22.0, driver.tap.get("x"));
        Assert.assertEquals(22.0, driver.tap.get("y"));
        Assert.assertTrue(browser.checkedGone);
        Assert.assertTrue(browser.checkedProfile);
    }

    @Test public void stuckBrowserRestartsOnlyDuringRecovery() {
        Driver driver = new Driver();
        Browser browser = new Browser(driver);
        browser.stuck = true;
        browser.recover();
        Assert.assertFalse(browser.close.clicked);
        Assert.assertEquals(java.util.Arrays.asList("mobile: tap", "mobile: terminateApp", "mobile: activateApp"),
                driver.commands);
        Assert.assertFalse(browser.visible);
    }

    @Test public void absentBrowserDoesNotRestartOrClick() {
        Driver driver = new Driver();
        Browser browser = new Browser(driver);
        browser.visible = false;
        browser.recover();
        Assert.assertFalse(browser.close.clicked);
        Assert.assertTrue(driver.commands.isEmpty());
    }

    @Test public void responsiveBrowserDoesNotNeedRecoveryRestart() {
        Driver driver = new Driver();
        Browser browser = new Browser(driver);
        browser.recover();
        Assert.assertFalse(browser.close.clicked);
        Assert.assertFalse(browser.visible);
        Assert.assertEquals(java.util.Collections.singletonList("mobile: tap"), driver.commands);
    }

    @Test public void unsuccessfulDismissalCannotBeReportedAsRecovery() {
        Browser browser = new Browser(new Driver());
        browser.stuck = true;
        try {
            browser.closeProfileBrowser();
            Assert.fail("A browser that remains open must fail recovery");
        } catch (TimeoutException expected) {
            Assert.assertFalse(browser.checkedProfile);
            Assert.assertEquals(java.util.Collections.singletonList("mobile: tap"), browser.fakeDriver.commands);
        }
    }

    @Test(expected = TimeoutException.class)
    public void restartThatDoesNotDismissBrowserStillFailsRecovery() {
        Browser browser = new Browser(new Driver());
        browser.stuck = true;
        browser.restartWorks = false;
        browser.recover();
    }

    @Test public void tapUsesButtonSizeAndNotFixedScreenCoordinates() {
        Driver driver = new Driver();
        Browser browser = new Browser(driver);
        browser.close.bounds = new Rectangle(240, 90, 36, 96);
        browser.closeProfileBrowser();
        Assert.assertEquals(48.0, driver.tap.get("x"));
        Assert.assertEquals(18.0, driver.tap.get("y"));
    }

    @Test public void nativeTapFailureIsNotReportedAsSuccessfulClosure() {
        Driver driver = new Driver();
        driver.tapFails = true;
        Browser browser = new Browser(driver);
        try {
            browser.closeProfileBrowser();
            Assert.fail("A failed native tap must not pass the close scenario");
        } catch (WebDriverException expected) {
            Assert.assertFalse(browser.checkedGone);
            Assert.assertFalse(browser.checkedProfile);
            Assert.assertEquals(java.util.Collections.singletonList("mobile: tap"), driver.commands);
        }
    }

    @Test public void recoveredFaqFailureDoesNotBlockFollowingScenario() throws Throwable {
        Browser browser = new Browser(new Driver());
        browser.stuck = true;
        lib.CoreTestCase first = new lib.CoreTestCase() {
            @Override protected boolean isAllureReportingEnabled() { return false; }
            @Override public void tearDown() { browser.recover(); }
        };
        AssertionError primary = new AssertionError("FAQ content did not load");
        org.junit.runners.model.Statement failingFaq = new org.junit.runners.model.Statement() {
            @Override public void evaluate() { throw primary; }
        };
        String fixture = "tests.OfflineRecoveredBrowserFixture";
        try {
            first.sessionLifecycle.apply(failingFaq,
                    org.junit.runner.Description.createTestDescription(fixture, "faq")).evaluate();
            Assert.fail("The original FAQ failure must remain visible");
        } catch (AssertionError actual) {
            Assert.assertSame(primary, actual);
        }
        final boolean[] executed = {false};
        lib.CoreTestCase next = new lib.CoreTestCase() {
            @Override protected boolean isAllureReportingEnabled() { return false; }
        };
        next.sessionLifecycle.apply(new org.junit.runners.model.Statement() {
            @Override public void evaluate() { executed[0] = true; }
        }, org.junit.runner.Description.createTestDescription(fixture, "terms")).evaluate();
        Assert.assertTrue("Recovery must allow the next scenario to execute", executed[0]);
    }
}
