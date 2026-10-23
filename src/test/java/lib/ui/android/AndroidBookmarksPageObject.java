package lib.ui.android;

import lib.Platform;
import lib.ui.BookmarksPageObject;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.Map;

/** Native Android contract from the local Bookmarks worktree; no iOS selectors are replaced. */
public class AndroidBookmarksPageObject extends BookmarksPageObject {
    private static final String P = Platform.getInstance().getAndroidAppPackage() + ":id/";

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
        CONTENT_NEXT = "id:" + P + "bContinueLesson";
        CONTENT_PREVIOUS = null; // Use the existing explicit-wait swipe path, not an invented Back id.
        FINISH = "xpath://*[@resource-id='" + P + "bContinueLesson' and translate(normalize-space(@text),"
                + "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='FINISH']";
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

    @Override protected void tapBlankArea(int x, int y) {
        Map<String, Object> tap = new HashMap<>();
        tap.put("x", x); tap.put("y", y);
        ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", tap);
    }

    @Override protected void swipe(String direction) {
        Dimension size = driver.manage().window().getSize();
        Map<String, Object> gesture = new HashMap<>();
        gesture.put("left", 0); gesture.put("top", 0);
        gesture.put("width", size.width); gesture.put("height", size.height);
        gesture.put("direction", direction); gesture.put("percent", 0.75);
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

    @Override protected String cardBookmarkFor(String title) {
        return cardFor(title) + "//*[@resource-id='" + P
                + "ivBookmark' and @content-desc='Remove from bookmarks']";
    }

    @Override protected String cardCategoryFor(String title, String category) {
        return headingFor(title, "tvSectionName", false, category);
    }

    @Override protected String cardGroupFor(String title, String group) {
        boolean module = group.toUpperCase(java.util.Locale.ROOT).startsWith("MODULE ");
        return headingFor(title, module ? "tvModuleName" : "tvProgramName", module, group);
    }

    private String headingFor(String title, String resource, boolean stopAtProgram, String text) {
        String boundary = "@resource-id=" + xpathLiteral(P + resource);
        if (stopAtProgram) boundary += " or @resource-id=" + xpathLiteral(P + "tvProgramName");
        return cardFor(title) + "/preceding-sibling::*[descendant-or-self::*[" + boundary
                + "]][1]/descendant-or-self::*[@resource-id=" + xpathLiteral(P + resource)
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

    @Override protected boolean isDailyPlanItemCompleted(String title, boolean attachEvidence) {
        return isElementVisible(completedMarkerFor(title));
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
