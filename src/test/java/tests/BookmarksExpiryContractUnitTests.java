package tests;

import lib.ui.ios.iOSBookmarksPageObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Expiry keeps the list accessible, while opening the lesson requires access. */
public class BookmarksExpiryContractUnitTests {
    private static class Page extends iOSBookmarksPageObject {
        final List<String> actions = new ArrayList<>();
        boolean missingCard;
        Page() { super(null); }
        @Override public void openFromToday() { actions.add("list"); }
        @Override public void assertCard(String title) {
            Assert.assertEquals("Saved lesson", title);
            Assert.assertFalse("The expired bookmark disappeared", missingCard);
            actions.add("card");
        }
        @Override public void assertNoPaywall() { actions.add("list-without-paywall"); }
        @Override public void openCardExpectingPaywall(String title) { actions.add("lesson-paywall"); }
        @Override public void openCard(String title) { Assert.fail("Expired access cannot require an unblocked reader"); }
    }
    @Test public void expiryAllowsTheListButRequiresPaywallOnTheSavedLesson() {
        Page page = new Page();
        BookmarksExpiryTests.assertExpiredBookmarkAccess(page, "Saved lesson");
        Assert.assertEquals(Arrays.asList("list", "card", "list-without-paywall", "lesson-paywall"), page.actions);
    }
    @Test(expected = AssertionError.class) public void missingExpiredBookmarkCannotPassAsAPaywallCheck() {
        Page page = new Page();
        page.missingCard = true;
        BookmarksExpiryTests.assertExpiredBookmarkAccess(page, "Saved lesson");
    }
}
