package lib.ui.android;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import lib.AppearanceSnapshot;
import lib.DailyPlanCompletionMarker;
import lib.Platform;
import lib.ui.BookmarksPageObject;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Native Android contract from the local Bookmarks worktree; no iOS selectors are replaced. */
public class AndroidBookmarksPageObject extends BookmarksPageObject {
    private static final String P = Platform.getInstance().getAndroidAppPackage() + ":id/";
    private static final String READER_ACTION = "xpath://*[@resource-id='" + P + "bContinueLesson'";
    private static final String READER_ACTION_TEXT = "translate(normalize-space(@text),"
            + "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')";

    static {
        TODAY_TAB = "id:" + P + "nav_graph_daily";
        TODAY_SELECTED = "xpath://android.widget.FrameLayout[@resource-id='" + P + "nav_graph_daily' and @selected='true']";
        EXPLORE_TAB = "id:" + P + "nav_graph_explore";
        EXPLORE_SELECTED = "xpath://android.widget.FrameLayout[@resource-id='" + P + "nav_graph_explore' and @selected='true']";
        BOOKMARKS_ENTRY = "id:" + P + "ivBookmarks";
        BOOKMARKS_TITLE = "xpath://*[@text='My Bookmarks']";
        BOOKMARKS_BACK = "id:" + P + "btnNavigateUp";
        EMPTY_IMAGE = null; // Android exposes tvEmpty, not the iOS illustration.
        EMPTY_TEXT = "id:" + P + "tvEmpty";
        INACTIVE_BOOKMARK = "xpath://*[@resource-id='" + P + "ivBookmark' and @content-desc='Add to bookmarks']";
        ACTIVE_BOOKMARK = "xpath://*[@resource-id='" + P + "ivBookmark' and @content-desc='Remove from bookmarks']";
        CONTENT_CLOSE = "xpath://*[@resource-id='" + P + "ivButtonClose' or @resource-id='" + P + "btnNavigateUp']";
        CONTENT_EXIT_QUIT = "id:" + P + "btnYes";
        CONTENT_NEXT = READER_ACTION + " and (" + READER_ACTION_TEXT + "='CONTINUE' or "
                + READER_ACTION_TEXT + "='NEXT')]";
        CONTENT_PREVIOUS = "id:" + P + "ivButtonBack";
        FINISH = READER_ACTION + " and " + READER_ACTION_TEXT + "='FINISH']";
        CANCEL = "xpath://*[@text='CANCEL' or @text='Cancel']";
        REMOVE = "xpath://*[@text='REMOVE' or @text='Remove']";
        FEEDBACK = "xpath://*[contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'feedback')"
                + " or contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'how was this')]";
        PAYWALL = "id:" + P + "clPaywallContainer";
    }

    public AndroidBookmarksPageObject(RemoteWebDriver driver) { super(driver); }

    @Override protected String exploreBookmarksEntry() { return "id:" + P + "btnSecondaryAction"; }
    @Override protected String previousDayControl() { return "id:" + P + "ivBack"; }
    @Override protected String nextDayControl() { return "id:" + P + "ivNext"; }

    @Override
    @Step("Verify the first-entry Android Bookmarks tooltip and its anchor")
    public void assertFirstEntryTooltip(String text, List<String> expectedTitles) {
        tapEntryFromToday();
        Rectangle tooltip = waitForElementVisible(exactText(text), "Bookmarks tooltip is absent", 10).getRect();
        byte[] before = driver.getScreenshotAs(OutputType.BYTES);
        Allure.addAttachment("Bookmarks with the tooltip visible", "image/png", new ByteArrayInputStream(before), "png");

        // Android exposes only the balloon window while it is visible. Revealing the list
        // supplies the element bounds, and the two captures verify the positions did not move.
        dismissTooltipByBlankArea(text);
        assertScreenVisible();
        assertTooltipPlacement(tooltip, expectedTitles);
        byte[] after = driver.getScreenshotAs(OutputType.BYTES);
        int screenWidth = driver.manage().window().getSize().width;
        for (String title : expectedTitles) {
            assertSameTooltipRegion(before, after, screenWidth, tooltipBounds(cardTitleFor(title)), "Title of " + title);
            assertSameTooltipRegion(before, after, screenWidth, tooltipBounds(cardBookmarkFor(title)), "Bookmark icon for " + title);
        }
        assertExactCardTitles(expectedTitles, null);
    }

