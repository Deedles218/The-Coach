package tests;

import lib.ui.android.AndroidOnboardingPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

public class AndroidOnboardingTransitionUnitTests {
    private static class Screen extends AndroidOnboardingPageObject {
        final List<String> actions = new ArrayList<>();
        String screen = "transition";
        boolean stuck;
        Screen() { super(null); }
        @Override public boolean isElementVisible(String locator) {
            return screen.equals("slides") && locator.contains("viewPager");
        }
        @Override public boolean isElementPresent(String locator) {
            return (screen.equals("today") && locator.endsWith("/nav_graph_daily"))
                    || (screen.equals("intro") && locator.endsWith("/tvHeader"));
        }
        @Override protected WebElement firstDisplayedEnabled(By locator) {
            return screen.equals("intro") && locator.toString().endsWith("/btnGotIt")
                    ? new org.openqa.selenium.remote.RemoteWebElement() : null;
        }
        @Override protected void clickAndWaitForStateChange(WebElement element, long timeout) {
            Assert.assertEquals("intro", screen); actions.add("intro"); screen = "today";
        }
        @Override protected void waitForNextOnboardingActionOrDestination(BooleanSupplier destinationVisible) {
            actions.add("wait");
            if (stuck) throw new TimeoutException("No next onboarding action");
            screen = "intro";
        }
        @Override public WebElement waitForElementVisible(String locator, String message, long timeout) {
            Assert.assertTrue(isElementVisible(locator)); return null;
        }
    }
    @Test public void delayedIntroductionIsHandledInsteadOfReturningBeforeItsButtonAppears() {
        Screen screen = new Screen();
        Assert.assertEquals(0, screen.closePaywallsAndPopups());
        Assert.assertEquals(Arrays.asList("wait", "intro"), screen.actions);
        Assert.assertEquals("today", screen.screen);
    }
    @Test public void readyTodayDoesNotWaitOrTapAnything() {
        Screen screen = new Screen(); screen.screen = "today";
        Assert.assertEquals(0, screen.closePaywallsAndPopups());
        Assert.assertTrue(screen.actions.isEmpty());
    }
    @Test public void anUnresolvedTransitionFailsRatherThanPretendingItWasHandled() {
        Screen screen = new Screen(); screen.stuck = true;
        try { screen.closePaywallsAndPopups(); Assert.fail("Stuck transition was accepted"); }
        catch (TimeoutException expected) { Assert.assertTrue(expected.getMessage().contains("No next")); }
        Assert.assertEquals(Arrays.asList("wait"), screen.actions);
    }
    @Test public void productSlidesRemainADestinationAndAreNotConsumed() {
        Screen screen = new Screen(); screen.screen = "slides";
        Assert.assertEquals(0, screen.closePaywallsBeforeSlides("Expected header"));
        Assert.assertTrue(screen.actions.isEmpty());
    }
}
