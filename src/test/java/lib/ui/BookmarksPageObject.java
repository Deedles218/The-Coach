package lib.ui;

import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.Map;

public abstract class BookmarksPageObject extends MainPageObject {
    protected static String TODAY_TAB;
    protected static String TODAY_SELECTED;
    protected static String EXPLORE_TAB;
    protected static String EXPLORE_SELECTED;
    protected static String BOOKMARKS_ENTRY;
    protected static String BOOKMARKS_TITLE;
    protected static String BOOKMARKS_BACK;
    protected static String EMPTY_IMAGE;
    protected static String EMPTY_TEXT;
    protected static String INACTIVE_BOOKMARK;
    protected static String ACTIVE_BOOKMARK;
    protected static String CONTENT_CLOSE;
    protected static String CONTENT_EXIT_QUIT;
    protected static String FINISH;
    protected static String CANCEL;
    protected static String REMOVE;
    protected static String FEEDBACK;
    protected static String PAYWALL;

    protected BookmarksPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Step("Open My Bookmarks from Today")
    public void openFromToday() {
        tapEntryFromToday();
        waitForElementPresent(BOOKMARKS_TITLE, "My Bookmarks did not open", 15);
    }

    @Step("Tap My Bookmarks entry point from Today")
    public void tapEntryFromToday() {
        if (!isElementPresent(TODAY_SELECTED)) {
            waitForElementAndClick(TODAY_TAB, "Cannot open Today", 10);
            waitForElementPresent(TODAY_SELECTED, "Today tab is not selected", 10);
        }
        tapBookmarksEntry();
    }

    @Step("Open My Bookmarks from Explore")
    public void openFromExplore() {
        if (!isElementPresent(EXPLORE_SELECTED)) {
            waitForElementAndClick(EXPLORE_TAB, "Cannot open Explore", 10);
            waitForElementPresent(EXPLORE_SELECTED, "Explore tab is not selected", 10);
        }
        openBookmarks();
    }

    private void openBookmarks() {
        tapBookmarksEntry();
        waitForElementPresent(BOOKMARKS_TITLE, "My Bookmarks did not open", 15);
    }

    private void tapBookmarksEntry() {
        waitForElementAndClick(BOOKMARKS_ENTRY, "Bookmarks entry point is not displayed", 15);
    }

    @Step("Return from My Bookmarks")
    public void backFromBookmarks() {
        waitForElementAndClick(BOOKMARKS_BACK, "Cannot return from My Bookmarks", 10);
        waitForElementNotVisible(BOOKMARKS_TITLE, "My Bookmarks stayed open", 10);
    }

    @Step("Verify empty Bookmarks state")
    public void assertEmptyState() {
        waitForElementPresent(EMPTY_IMAGE, "Empty Bookmarks illustration is absent", 10);
        waitForElementPresent(EMPTY_TEXT, "Empty Bookmarks explanation is absent", 10);
        Assert.assertFalse("Bookmarks list unexpectedly contains cards", hasAnyBookmarkCard());
    }

    public void assertNotEmpty() {
        Assert.assertFalse("Bookmarks still displays the empty illustration", isElementPresent(EMPTY_IMAGE));
    }

    public boolean hasAnyBookmarkCard() {
        return isElementPresent(ACTIVE_BOOKMARK);
    }

    @Step("Open content from Today: {title}")
    public void openContentFromToday(String title) {
        if (isElementPresent(BOOKMARKS_TITLE)) backFromBookmarks();
        waitForElementAndClick(TODAY_TAB, "Cannot open Today", 10);
        String locator = exactText(title);
        swipeUpToFindFirstVisibleElement(new String[]{locator}, "Daily Plan content is absent: " + title, 8);
        waitForElementAndClick(locator, "Cannot open Daily Plan content: " + title, 10);
        waitForFirstElementPresent(new String[]{INACTIVE_BOOKMARK, ACTIVE_BOOKMARK},
                "Content does not expose the bookmark control: " + title, 20);
    }