    @Step("Record bookmark controls before preparing a fresh app installation")
    public TooltipLayout captureTooltipLayout(String text, List<String> expectedTitles) {
        openFromTodayWithTooltipDismissed(expectedTitles.get(0), text);
        assertExactCardTitles(expectedTitles, null);
        backFromBookmarksToToday();
        openFromToday();
        assertTooltipAbsent(text);

        TooltipLayout layout = new TooltipLayout(driver.manage().window().getSize(), tooltipBounds(BOOKMARKS_BACK),
                driver.getScreenshotAs(OutputType.BYTES));
        for (String title : expectedTitles) {
            layout.rows.put(title, tooltipBounds(cardFor(title)));
            layout.titles.put(title, tooltipBounds(cardTitleFor(title)));
            layout.icons.put(title, tooltipBounds(cardBookmarkFor(title)));
        }
        return layout;
    }

    @Step("Dismiss the fresh Android Bookmarks tooltip by {variant}")
    public void dismissFreshTooltip(String variant, String text, TooltipLayout layout) {
        tapEntryFromToday();
        Rectangle tooltip = waitForElementVisible(exactText(text), "Bookmarks tooltip is absent", 10).getRect();
        Assert.assertEquals("Android Bookmarks viewport changed after installation", layout.viewport, driver.manage().window().getSize());
        byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
        Allure.addAttachment("Bookmarks before the dismissal tap", "image/png", new ByteArrayInputStream(screenshot), "png");
        String first = assertTooltipPlacement(tooltip, layout.rows, layout.icons);
        for (String title : layout.rows.keySet()) {
            assertSameTooltipRegion(screenshot, layout.screenshot, layout.viewport.width, layout.titles.get(title), "Title of " + title);
            assertSameTooltipRegion(screenshot, layout.screenshot, layout.viewport.width, layout.icons.get(title), "Bookmark icon for " + title);
        }
        assertSameTooltipRegion(screenshot, layout.screenshot, layout.viewport.width, layout.navigation, "Bookmarks back control");

        Point target = layout.dismissalPoint(variant, first);
        Assert.assertFalse("The tap must be outside the tooltip text",
                new java.awt.Rectangle(tooltip.x, tooltip.y, tooltip.width, tooltip.height).contains(target.x, target.y));
        Assert.assertTrue("The tooltip disappeared before the dismissal tap", isElementVisible(exactText(text)));
        Allure.addAttachment("Android tooltip dismissal tap", variant + " at " + target);
        tapBlankArea(target.x, target.y);
        waitForElementNotVisible(exactText(text), "Tooltip stayed visible after tapping " + variant, 10);
        if ("bookmark".equals(variant) && isElementVisible(CANCEL)) cancelRemoval();
    }

    private Rectangle tooltipBounds(String locator) {
        return waitForElementVisible(locator, "Cannot read the bookmark control bounds for " + locator, 5).getRect();
    }

    private String cardTitleFor(String title) {
        return cardFor(title) + "//*[@resource-id='" + P + "tvTitle']";
    }

    private void assertSameTooltipRegion(byte[] before, byte[] after, int screenWidth, Rectangle bounds, String label) {
        try {
            Assert.assertTrue(label + " did not match the position and appearance captured with the tooltip",
                    AppearanceSnapshot.sameRendering(DailyPlanCompletionMarker.crop(before, bounds, screenWidth),
                            DailyPlanCompletionMarker.crop(after, bounds, screenWidth)));
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot inspect " + label + " in the tooltip screenshots", failure);
        }
    }

