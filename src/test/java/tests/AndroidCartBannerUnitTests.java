package tests;

import lib.ui.android.AndroidCartPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.Collections;
import java.util.List;

/** Verifies that a negative banner oracle observes content and propagates driver failures. */
public class AndroidCartBannerUnitTests {
    private static final class Driver extends RemoteWebDriver {
        boolean selectedObserved, loadedObserved, loadingChecked;
        boolean missingContent, driverFailure;
        int bannerPolls;
        final TimeoutException timeout = new TimeoutException("synthetic driver failure");

        @Override public List<WebElement> findElements(By by) {
            String locator = by.toString();
            if (locator.contains("@selected='true'")) selectedObserved = true;
            else if (locator.endsWith(":id/rvQuestions") || locator.endsWith(":id/catalogContainer")) {
                Assert.assertTrue("Observe the selected tab before its content", selectedObserved);
                if (missingContent) throw timeout;
                loadedObserved = true;
            } else if (locator.endsWith(":id/pbLoading")) {
                Assert.assertTrue("Observe loaded content before accepting no spinner", loadedObserved);
                loadingChecked = true;
                return Collections.emptyList();
            } else if (locator.endsWith(":id/abandoned_cart_banner")) {
                Assert.assertTrue("Banner absence was checked before the tab loaded", loadedObserved && loadingChecked);
                bannerPolls++;
                if (driverFailure) throw timeout;
                if (bannerPolls < 2) return Collections.emptyList();
            } else throw new AssertionError("Unexpected offline lookup: " + locator);
            return Collections.singletonList(new RemoteWebElement() {
                @Override public boolean isDisplayed() { return true; }
            });
        }
        @Override public WebElement findElement(By by) {
            List<WebElement> found = findElements(by);
            if (found.isEmpty()) throw new NoSuchElementException("Offline screen has no such control");
            return found.get(0);
        }
    }

    @Test public void delayedBannerFailsOnBothLoadedTabs() {
        for (String tab : new String[]{"daily", "explore"}) {
            Driver driver = new Driver();
            try {
                new AndroidCartPageObject(driver).assertBannerAbsent(tab);
                Assert.fail("A delayed banner was missed");
            } catch (AssertionError actual) {
                Assert.assertEquals("Abandoned-cart banner is visible with an empty cart", actual.getMessage());
            }
            Assert.assertEquals(2, driver.bannerPolls);
        }
    }

    @Test public void missingLoadedContentCannotPassAsBannerAbsence() {
        Driver driver = new Driver(); driver.missingContent = true;
        try {
            new AndroidCartPageObject(driver).assertBannerAbsent("daily");
            Assert.fail("Unloaded content was accepted");
        } catch (TimeoutException actual) { Assert.assertSame(driver.timeout, actual); }
        Assert.assertEquals(0, driver.bannerPolls);
    }

    @Test public void driverTimeoutDuringObservationIsNotSuccessfulAbsence() {
        Driver driver = new Driver(); driver.driverFailure = true;
        try {
            new AndroidCartPageObject(driver).assertBannerAbsent("explore");
            Assert.fail("Driver failure was accepted as no banner");
        } catch (TimeoutException actual) { Assert.assertSame(driver.timeout, actual); }
        Assert.assertEquals(1, driver.bannerPolls);
    }
}