    @Step("Open non-bookmarkable content from Today: {title}")
    public void openContentWithoutBookmarkFromToday(String title) {
        if (isElementPresent(BOOKMARKS_TITLE)) backFromBookmarks();
        waitForElementAndClick(TODAY_TAB, "Cannot open Today", 10);
        String locator = exactText(title);
        swipeUpToFindFirstVisibleElement(new String[]{locator}, "Daily Plan content is absent: " + title, 8);
        waitForElementAndClick(locator, "Cannot open Daily Plan content: " + title, 10);
        waitForElementPresent(exactText(title), "Content did not open: " + title, 20);
    }

    @Step("Save opened content")
    public void saveOpenedContent() {
        waitForElementAndClick(INACTIVE_BOOKMARK, "Inactive bookmark control is absent", 10);
        waitForElementNotVisible(INACTIVE_BOOKMARK, "Bookmark did not switch to active state", 15);
        waitForElementPresent(ACTIVE_BOOKMARK, "Active bookmark control is absent", 10);
    }

    @Step("Remove opened content from Bookmarks")
    public void removeOpenedContent() {
        waitForElementAndClick(ACTIVE_BOOKMARK, "Active bookmark control is absent", 10);
        waitForElementPresent(INACTIVE_BOOKMARK, "Bookmark did not switch to inactive state", 15);
    }

    public void ensureOpenedContentIsNotSaved() {
        if (isElementPresent(ACTIVE_BOOKMARK)) removeOpenedContent();
        waitForElementPresent(INACTIVE_BOOKMARK, "Content did not reach inactive bookmark state", 10);
    }

    public void assertOpenedContentSaved() {
        waitForElementPresent(ACTIVE_BOOKMARK, "Bookmark is not active on the opened slide", 10);
        Assert.assertFalse("Both active and inactive bookmark controls are visible", isElementPresent(INACTIVE_BOOKMARK));
    }

    public void assertOpenedContentCannotBeSaved() {
        Assert.assertFalse("Kegel content exposes an inactive bookmark", isElementPresent(INACTIVE_BOOKMARK));
        Assert.assertFalse("Kegel content exposes an active bookmark", isElementPresent(ACTIVE_BOOKMARK));
    }

    @Step("Close opened content")
    public void closeOpenedContent() {
        waitForElementAndClick(CONTENT_CLOSE, "Cannot close content", 10);
        confirmContentExitIfNeeded();
        waitForElementPresent(TODAY_TAB, "Today did not return after closing content", 15);
    }

    public void closeOpenedContentIfPresent() {
        if (isElementPresent(CONTENT_EXIT_QUIT)) {
            confirmContentExitIfNeeded();
            waitForFirstElementPresent(new String[]{TODAY_TAB, BOOKMARKS_TITLE},
                    "Neither Today nor My Bookmarks returned after confirming content exit", 15);
            return;
        }
        if (!isElementPresent(FINISH)
                || (!isElementPresent(INACTIVE_BOOKMARK) && !isElementPresent(ACTIVE_BOOKMARK))) return;
        waitForElementAndClick(CONTENT_CLOSE, "Cannot close interrupted bookmarked content", 10);
        confirmContentExitIfNeeded();
        waitForFirstElementPresent(new String[]{TODAY_TAB, BOOKMARKS_TITLE},
                "Neither Today nor My Bookmarks returned after closing content", 15);
    }

    private void confirmContentExitIfNeeded() {
        if (isElementPresent(CONTENT_EXIT_QUIT)) {
            waitForElementAndClick(CONTENT_EXIT_QUIT, "Cannot confirm leaving bookmarked content", 10);
            waitForElementNotVisible(CONTENT_EXIT_QUIT, "Content exit dialog stayed visible", 10);
        }
    }

    @Step("Verify bookmark card: {title}")
    public void assertCard(String title) {
        waitForElementPresent(exactText(title), "Bookmark card is absent: " + title, 15);
        assertNotEmpty();
    }