    public static final class TooltipLayout {
        private final Dimension viewport;
        private final Rectangle navigation;
        private final byte[] screenshot;
        private final Map<String, Rectangle> rows = new HashMap<>();
        private final Map<String, Rectangle> titles = new HashMap<>();
        private final Map<String, Rectangle> icons = new HashMap<>();

        private TooltipLayout(Dimension viewport, Rectangle navigation, byte[] screenshot) {
            this.viewport = viewport;
            this.navigation = navigation;
            this.screenshot = screenshot;
        }

        private Point dismissalPoint(String variant, String first) {
            Rectangle target;
            switch (variant) {
                case "card": target = titles.get(first); break;
                case "bookmark": target = icons.get(first); break;
                case "navigation": target = navigation; break;
                case "blank":
                    Rectangle row = rows.get(first);
                    int right = row.x + row.width;
                    int margin = row.x > 1 ? row.x / 2 : (right + viewport.width) / 2;
                    Assert.assertTrue("No visible blank margin beside the first card",
                            margin > 0 && margin < viewport.width && (margin < row.x || margin > right));
                    return new Point(margin, row.y + row.height / 2);
                default: throw new IllegalArgumentException("Unknown tooltip dismissal variant: " + variant);
            }
            return new Point(target.x + target.width / 2, target.y + target.height / 2);
        }
    }

    @Override protected void tapBlankArea(int x, int y) {
        Map<String, Object> tap = new HashMap<>();
        tap.put("x", x); tap.put("y", y);
        ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", tap);
    }

    @Override protected void swipe(String direction) {
        swipe(direction, 0.75);
    }

    private void swipe(String direction, double portion) {
        Map<String, Object> gesture = new HashMap<>();
        if (("up".equals(direction) || "down".equals(direction)) && isScreenVisible()) {
            if (isElementVisible(EMPTY_TEXT)) return;
            RemoteWebElement list = (RemoteWebElement) waitForElementVisible("id:" + P + "rvBookmarks",
                    "Bookmark list is not available for scrolling", 10);
            gesture.put("elementId", list.getId());
        } else {
            Dimension size = driver.manage().window().getSize();
            gesture.put("left", 0);
            gesture.put("top", 0);
            gesture.put("width", size.width);
            gesture.put("height", size.height);
        }
        gesture.put("direction", direction); gesture.put("percent", portion);
        ((JavascriptExecutor) driver).executeScript("mobile: swipeGesture", gesture);
    }

    @Override public void swipeUp(int timeOfSwipe) { swipe("up"); }

    @Override protected String exactText(String text) {
        return "xpath://*[@text=" + xpathLiteral(text) + " and not(@displayed='false')]";
    }

    @Override protected String slideText(String text) {
        // rvLessonDetail is present in both captured Android lesson/practice readers.
        return "xpath://*[@resource-id='" + P + "rvLessonDetail']//*[@text="
                + xpathLiteral(text) + " and not(@displayed='false')]";
    }

    @Override protected String cardFor(String title) {
        // A row is a CardView child of rvBookmarks; the inner ViewGroup is not its heading's sibling.
        return "xpath:(//*[@resource-id='" + P + "rvBookmarks']//*[@resource-id='" + P
                + "tvTitle' and @text=" + xpathLiteral(title) + "]"
                + "/ancestor::*[parent::*[@resource-id='" + P + "rvBookmarks']][1])";
    }

    @Override protected String cardImageFor(String title) {
        return cardFor(title) + "//*[@resource-id='" + P + "ivCover']";
    }

    @Override protected List<String> visibleCardTitles(List<String> expected) {
        return getElementAccessibleNames("xpath://*[@resource-id='" + P + "rvBookmarks']"
                + "//*[@resource-id='" + P + "tvTitle' and not(@displayed='false')]");
    }

