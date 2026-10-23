package tests;

import lib.ui.android.AndroidOnboardingPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

public class AndroidOnboardingHintUnitTests {
    private static class Screen extends AndroidOnboardingPageObject {
        boolean visible;
        boolean closed;
        int clicks;
        boolean rejectClick;
        Screen(boolean visible) { super(null); this.visible = visible; }
        boolean dismissHint() { return dismissImmersiveHintIfPresent(); }
        @Override public boolean isElementVisible(String locator) {
            Assert.assertEquals("id:com.android.systemui:id/immersive_cling_title", locator);
            return visible;
        }
        @Override public WebElement waitForElementAndClick(String locator, String message, long timeout) {
            Assert.assertEquals("id:com.android.systemui:id/ok", locator);
            clicks++;
            if (rejectClick) throw new AssertionError("System hint close failed");
            visible = false;
            return null;
        }
        @Override public void waitForElementNotVisible(String locator, String message, long timeout) {
            Assert.assertEquals("id:com.android.systemui:id/immersive_cling_title", locator);
            Assert.assertFalse(visible);
            closed = true;
        }
    }

    @Test public void recognizedSystemHintIsClosedAndItsDisappearanceVerified() {
        Screen screen = new Screen(true);
        Assert.assertTrue(screen.dismissHint());
        Assert.assertEquals(1, screen.clicks);
        Assert.assertTrue(screen.closed);
    }

    @Test public void anUnrelatedOkButtonIsNeverClickedWithoutTheSystemHintTitle() {
        Screen screen = new Screen(false);
        Assert.assertFalse(screen.dismissHint());
        Assert.assertEquals(0, screen.clicks);
        Assert.assertFalse(screen.closed);
    }

    @Test public void systemHintCloseFailureIsNotSwallowed() {
        Screen screen = new Screen(true);
        screen.rejectClick = true;
        try { screen.dismissHint(); Assert.fail("Hint close failure was hidden"); }
        catch (AssertionError expected) { Assert.assertEquals("System hint close failed", expected.getMessage()); }
        Assert.assertFalse(screen.closed);
    }
}