    public void assertCardAbsent(String title) {
        Assert.assertFalse("Bookmark card is still displayed: " + title, isElementPresent(exactText(title)));
    }

    public void assertCardComposition(String title) {
        String card = cardFor(title);
        waitForElementPresent(card, "Bookmark card container is absent: " + title, 10);
        waitForElementPresent(card + "//XCUIElementTypeImage", "Bookmark card has no image: " + title, 10);
        waitForElementPresent(card + "//*[@visible='true' and contains(translate(@name,'BOOKMARK','bookmark'),'bookmark')]",
                "Bookmark card has no active bookmark icon: " + title, 10);
    }

    public void assertGroupAndCategory(String group, String category) {
        waitForElementPresent(exactText(group), "Bookmark group is absent: " + group, 10);
        waitForElementPresent(exactText(category), "Bookmark category is absent: " + category, 10);
    }

    public void assertCategoryAbsent(String category) {
        Assert.assertFalse("Empty category must be hidden: " + category, isElementPresent(exactText(category)));
    }

    @Step("Open bookmark card: {title}")
    public void openCard(String title) {
        waitForElementAndClick(exactText(title), "Cannot open bookmark card: " + title, 10);
        waitForFirstElementPresent(new String[]{INACTIVE_BOOKMARK, ACTIVE_BOOKMARK},
                "Bookmark card did not open content", 20);
    }

    @Step("Open removal dialog for card: {title}")
    public void openRemovalDialog(String title) {
        String control = cardFor(title)
                + "//*[@visible='true' and contains(translate(@name,'BOOKMARK','bookmark'),'bookmark')]";
        waitForElementAndClick(control, "Cannot open bookmark removal dialog for: " + title, 10);
        waitForElementPresent(CANCEL, "Removal dialog has no CANCEL action", 10);
        waitForElementPresent(REMOVE, "Removal dialog has no REMOVE action", 10);
    }

    public void cancelRemoval() {
        waitForElementAndClick(CANCEL, "Cannot cancel bookmark removal", 10);
        waitForElementNotVisible(CANCEL, "Bookmark removal dialog stayed open", 10);
    }

    public void confirmRemoval() {
        waitForElementAndClick(REMOVE, "Cannot confirm bookmark removal", 10);
        waitForElementNotVisible(REMOVE, "Bookmark removal dialog stayed open", 10);
    }

    public void assertTooltipVisible(String text) {
        waitForElementPresent(exactText(text), "Bookmarks tooltip is absent", 10);
    }

    public void assertTooltipAbsent(String text) {
        Assert.assertFalse("Bookmarks tooltip must not be displayed", isElementPresent(exactText(text)));
    }

    public void dismissTooltipByBlankArea(String text) {
        Dimension size = driver.manage().window().getSize();
        Map<String, Object> tap = new HashMap<>();
        tap.put("x", size.width / 2);
        tap.put("y", Math.max(80, size.height / 4));
        ((JavascriptExecutor) driver).executeScript("mobile: tap", tap);
        waitForElementNotVisible(exactText(text), "Tooltip stayed visible after blank-area tap", 10);
    }

    public void dismissTooltipByCard(String title, String text) {
        waitForElementAndClick(exactText(title), "Cannot tap bookmark card", 10);
        waitForElementNotVisible(exactText(text), "Tooltip stayed visible after card tap", 10);
    }

    public void dismissTooltipByBookmark(String title, String text) {
        openRemovalDialog(title);
        waitForElementNotVisible(exactText(text), "Tooltip stayed visible after bookmark tap", 10);
        cancelRemoval();
    }

    public void dismissTooltipByNavigation(String text) {
        backFromBookmarks();
        Assert.assertFalse("Tooltip survived navigation", isElementPresent(exactText(text)));
    }

    public void reopenAndAssertTooltipAbsent(String text) {
        if (!isElementPresent(BOOKMARKS_TITLE)) openFromToday();
        else {
            backFromBookmarks();
            openFromToday();
        }
        assertTooltipAbsent(text);
    }

