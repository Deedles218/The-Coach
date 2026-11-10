package tests;

import lib.ui.DailyPlanPageObject;
import org.junit.Assert;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** An unlocked day must never be reported as a successful lock check. */
public class DailyPlanLockedDayUnitTests {
    private static class Driver extends RemoteWebDriver { }
    private static class Screen extends DailyPlanPageObject {
        boolean tapped, popup, changesOnClose, dashboardChecked;
        int changesAfterReads, reads, closed;
        String day = "Day 1 of 60";
        Screen() { super(new Driver()); }
        @Override public String getCurrentDayLabel() {
            if (tapped && changesAfterReads > 0 && ++reads >= changesAfterReads) day = "Day 2 of 60";
            return day;
        }
        @Override public WebElement waitForFirstElementAndClick(String[] locators, String message, long timeout) {
            tapped = true;
            return new RemoteWebElement();
        }
        @Override public boolean isLockedNextDayPopupDisplayed() { return popup; }
        @Override public void closeLockedNextDayPopup() {
            closed++;
            popup = false;
            if (changesOnClose) day = "Day 2 of 60";
        }
        @Override public void assertDailyPlanDaySwitcherIsDisplayed() { dashboardChecked = true; }
        @Override public WebDriverWait createWait(long seconds) {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(25));
            wait.pollingEvery(Duration.ofMillis(1));
            return wait;
        }
    }

    @Test(expected = ComparisonFailure.class)
    public void navigationWithoutPopupFailsTheLockCheck() {
        Screen screen = new Screen();
        screen.changesAfterReads = 1;
        screen.assertRightArrowDoesNotChangeCurrentDayWhenLocked();
    }

    @Test(expected = ComparisonFailure.class)
    public void delayedNavigationWithoutPopupAlsoFails() {
        Screen screen = new Screen();
        screen.changesAfterReads = 3;
        screen.assertRightArrowDoesNotChangeCurrentDayWhenLocked();
    }

    @Test public void unchangedDayWithoutPopupStillRequiresTheDashboard() {
        Screen screen = new Screen();
        screen.assertRightArrowDoesNotChangeCurrentDayWhenLocked();
        Assert.assertTrue(screen.tapped);
        Assert.assertTrue(screen.dashboardChecked);
        Assert.assertEquals(0, screen.closed);
    }

    @Test public void blockingPopupIsClosedAndDayIsPreserved() {
        Screen screen = new Screen();
        screen.popup = true;
        screen.assertRightArrowDoesNotChangeCurrentDayWhenLocked();
        Assert.assertEquals(1, screen.closed);
        Assert.assertEquals("Day 1 of 60", screen.day);
    }

    @Test(expected = ComparisonFailure.class)
    public void closingPopupCannotHideNavigation() {
        Screen screen = new Screen();
        screen.popup = true;
        screen.changesOnClose = true;
        screen.assertRightArrowDoesNotChangeCurrentDayWhenLocked();
    }
}
