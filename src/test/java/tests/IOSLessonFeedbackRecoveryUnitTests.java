package tests;

import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;

/** Lesson rating must not poison the next class or dismiss unrelated close controls. */
public class IOSLessonFeedbackRecoveryUnitTests {
    private static class Driver extends RemoteWebDriver { }
    private static class Screen extends iOSCoachFlowPageObject {
        boolean rating, stuck, clickFails, verified;
        int clicks;
        Screen() { super(new Driver()); }
        void recover() { closeLessonFeedbackIfPresent(); }
        boolean ready() { return isAuthorizedDashboardVisible(); }
        @Override public boolean isElementVisible(String locator) {
            if ("id:How would you rate the lesson?".equals(locator)) return rating;
            return "id:ic outline close".equals(locator) || "id:UserProfileImage".equals(locator)
                    || "id:DailyPlanItem".equals(locator);
        }
        @Override public WebElement waitForElementAndClick(String locator, String message, long timeout) {
            Assert.assertEquals("id:ic outline close", locator);
            if (clickFails) throw new WebDriverException("Observed rating close failed");
            clicks++;
            return new RemoteWebElement();
        }
        @Override public void waitForElementNotVisible(String locator, String message, long timeout) {
            Assert.assertEquals("id:How would you rate the lesson?", locator);
            if (stuck) throw new TimeoutException(message);
            rating = false;
            verified = true;
        }
    }

    @Test public void unrelatedCloseControlIsLeftAlone() {
        Screen screen = new Screen();
        screen.recover();
        Assert.assertEquals(0, screen.clicks);
        Assert.assertFalse(screen.verified);
    }

    @Test public void ratingBlocksDashboardUntilClosedAndVerified() {
        Screen screen = new Screen(); screen.rating = true;
        Assert.assertFalse(screen.ready());
        screen.recover();
        Assert.assertEquals(1, screen.clicks);
        Assert.assertTrue(screen.verified);
        Assert.assertTrue(screen.ready());
    }

    @Test(expected = TimeoutException.class) public void ratingThatRemainsOpenStillFailsRecovery() {
        Screen screen = new Screen(); screen.rating = true; screen.stuck = true;
        screen.recover();
    }

    @Test(expected = WebDriverException.class) public void failedCloseIsNotReportedAsRecovery() {
        Screen screen = new Screen(); screen.rating = true; screen.clickFails = true;
        screen.recover();
    }
}