    public void swipeLeftAndAssertSaved(String expectedSlideText) {
        swipe("left");
        waitForElementPresent(exactText(expectedSlideText), "Expected content slide did not open: " + expectedSlideText, 15);
        assertOpenedContentSaved();
    }

    public void swipeLeftToSlide(String expectedSlideText) {
        swipe("left");
        waitForElementPresent(exactText(expectedSlideText), "Expected content slide did not open: " + expectedSlideText, 15);
    }

    public void swipeRightAndAssertSaved(String expectedSlideText) {
        swipe("right");
        waitForElementPresent(exactText(expectedSlideText), "Expected content slide did not open: " + expectedSlideText, 15);
        assertOpenedContentSaved();
    }

    public void assertOpenedAtSlide(String title, String slideText) {
        waitForElementPresent(exactText(title), "Wrong bookmarked content opened: " + title, 10);
        waitForElementPresent(exactText(slideText), "Content did not open at the saved slide: " + slideText, 10);
    }

    public void finishFromBookmarks() {
        waitForElementAndClick(FINISH, "FINISH is absent in bookmarked content", 10);
        waitForElementPresent(BOOKMARKS_TITLE, "FINISH did not return to My Bookmarks", 20);
        Assert.assertFalse("Feedback UI appeared after FINISH from Bookmarks", isElementPresent(FEEDBACK));
    }

    public void assertDailyPlanItemCompleted(String title) {
        if (isElementPresent(BOOKMARKS_TITLE)) backFromBookmarks();
        waitForElementAndClick(TODAY_TAB, "Cannot open Today", 10);
        String completed = cardFor(title)
                + "//*[@visible='true' and (contains(translate(@name,'COMPLETED','completed'),'completed')"
                + " or contains(translate(@name,'DONE','done'),'done'))]";
        waitForElementPresent(completed, "Daily Plan item is not marked completed: " + title, 15);
    }

    public void assertPreviousDayItemNotCompleted(String title, String previousDayTitle) {
        waitForElementAndClick("id:leftSwitcherArrow", "Cannot open previous Daily Plan day", 10);
        waitForElementPresent(exactText(previousDayTitle), "Previous Daily Plan day did not open", 15);
        String titleLocator = exactText(title);
        swipeUpToFindFirstVisibleElement(new String[]{titleLocator},
                "Recurring practice is absent on previous day: " + title, 8);
        String completed = cardFor(title)
                + "//*[@visible='true' and (contains(translate(@name,'COMPLETED','completed'),'completed')"
                + " or contains(translate(@name,'DONE','done'),'done'))]";
        Assert.assertFalse("Finishing the saved occurrence also completed the previous day",
                isElementPresent(completed));
    }

    public void assertNoPaywall() {
        Assert.assertFalse("Paywall blocks bookmarked content", isElementPresent(PAYWALL));
    }

    public void assertScreenVisible() {
        waitForElementPresent(BOOKMARKS_TITLE, "My Bookmarks is not visible", 10);
    }

    public boolean isScreenVisible() {
        return isElementPresent(BOOKMARKS_TITLE);
    }

    private void swipe(String direction) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("direction", direction);
        ((JavascriptExecutor) driver).executeScript("mobile: swipe", arguments);
    }

    protected String exactText(String text) {
        return "xpath://XCUIElementTypeStaticText[@name=" + xpathLiteral(text) + " and @visible='true']";
    }

    protected String cardFor(String title) {
        String literal = xpathLiteral(title);
        return "xpath:(//XCUIElementTypeStaticText[@name=" + literal + " and @visible='true']"
                + "/ancestor::*[self::XCUIElementTypeCell or self::XCUIElementTypeOther][.//*[@name="
                + literal + "]][1])";
    }

    private String xpathLiteral(String value) {
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        StringBuilder result = new StringBuilder("concat(");
        String[] parts = value.split("'", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(", \"'\", ");
            result.append("'").append(parts[i]).append("'");
        }
        return result.append(')').toString();
    }
}
