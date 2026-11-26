package tests;

import lib.EmailTestMailbox;
import lib.ui.ios.IOSEmailReleasePageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Offline checks for release-stage, delivery and restoration failure boundaries. */
public class EmailReleaseRegressionUnitTests {
    private static class Element extends RemoteWebElement {
        @Override public boolean isDisplayed() { return true; }
        @Override public boolean isEnabled() { return true; }
        @Override public String getAttribute(String name) { return "enabled".equals(name) ? "true" : null; }
    }

    private static class StageDriver extends RemoteWebDriver {
        final boolean accessWarning;
        final boolean dismissalWarning;
        StageDriver(boolean accessWarning, boolean dismissalWarning) {
            this.accessWarning = accessWarning;
            this.dismissalWarning = dismissalWarning;
        }
        @Override public List<WebElement> findElements(By by) {
            String locator = by.toString();
            if (locator.contains("Without an email") && !accessWarning
                    || locator.contains("Keep your progress safe") && !dismissalWarning) {
                return Collections.emptyList();
            }
            return Collections.singletonList(new Element());
        }
        @Override public WebElement findElement(By by) { return findElements(by).get(0); }
    }

    private IOSEmailReleasePageObject stage(boolean accessWarning, boolean dismissalWarning) {
        return new IOSEmailReleasePageObject(new StageDriver(accessWarning, dismissalWarning), "en", null);
    }

    @Test public void firstRequestWithoutLaterStageWarningsIsAccepted() {
        stage(false, false).assertPreviouslyDisplayedFirstRequest();
    }
    @Test(expected = AssertionError.class) public void openE2FormCannotSatisfyFirstRequestPrecondition() {
        stage(true, false).assertPreviouslyDisplayedFirstRequest();
    }
    @Test(expected = AssertionError.class) public void dismissalWarningCannotSatisfyFirstRequestPrecondition() {
        stage(false, true).assertPreviouslyDisplayedFirstRequest();
    }

    private boolean receivedAfter(String createdAt, long boundary) throws Exception {
        Map<String, Object> message = new HashMap<>();
        if (createdAt != null) message.put("createdAt", createdAt);
        Method method = EmailTestMailbox.class.getDeclaredMethod("receivedAfterAction", Map.class, long.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(null, message, boundary);
    }
    private long boundary() { return Instant.parse("2026-11-26T12:00:00.100Z").toEpochMilli(); }

    @Test public void deliveryBeforeResendCannotProveAnotherCodeWasSent() throws Exception {
        Assert.assertFalse(receivedAfter("2026-11-26T12:00:00.099Z", boundary()));
    }
    @Test public void deliveryAfterResendIsEligible() throws Exception {
        Assert.assertTrue(receivedAfter("2026-11-26T12:00:01.000Z", boundary()));
    }
    @Test public void deliveryAtTheActionBoundaryIsEligible() throws Exception {
        Assert.assertTrue(receivedAfter("2026-11-26T12:00:00.100Z", boundary()));
    }
    @Test public void missingDeliveryTimestampCannotProveResend() throws Exception {
        Assert.assertFalse(receivedAfter(null, boundary()));
    }
    @Test public void malformedDeliveryTimestampCannotProveResend() throws Exception {
        Assert.assertFalse(receivedAfter("not-a-timestamp", boundary()));
    }
    @Test public void initialDeliveryRetainsTheUntimedMailboxContract() throws Exception {
        Assert.assertTrue(receivedAfter(null, 0));
    }

    private static class Lifecycle extends AndroidEmailReleaseRegression {
        final RuntimeException localeFailure;
        int restorations;
        Lifecycle(RuntimeException quitFailure, RuntimeException localeFailure) {
            this.localeFailure = localeFailure;
            driver = new RemoteWebDriver() {
                @Override public void quit() { if (quitFailure != null) throw quitFailure; }
            };
        }
        @Override protected void cleanupSessionForNextTest() { }
        @Override protected void restoreAppLocale(Throwable failure) {
            restorations++;
            if (localeFailure != null) throw localeFailure;
        }
    }

    @Test public void localeRestorationFailureDoesNotReplaceDriverTeardownFailure() {
        RuntimeException quit = new IllegalStateException("driver quit failed");
        RuntimeException locale = new IllegalStateException("locale restore failed");
        Lifecycle test = new Lifecycle(quit, locale);
        try {
            test.tearDown();
            Assert.fail("Both cleanup failures were ignored");
        } catch (RuntimeException failure) {
            Assert.assertSame(quit, failure);
            Assert.assertArrayEquals(new Throwable[]{locale}, failure.getSuppressed());
        }
        Assert.assertEquals(1, test.restorations);
    }
    @Test public void localeRestorationFailureAloneStillFailsTheTest() {
        RuntimeException locale = new IllegalStateException("locale restore failed");
        Lifecycle test = new Lifecycle(null, locale);
        try {
            test.tearDown();
            Assert.fail("Locale restoration failure was ignored");
        } catch (RuntimeException failure) {
            Assert.assertSame(locale, failure);
        }
        Assert.assertEquals(1, test.restorations);
    }
    @Test public void successfulTeardownRestoresLocaleExactlyOnce() {
        Lifecycle test = new Lifecycle(null, null);
        test.tearDown();
        Assert.assertEquals(1, test.restorations);
    }
}
