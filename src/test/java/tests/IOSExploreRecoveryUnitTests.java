package tests;

import lib.ui.ios.iOSDailyPlanPageObject;
import lib.ui.ios.iOSExplorePageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The article reader also says Explore; only its selected tab identifies the catalog. */
public class IOSExploreRecoveryUnitTests {
    private static class Destination extends iOSExplorePageObject {
        final List<String> actions = new ArrayList<>();
        final List<String> verified = new ArrayList<>();
        boolean heading = true;
        boolean catalog;
        boolean confirmation;
        boolean stuck;
        boolean headingOnReturn = true;
        boolean browser;
        String quit;
        String close = "id:ic outline close";
        TimeoutException closeFailure;

        Destination() { super(new RemoteWebDriver() { }); }

        @Override public boolean isElementVisible(String locator) {
            if ("id:TopBrowserBar".equals(locator)) return browser;
            if (SELECTED_EXPLORE_TAB.equals(locator)) return catalog;
            if (locator.contains("XCUIElementTypeStaticText[@name='Explore'")) return heading;
            if ("id:QUIT".equals(locator)) return confirmation && "id:QUIT".equals(quit);
            if (iOSDailyPlanPageObject.lessonExitConfirmationLocator().equals(locator))
                return confirmation && "id:Quit".equals(quit);
            return close != null && close.equals(locator);
        }

        @Override public WebElement waitForFirstElementAndClick(String[] locators, String message, long timeout) {
            Assert.assertTrue("Use the observed destination close only", Arrays.asList(locators).contains(close));
            if (closeFailure != null) throw closeFailure;
            actions.add(close);
            if (quit != null) confirmation = true;
            else returnToCatalog();
            return null;
        }

        @Override public WebElement waitForElementAndClick(String locator, String message, long timeout) {
            Assert.assertEquals("Only confirm unfinished exit", quit, locator);
            actions.add(locator);
            confirmation = false;
            returnToCatalog();
            return null;
        }

        private void returnToCatalog() {
            if (!stuck) { catalog = true; heading = headingOnReturn; }
        }

        @Override protected void closeExploreBrowser() {
            if (closeFailure != null) throw closeFailure;
            actions.add("browser-close");
            if (!stuck) browser = false;
            returnToCatalog();
        }

        @Override public void waitForElementNotVisible(String locator, String message, long timeout) {
            if (isElementVisible(locator)) throw new TimeoutException(message);
        }

        @Override public WebElement waitForElementVisible(String locator, String message, long timeout) {
            if (!isElementVisible(locator)) throw new TimeoutException(message);
            verified.add(SELECTED_EXPLORE_TAB.equals(locator) ? "selected-tab" : "heading");
            return null;
        }

        @Override public WebDriverWait createWait(long timeout) {
            return new WebDriverWait(driver, Duration.ZERO);
        }
    }

    @Test public void selectedCatalogDoesNotCloseUnrelatedControls() {
        Destination page = new Destination();
        page.catalog = true;
        page.closeTransientDetailIfPresent();
        Assert.assertTrue(page.actions.isEmpty());
    }

    @Test public void exploreHeadingAloneCannotHideOpenArticle() {
        Destination page = new Destination();
        page.closeTransientDetailIfPresent();
        Assert.assertEquals(Arrays.asList("id:ic outline close"), page.actions);
        Assert.assertEquals(Arrays.asList("selected-tab", "heading"), page.verified);
        Assert.assertTrue(page.catalog);
    }

    @Test public void legacyDetailCloseStillVerifiesTheSelectedCatalog() {
        Destination page = new Destination();
        page.heading = false;
        page.close = "id:CloseRoundBlack";
        page.closeTransientDetailIfPresent();
        Assert.assertEquals(Arrays.asList("id:CloseRoundBlack"), page.actions);
        Assert.assertEquals(Arrays.asList("selected-tab", "heading"), page.verified);
    }

    @Test public void articleExitConfirmsTheObservedMixedCaseQuit() {
        Destination page = new Destination();
        page.quit = "id:Quit";
        page.closeTransientDetailIfPresent();
        Assert.assertEquals(Arrays.asList("id:ic outline close", "id:Quit"), page.actions);
        Assert.assertTrue(page.catalog);
    }

    @Test public void legacyUpperCaseQuitStillWorks() {
        Destination page = new Destination();
        page.heading = false;
        page.close = "id:navBarRoundClose";
        page.quit = "id:QUIT";
        page.closeTransientDetailIfPresent();
        Assert.assertEquals(Arrays.asList("id:navBarRoundClose", "id:QUIT"), page.actions);
        Assert.assertTrue(page.catalog);
    }

    @Test public void originalCloseFailureIsPreserved() {
        Destination page = new Destination();
        page.closeFailure = new TimeoutException("Injected destination close failure");
        try {
            page.closeTransientDetailIfPresent();
            Assert.fail("Unclosed destination must fail");
        } catch (TimeoutException failure) { Assert.assertSame(page.closeFailure, failure); }
    }

    @Test(expected = TimeoutException.class) public void survivingArticleCannotPassOnItsExploreHeading() {
        Destination page = new Destination();
        page.stuck = true;
        page.closeTransientDetailIfPresent();
    }

    @Test public void unknownDestinationDoesNotTriggerAnArbitraryTap() {
        Destination page = new Destination();
        page.heading = false;
        page.close = null;
        page.closeTransientDetailIfPresent();
        Assert.assertTrue(page.actions.isEmpty());
        Assert.assertFalse(page.catalog);
    }

    @Test(expected = TimeoutException.class) public void selectedTabWithoutCatalogHeadingIsNotSuccessfulReturn() {
        Destination page = new Destination();
        page.headingOnReturn = false;
        page.closeTransientDetailIfPresent();
    }

    @Test public void coachingBusinessCloseReturnsToTheSelectedCatalog() {
        Destination page = new Destination();
        page.browser = true;
        page.closePrivateCoachingWebView();
        Assert.assertEquals(Arrays.asList("browser-close"), page.actions);
        Assert.assertEquals(Arrays.asList("selected-tab", "heading"), page.verified);
    }

    @Test public void failedBusinessBrowserCloseIsNotReplacedWithRecovery() {
        Destination page = new Destination();
        page.browser = true;
        page.closeFailure = new TimeoutException("Injected browser close failure");
        try {
            page.closePrivateCoachingWebView();
            Assert.fail("A recovery restart must not make the business close pass");
        } catch (TimeoutException failure) { Assert.assertSame(page.closeFailure, failure); }
        Assert.assertTrue(page.browser);
        Assert.assertTrue(page.actions.isEmpty());
    }

    @Test(expected = TimeoutException.class) public void survivingBrowserFailsBusinessClose() {
        Destination page = new Destination();
        page.browser = true;
        page.stuck = true;
        page.closePrivateCoachingWebView();
    }
}