    @Override protected String cardBookmarkFor(String title) {
        return cardFor(title) + "//*[@resource-id='" + P
                + "ivBookmark' and @content-desc='Remove from bookmarks']";
    }

    @Override public void assertCardGroupAndCategory(String title, String group, String category) {
        ensureBookmarkCardVisible(title);
        String groupHeading = cardGroupFor(title, group);
        String categoryHeading = cardCategoryFor(title, category);
        // RecyclerView can expose the card after its preceding heading leaves the native tree.
        for (int step = 0; step < 8 && isElementVisible(cardBookmarkFor(title)); step++) {
            if (isElementVisible(groupHeading) && isElementVisible(categoryHeading)) return;
            String before = bookmarkListSignature();
            swipe("down", 0.2);
            if (before.equals(bookmarkListSignature())) break;
        }
        super.assertCardGroupAndCategory(title, group, category);
    }

    @Override protected String cardCategoryFor(String title, String category) {
        return headingFor(title, "tvSectionName", true, category);
    }

    @Override protected String cardGroupFor(String title, String group) {
        boolean module = group.toUpperCase(java.util.Locale.ROOT).startsWith("MODULE ");
        return headingFor(title, module ? "tvModuleName" : "tvProgramName", module, group);
    }

    private String headingFor(String title, String resource, boolean stopAtProgram, String text) {
        String boundary = "@resource-id=" + xpathLiteral(P + resource);
        if (stopAtProgram) boundary += " or @resource-id=" + xpathLiteral(P + "tvProgramName");
        if ("tvSectionName".equals(resource)) boundary += " or @resource-id=" + xpathLiteral(P + "tvModuleName");
        // Select from the preceding rows in document order across Appium XPath engines.
        return "xpath:(" + cardFor(title).substring("xpath:".length())
                + "/preceding-sibling::*[descendant-or-self::*[" + boundary
                + "]])[last()]/descendant-or-self::*[@resource-id=" + xpathLiteral(P + resource)
                + " and @text=" + xpathLiteral(text) + "]";
    }

    @Override protected String dailyPlanCardFor(String title) {
        return "xpath:(//*[@resource-id='" + P + "tvLessonName' and @text=" + xpathLiteral(title)
                + "]/ancestor::*[@resource-id='" + P + "cvLessonContainer'][1])";
    }

    @Override protected String completedMarkerFor(String title) {
        String status = "translate(normalize-space(@content-desc),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')";
        String text = "translate(normalize-space(@text),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')";
        return dailyPlanCardFor(title) + "//*[" + status + "='COMPLETED' or " + status + "='DONE' or "
                + text + "='COMPLETED' or " + text + "='DONE' or (@resource-id='" + P
                + "lpProgressContainer' and (@content-desc='100%' or @selected='true' or @checked='true'))]";
    }

    @Override protected String dailyPlanCompletionIndicatorFor(String title) {
        return dailyPlanCardFor(title) + "//*[@resource-id='" + P + "ivLearnMore']";
    }

    @Override protected boolean isContentPaywallVisible() {
        return isElementVisible(PAYWALL) || isElementVisible("id:" + P + "tvEworkbook");
    }

    @Override protected void dismissContentPaywallIfPresent() {
        if (!isContentPaywallVisible()) return;
        new AndroidOnboardingPageObject(driver).closePaywallsAndPopups();
        createWait(15).withMessage("Android content paywall stayed visible during cleanup")
                .until(ignored -> !isContentPaywallVisible());
    }

    public void assertSubscriptionPaywallIsDisplayed() {
        waitForElementVisible(PAYWALL, "Android Bookmarks entry did not show the expected subscription paywall", 15);
        Assert.assertFalse("My Bookmarks opened behind the subscription paywall", isScreenVisible());
    }
}
