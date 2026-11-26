package tests;

import lib.ui.android.AndroidProgramSelectionPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import java.util.*;

/** A visible card root must not be mistaken for its clipped native action. */
public class AndroidProgramSelectionControlsUnitTests {
    private static class Screen extends AndroidProgramSelectionPageObject {
        int page = 1, actionPage = 2;
        boolean missing;
        String requested;
        final List<String> gestures = new ArrayList<>();
        final WebElement action = new RemoteWebElement();
        Screen() { super(null); }
        WebElement reveal(String name) { return revealControl("Last Longer", name); }
        @Override public boolean isElementVisible(String locator) {
            requested = locator;
            return !missing && page == actionPage;
        }
        @Override public WebElement waitForElementEnabled(String locator, String message, long timeout) {
            if (!isElementVisible(locator)) throw new TimeoutException(message);
            return action;
        }
        @Override protected boolean scroll(String direction) {
            gestures.add(direction);
            if ("up".equals(direction)) { page = 0; return false; }
            page = Math.min(2, page + 1); return page < 2;
        }
    }
    @Test public void clippedLearnMoreIsFoundEvenWhenTheLastScrollReachesTheEnd() {
        Screen screen = new Screen();
        Assert.assertSame(screen.action, screen.reveal("tvLearnMore"));
        Assert.assertEquals(Arrays.asList("up", "down", "down"), screen.gestures);
        Assert.assertTrue(screen.requested.contains("Last Longer"));
        Assert.assertTrue(screen.requested.endsWith("tvLearnMore']"));
    }
    @Test public void checkmarkAboveTheViewportIsRevealedByRewinding() {
        Screen screen = new Screen(); screen.actionPage = 0;
        Assert.assertSame(screen.action, screen.reveal("ivSelection"));
        Assert.assertEquals(Collections.singletonList("up"), screen.gestures);
    }
    @Test public void anAlreadyVisibleActionNeedsNoGesture() {
        Screen screen = new Screen(); screen.actionPage = 1;
        Assert.assertSame(screen.action, screen.reveal("tvLearnMore"));
        Assert.assertTrue(screen.gestures.isEmpty());
    }
    @Test public void absentActionFailsInsteadOfClickingAnUnrelatedCard() {
        Screen screen = new Screen(); screen.missing = true;
        try { screen.reveal("tvLearnMore"); Assert.fail("Missing Learn More was accepted"); }
        catch (TimeoutException expected) { Assert.assertTrue(expected.getMessage().contains("Last Longer/tvLearnMore")); }
    }
}
