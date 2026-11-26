package lib.ui;

import io.qameta.allure.Step;
import lib.RestoringTestAction;
import lib.TestData;
import lib.DailyPlanCompletionMarker;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

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
    protected static String CONTENT_NEXT;
    protected static String CONTENT_PREVIOUS;
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
        if (isScreenVisible()) return;
        tapEntryFromToday();
        waitForElementPresent(BOOKMARKS_TITLE, "My Bookmarks did not open", 15);
    }

    @Step("Open My Bookmarks from Today, allowing the first-visit tooltip")
    public void openFromTodayAllowingTooltip(String tooltip) {
        if (isScreenVisible()) return;
        tapEntryFromToday();
        waitForFirstElementPresent(new String[]{tooltip == null ? null : tooltipFor(tooltip), BOOKMARKS_TITLE},
                "Neither My Bookmarks nor its tooltip appeared", 15);
    }

    @Step("Tap My Bookmarks entry point from Today")
    public void tapEntryFromToday() {
        if (!isElementPresent(TODAY_SELECTED)) {
            waitForElementAndClick(TODAY_TAB, "Cannot open Today", 10);
            waitForElementPresent(TODAY_SELECTED, "Today tab is not selected", 10);
        }
        tapBookmarksEntry(BOOKMARKS_ENTRY);
    }

    @Step("Open My Bookmarks from Explore")
    public void openFromExplore() {
        CoachFlowPageObjectFactory.get(driver).openExplore();
        openBookmarks(exploreBookmarksEntry());
    }

    private void openBookmarks(String entry) {
        tapBookmarksEntry(entry);
        waitForElementPresent(BOOKMARKS_TITLE, "My Bookmarks did not open", 15);
    }

    protected String exploreBookmarksEntry() { return BOOKMARKS_ENTRY; }

    protected void tapBookmarksEntry(String entry) {
        waitForElementAndClick(entry, "Bookmarks entry point is not displayed", 15);
    }

    @Step("Return from My Bookmarks")
    public void backFromBookmarks() {
        waitForElementAndClick(BOOKMARKS_BACK, "Cannot return from My Bookmarks", 10);
        waitForElementNotVisible(BOOKMARKS_TITLE, "My Bookmarks stayed open", 10);
    }

    @Step("Verify Back from My Bookmarks returns to Today")
    public void backFromBookmarksToToday() {
        backFromBookmarks();
        waitForElementVisible(TODAY_SELECTED, "Back from My Bookmarks did not return to Today", 10);
    }

    @Step("Verify Back from My Bookmarks returns to Explore")
    public void backFromBookmarksToExplore() {
        backFromBookmarks();
        waitForElementVisible(EXPLORE_SELECTED, "Back from My Bookmarks did not return to Explore", 10);
    }

    @Step("Verify empty Bookmarks state")
    public void assertEmptyState() {
        if (EMPTY_IMAGE != null) waitForElementPresent(EMPTY_IMAGE, "Empty Bookmarks illustration is absent", 10);
        waitForElementPresent(EMPTY_TEXT, "Empty Bookmarks explanation is absent", 10);
        Assert.assertFalse("Bookmarks list unexpectedly contains cards", hasAnyBookmarkCard());
    }

    public void assertNotEmpty() {
        if (EMPTY_IMAGE != null) {
            Assert.assertFalse("Bookmarks still displays the empty illustration", isElementPresent(EMPTY_IMAGE));
        } else {
            Assert.assertTrue("Bookmarks list has no saved cards", hasAnyBookmarkCard());
        }
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
        waitForElementPresent(CONTENT_CLOSE, "Content did not open: " + title, 20);
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

    public void ensureOpenedContentIsSaved() {
        if (!isElementPresent(ACTIVE_BOOKMARK)) saveOpenedContent();
        assertOpenedContentSaved();
    }

    public void assertOpenedContentSaved() {
        waitForElementPresent(ACTIVE_BOOKMARK, "Bookmark is not active on the opened slide", 10);
        Assert.assertFalse("Both active and inactive bookmark controls are visible", isElementPresent(INACTIVE_BOOKMARK));
    }

    public void assertOpenedContentNotSaved() {
        waitForElementPresent(INACTIVE_BOOKMARK, "Bookmark is not inactive on the opened slide", 10);
        Assert.assertFalse("Removed content still exposes an active bookmark", isElementPresent(ACTIVE_BOOKMARK));
    }

    public void assertOpenedContentCannotBeSaved() {
        Assert.assertFalse("Kegel content exposes an inactive bookmark", isElementPresent(INACTIVE_BOOKMARK));
        Assert.assertFalse("Kegel content exposes an active bookmark", isElementPresent(ACTIVE_BOOKMARK));
    }

    @Step("Close opened content")
    public void closeOpenedContent() {
        waitForElementAndClick(CONTENT_CLOSE, "Cannot close content", 10);
        waitForContentExit();
    }

    public void closeOpenedContentIfPresent() {
        dismissContentPaywallIfPresent();
        if (isElementPresent(CONTENT_EXIT_QUIT)) {
            waitForContentExit();
            return;
        }
        if (!isElementPresent(CONTENT_CLOSE)
                || (!isElementPresent(INACTIVE_BOOKMARK) && !isElementPresent(ACTIVE_BOOKMARK))) return;
        waitForElementAndClick(CONTENT_CLOSE, "Cannot close interrupted bookmarked content", 10);
        waitForContentExit();
    }

    private void waitForContentExit() {
        waitForFirstElementPresent(new String[]{CONTENT_EXIT_QUIT, TODAY_TAB, BOOKMARKS_TITLE},
                "Neither the exit confirmation nor Today or My Bookmarks appeared after closing content", 15);
        if (isElementVisible(CONTENT_EXIT_QUIT)) {
            waitForElementAndClick(CONTENT_EXIT_QUIT, "Cannot confirm leaving bookmarked content", 10);
            createWait(10).withMessage("Content exit dialog stayed visible")
                    .until(ignored -> !isElementVisible(CONTENT_EXIT_QUIT));
        }
        waitForFirstElementPresent(new String[]{TODAY_TAB, BOOKMARKS_TITLE},
                "Neither Today nor My Bookmarks returned after closing content", 15);
    }

    @Step("Verify bookmark card: {title}")
    public void assertCard(String title) {
        ensureBookmarkCardVisible(title);
        waitForElementPresent(exactText(title), "Bookmark card is absent: " + title, 15);
        assertNotEmpty();
    }

    public void assertSingleCard(String title, String... slideTexts) {
        assertCard(title);
        Assert.assertEquals("The whole content must have exactly one bookmark card: " + title,
                1, getAmountElements(cardFor(title)));
        for (String slide : slideTexts) {
            if (!title.equals(slide)) assertCardAbsent(slide);
        }
    }

    public void assertCardAbsent(String title) {
        assertScreenVisible();
        // Recycler-backed lists may omit offscreen rows from the native tree.
        // Reach the beginning, then verify every viewport through the end.
        scanForAbsentCard(title, "down");
        scanForAbsentCard(title, "up");
    }

    private void scanForAbsentCard(String title, String direction) {
        scanBookmarkViewports(direction, () -> Assert.assertFalse(
                "Bookmark card is still displayed: " + title, isElementPresent(exactText(title))));
    }

    private void scanBookmarkViewports(String direction, Runnable check) {
        for (int viewport = 0; viewport < 20; viewport++) {
            assertScreenVisible();
            check.run();
            String before = bookmarkListSignature();
            swipe(direction);
            if (before.equals(bookmarkListSignature())) {
                check.run();
                return;
            }
        }
        Assert.fail("Bookmarks list boundary was not reached while scrolling " + direction);
    }

    public void assertExactCardTitles(List<String> expected, String absentCategory) {
        scanBookmarkViewports("down", () -> { });
        List<String> visited = new ArrayList<>();
        scanBookmarkViewports("up", () -> {
            if (absentCategory != null) assertCategoryAbsent(absentCategory);
            List<String> visible = visibleCardTitles(expected);
            Assert.assertFalse("Cannot read the saved cards in this viewport", visible.isEmpty());
            Assert.assertEquals("Duplicate bookmark titles in one viewport", visible.size(), new LinkedHashSet<>(visible).size());
            Assert.assertTrue("Unexpected bookmark cards in viewport: " + visible, expected.containsAll(visible));
            // Merge only the ordered overlap so a nonadjacent repeated card is still detected.
            int overlap = Math.min(visited.size(), visible.size());
            while (overlap > 0 && !visited.subList(visited.size() - overlap, visited.size()).equals(visible.subList(0, overlap))) {
                overlap--;
            }
            Assert.assertTrue("Bookmark list scroll skipped rows between viewports", visited.isEmpty() || overlap > 0);
            for (String title : visible.subList(overlap, visible.size())) {
                Assert.assertFalse("Bookmark card appears again after intervening rows: " + title, visited.contains(title));
                visited.add(title);
            }
        });
        Assert.assertEquals("Unexpected saved card set", new LinkedHashSet<>(expected), new LinkedHashSet<>(visited));
    }

    protected List<String> visibleCardTitles(List<String> expected) {
        String anchor = expected.stream().filter(this::hasCard).findFirst()
                .orElseThrow(() -> new AssertionError("No expected bookmark card is visible in this viewport"));
        return getElementAccessibleNames(cardListTitleLocator(anchor));
    }

    protected String cardListTitleLocator(String anchor) {
        return cardFor(anchor) + "/../*[self::XCUIElementTypeButton or self::XCUIElementTypeCell"
                + " or self::XCUIElementTypeOther][@visible='true']"
                + "/descendant::XCUIElementTypeStaticText[@visible='true'][1]";
    }

    protected String bookmarkListSignature() {
        return driver.getPageSource();
    }

    public boolean hasCard(String title) {
        return isElementVisible(exactText(title));
    }

    protected void ensureBookmarkCardVisible(String title) {
        String control = cardBookmarkFor(title);
        for (String direction : new String[]{"up", "down"}) {
            for (int attempt = 0; attempt < 8; attempt++) {
                if (isElementVisible(control)) return;
                swipe(direction);
            }
        }
        Assert.assertTrue("Bookmark card control is not visible after scrolling: " + title,
                isElementVisible(control));
    }

    public void assertCardComposition(String title) {
        ensureBookmarkCardVisible(title);
        String card = cardFor(title);
        Rectangle row = waitForElementPresent(card, "Bookmark card container is absent: " + title, 10).getRect();
        waitForElementPresent(cardImageFor(title), "Bookmark card has no image: " + title, 10);
        Rectangle icon = waitForElementPresent(cardBookmarkFor(title),
                "Bookmark card has no active bookmark icon: " + title, 10).getRect();
        double iconCenterX = icon.x + icon.getWidth() / 2.0;
        Assert.assertTrue("The active bookmark icon must be on the right side of the card: " + title,
                iconCenterX > row.x + row.getWidth() / 2.0 && iconCenterX < row.x + row.getWidth());
    }

    public void assertCardCover(String title, BufferedImage expected) throws IOException {
        ensureBookmarkCardVisible(title);
        WebElement cover = waitForElementPresent(cardImageFor(title), "Bookmark card has no image: " + title, 10);
        byte[] screenshot = cover.getScreenshotAs(OutputType.BYTES);
        BufferedImage actual = ImageIO.read(new ByteArrayInputStream(screenshot));
        Assert.assertNotNull("Unreadable bookmark cover: " + title, actual);
        int width = expected.getWidth(), height = expected.getHeight();
        boolean sameSize = actual.getWidth() == width && actual.getHeight() == height;
        if (!sameSize || !Arrays.equals(expected.getRGB(0, 0, width, height, null, 0, width),
                actual.getRGB(0, 0, width, height, null, 0, width))) {
            Allure.addAttachment("Observed cover for " + title, "image/png", new ByteArrayInputStream(screenshot), "png");
            Assert.fail("Bookmark cover differs from the approved reference: " + title);
        }
    }

    public void assertGroupAndCategory(String group, String category) {
        waitForElementPresent(exactText(group), "Bookmark group is absent: " + group, 10);
        waitForElementPresent(exactText(category), "Bookmark category is absent: " + category, 10);
    }

    public void assertCardGroupAndCategory(String title, String group, String category) {
        ensureBookmarkCardVisible(title);
        waitForElementPresent(cardCategoryFor(title, category),
                "Bookmark card is in the wrong category: " + title + " / " + category, 10);
        waitForElementPresent(cardGroupFor(title, group),
                "Bookmark card is in the wrong group: " + title + " / " + group, 10);
    }

    protected String cardCategoryFor(String title, String category) {
        return cardFor(title) + "/preceding-sibling::XCUIElementTypeStaticText[1]"
                + "[@visible='true' and @name=" + xpathLiteral(category) + "]";
    }

    protected String cardGroupFor(String title, String group) {
        // In the observed native list, program/module/category headings and
        // card buttons are siblings. The nearest matching heading owns a card.
        String module = "starts-with(@name,'MODULE ')";
        String heading = "@name!='Lessons' and @name!='Practices'";
        if (!group.startsWith("MODULE ")) heading += " and not(" + module + ")";
        return cardFor(title) + "/preceding-sibling::XCUIElementTypeStaticText[" + heading + "][1]"
                + "[@visible='true' and @name=" + xpathLiteral(group) + "]";
    }

    public void assertCategoryAbsent(String category) {
        Assert.assertFalse("Empty category must be hidden: " + category, isElementPresent(exactText(category)));
    }

    @Step("Open bookmark card: {title}")
    public void openCard(String title) {
        tapBookmarkCard(title);
        createWait(20).withMessage("Bookmark card did not open content").until(ignored ->
                isElementVisible(INACTIVE_BOOKMARK) || isElementVisible(ACTIVE_BOOKMARK)
                        || isContentPaywallVisible());
        if (isContentPaywallVisible()) {
            attachPaywallEvidence("Paywall blocking bookmarked content");
            Assert.fail("Subscription paywall blocked bookmarked content: " + title);
        }
    }

    @Step("Verify that bookmark card shows the subscription paywall: {title}")
    public void openCardExpectingPaywall(String title) {
        tapBookmarkCard(title);
        createWait(20).withMessage("Bookmark card did not show the subscription paywall: " + title)
                .until(ignored -> isContentPaywallVisible());
        attachPaywallEvidence("Expected subscription paywall for bookmarked content");
    }

    private void tapBookmarkCard(String title) {
        ensureBookmarkCardVisible(title);
        waitForElementAndClick(exactText(title), "Cannot open bookmark card: " + title, 10);
    }

    private void attachPaywallEvidence(String name) {
        Allure.addAttachment(name, "application/xml", TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
        Allure.addAttachment(name, "image/png",
                new ByteArrayInputStream(driver.getScreenshotAs(OutputType.BYTES)), "png");
    }

    protected boolean isContentPaywallVisible() {
        return isElementVisible(PAYWALL);
    }

    protected abstract void dismissContentPaywallIfPresent();

    @Step("Open removal dialog for card: {title}")
    public void openRemovalDialog(String title) {
        ensureBookmarkCardVisible(title);
        String control = cardBookmarkFor(title);
        waitForElementAndClick(control, "Cannot open bookmark removal dialog for: " + title, 10);
        waitForElementPresent(CANCEL, "Removal dialog has no CANCEL action", 10);
        waitForElementPresent(REMOVE, "Removal dialog has no REMOVE action", 10);
    }

    public void cancelRemoval() {
        waitForElementAndClick(CANCEL, "Cannot cancel bookmark removal", 10);
        waitForElementNotVisible(CANCEL, "Bookmark removal dialog stayed open", 10);
        waitForElementVisible(BOOKMARKS_TITLE, "My Bookmarks did not return after CANCEL", 10);
    }

    public void confirmRemoval() {
        waitForElementAndClick(REMOVE, "Cannot confirm bookmark removal", 10);
        waitForElementNotVisible(REMOVE, "Bookmark removal dialog stayed open", 10);
        waitForElementVisible(BOOKMARKS_TITLE, "My Bookmarks did not return after REMOVE", 10);
    }

    public void assertTooltipVisible(String text) {
        waitForElementPresent(tooltipFor(text), "Bookmarks tooltip is absent", 10);
    }

    public void assertFirstEntryTooltip(String text, List<String> expectedTitles) {
        openFromTodayAndInspectTooltip(text, expectedTitles);
    }

    @Step("Open My Bookmarks and inspect the first card while the tooltip is showing")
    public String openFromTodayAndInspectTooltip(String text, List<String> expectedTitles) {
        tapEntryFromToday();
        Rectangle tooltip = waitForElementPresent(tooltipFor(text), "Bookmarks tooltip is absent", 10).getRect();
        return assertTooltipPlacement(tooltip, expectedTitles);
    }

    protected String assertTooltipPlacement(Rectangle tooltip, List<String> expectedTitles) {
        List<String> visibleTitles = visibleCardTitles(expectedTitles);
        Assert.assertEquals("The expected cards must be visible without moving the tooltip",
                new LinkedHashSet<>(expectedTitles), new LinkedHashSet<>(visibleTitles));
        Assert.assertEquals("Duplicate or missing card in the tooltip fixture", expectedTitles.size(), visibleTitles.size());
        Map<String, Rectangle> rows = new HashMap<>();
        Map<String, Rectangle> icons = new HashMap<>();
        for (String title : visibleTitles) {
            rows.put(title, waitForElementPresent(cardFor(title), "Cannot inspect card under the tooltip: " + title, 5).getRect());
            icons.put(title, waitForElementPresent(cardBookmarkFor(title), "Cannot locate the saved bookmark icon: " + title, 5).getRect());
        }
        return assertTooltipPlacement(tooltip, rows, icons);
    }

    protected String assertTooltipPlacement(Rectangle tooltip, Map<String, Rectangle> rows, Map<String, Rectangle> icons) {
        String first = rows.keySet().stream().min(Comparator.comparingInt(title -> rows.get(title).y)).get();
        Rectangle firstIcon = icons.get(first);
        int gap = tooltipGapToIcon(tooltip, firstIcon);
        Assert.assertTrue("Tooltip is not adjacent to the first card's bookmark icon: " + first,
                gap <= rows.get(first).getHeight());
        for (String title : rows.keySet()) {
            if (!title.equals(first)) {
                Assert.assertTrue("Tooltip is closer to another card's bookmark icon: " + title,
                        gap < tooltipGapToIcon(tooltip, icons.get(title)));
            }
        }
        Allure.addAttachment("First visible bookmark under tooltip", first + "\nTooltip " + tooltip + "\nBookmark icon " + firstIcon);
        return first;
    }

    private static int tooltipGapToIcon(Rectangle tooltip, Rectangle icon) {
        int x = icon.x + icon.getWidth() / 2, y = icon.y + icon.getHeight() / 2;
        int horizontal = Math.max(tooltip.x - x, x - (tooltip.x + tooltip.getWidth()));
        int vertical = Math.max(tooltip.y - y, y - (tooltip.y + tooltip.getHeight()));
        if ((horizontal <= 0) == (vertical <= 0)) return Integer.MAX_VALUE;
        return Math.max(horizontal, vertical);
    }

    public void assertTooltipAbsent(String text) {
        Assert.assertFalse("Bookmarks tooltip must not be displayed", isElementPresent(tooltipFor(text)));
    }

    @Step("Dismiss the observed Bookmarks tooltip during fixture preparation")
    public void dismissTooltipIfVisible(String text) {
        if (isElementVisible(tooltipFor(text))) dismissTooltipByBlankArea(text);
    }

    @Step("Open prepared My Bookmarks with the tooltip dismissed")
    public void openFromTodayWithTooltipDismissed(String title, String tooltip) {
        if (!isScreenVisible()) tapEntryFromToday();
        waitForFirstElementPresent(new String[]{tooltipFor(tooltip), cardBookmarkFor(title)},
                "Neither the Bookmarks tooltip nor the prepared bookmark appeared", 15);
        dismissTooltipIfVisible(tooltip);
        assertScreenVisible();
    }

    public void dismissTooltipByBlankArea(String text) {
        Dimension size = driver.manage().window().getSize();
        tapBlankArea(size.width / 2, Math.max(80, size.height / 4));
        waitForElementNotVisible(tooltipFor(text), "Tooltip stayed visible after blank-area tap", 10);
    }

    public void dismissTooltipBesideCard(String title, String text) {
        Rectangle card = waitForElementPresent(cardFor(title), "Bookmark card is unavailable for the blank-area tap", 5).getRect();
        Rectangle tooltip = waitForElementPresent(tooltipFor(text), "Bookmarks tooltip is absent", 5).getRect();
        Dimension screen = driver.manage().window().getSize();
        int y = card.y + card.getHeight() / 2;
        Assert.assertTrue("The card center is outside the viewport", y > 0 && y < screen.height);
        int right = card.x + card.getWidth();
        for (Point candidate : Arrays.asList(new Point(card.x / 2, y), new Point((right + screen.width) / 2, y))) {
            if (candidate.x <= 0 || candidate.x >= screen.width || (candidate.x >= card.x && candidate.x <= right)) continue;
            if (candidate.x >= tooltip.x && candidate.x <= tooltip.x + tooltip.getWidth()
                    && y >= tooltip.y && y <= tooltip.y + tooltip.getHeight()) continue;
            tapBlankArea(candidate.x, candidate.y);
            waitForElementNotVisible(tooltipFor(text), "Tooltip stayed visible after blank-area tap", 10);
            return;
        }
        Assert.fail("Cannot identify a blank margin beside the bookmark card");
    }

    public void dismissTooltipByCard(String title, String text) {
        waitForElementAndClick(exactText(title), "Cannot tap bookmark card", 10);
        waitForElementNotVisible(tooltipFor(text), "Tooltip stayed visible after card tap", 10);
    }

    public void dismissTooltipByBookmark(String title, String text) {
        waitForElementAndClick(cardBookmarkFor(title), "Cannot tap bookmark icon under tooltip", 10);
        waitForElementNotVisible(tooltipFor(text), "Tooltip stayed visible after bookmark tap", 10);
        if (isElementVisible(CANCEL)) cancelRemoval();
    }

    public void dismissTooltipByNavigation(String text) {
        waitForElementAndClick(BOOKMARKS_BACK, "Cannot tap navigation under tooltip", 10);
        waitForElementNotVisible(tooltipFor(text), "Tooltip survived navigation tap", 10);
    }

    public void reopenAndAssertTooltipAbsent(String text) {
        closeOpenedContentIfPresent();
        if (!isElementPresent(BOOKMARKS_TITLE)) openFromToday();
        else {
            backFromBookmarks();
            openFromToday();
        }
        assertTooltipAbsent(text);
    }

    public void swipeLeftAndAssertSaved(String expectedSlideText) {
        moveToSlide("left", expectedSlideText, true);
        assertOpenedContentSaved();
    }

    public void swipeLeftToSlide(String expectedSlideText) {
        moveToSlide("left", expectedSlideText, null);
    }

    public void swipeLeftAndAssertNotSaved(String expectedSlideText) {
        moveToSlide("left", expectedSlideText, false);
        assertOpenedContentNotSaved();
    }

    public void swipeRightAndAssertNotSaved(String expectedSlideText) {
        moveToSlide("right", expectedSlideText, false);
        assertOpenedContentNotSaved();
    }

    public void swipeRightAndAssertSaved(String expectedSlideText) {
        moveToSlide("right", expectedSlideText, true);
        assertOpenedContentSaved();
    }

    @Step("Verify the bookmark state on every reader slide")
    public void assertBookmarkOnEverySlide(List<String> markers, boolean saved) {
        Assert.assertFalse("Configure the ordered markers for all reader slides", markers.isEmpty());
        Assert.assertEquals("Each reader slide needs a distinct marker", markers.size(), new LinkedHashSet<>(markers).size());
        moveToSlide("right", markers.get(0), saved);
        assertSlideBookmarkState(saved);
        for (int index = 1; index < markers.size(); index++) {
            Assert.assertFalse("FINISH appeared before the final configured slide", isElementVisible(FINISH));
            String previous = slideText(markers.get(index - 1));
            String next = slideText(markers.get(index));
            navigateOneSlide("left");
            createWait(10).withMessage("Reader did not advance to configured slide " + (index + 1))
                    .until(ignored -> isElementVisible(next) && !isElementVisible(previous));
            assertSlideBookmarkState(saved);
        }
        waitForElementVisible(FINISH, "The final configured slide must display FINISH", 10);
    }

    private void assertSlideBookmarkState(boolean saved) {
        if (saved) assertOpenedContentSaved();
        else assertOpenedContentNotSaved();
    }

    public void assertOpenedContentTitle(String title) {
        waitForElementPresent(CONTENT_CLOSE, "Bookmark did not open the content reader: " + title, 10);
        assertContentTitleVisible(title);
    }

    public void assertOpenedAtSlide(String title, String slideText) {
        assertContentTitleVisible(title);
        waitForElementPresent(slideText(slideText), "Content did not open at the saved slide: " + slideText, 10);
    }

    public void assertOpenedAtFirstOrSavedSlide(String title, String firstSlide, String savedSlide) {
        assertContentTitleVisible(title);
        WebElement slide = waitForFirstElementPresent(new String[]{slideText(savedSlide), slideText(firstSlide)},
                "Content did not open at either accepted slide: " + firstSlide + " or " + savedSlide, 10);
        Allure.addAttachment("Opened bookmarked slide: " + title, slide.getText());
    }

    private void assertContentTitleVisible(String title) {
        waitForElementPresent(exactText(title), "Wrong bookmarked content opened: " + title, 10);
    }

    public void finishFromBookmarks() {
        finishFromBookmarks(null);
    }

    public void finishFromBookmarks(String completionAnswer) {
        for (int page = 0; page < 30 && !isElementVisible(FINISH); page++) {
            String before = driver.getPageSource();
            waitForElementAndClick(CONTENT_NEXT,
                    "Content cannot reach FINISH; check the lesson answer fixture", 10);
            createWait(10).withMessage("Content did not advance towards its final slide")
                    .until(ignored -> !before.equals(driver.getPageSource()));
        }
        WebElement finish = waitForElementPresent(FINISH, "FINISH is absent in bookmarked content", 10);
        if (!isElementEnabled(finish)) {
            Assert.assertTrue("FINISH requires an answer; configure the content completion-answer fixture",
                    completionAnswer != null && !completionAnswer.trim().isEmpty());
            waitForElementAndClick(exactText(completionAnswer),
                    "The configured completion answer is absent on the final slide", 10);
        }
        waitForElementAndClick(FINISH, "FINISH did not become enabled after the configured answer", 10);
        waitForElementPresent(BOOKMARKS_TITLE, "FINISH did not return to My Bookmarks", 20);
        Assert.assertFalse("Feedback UI appeared after FINISH from Bookmarks", isElementPresent(FEEDBACK));
    }

    public void assertDailyPlanItemCompleted(String title) {
        if (isElementPresent(BOOKMARKS_TITLE)) backFromBookmarks();
        CoachFlowPageObjectFactory.get(driver).openToday();
        swipeUpToFindFirstVisibleElement(new String[]{dailyPlanCardFor(title)},
                "Completed Daily Plan item is absent: " + title, 8);
        Assert.assertEquals("Completion must identify one current Daily Plan card", 1,
                getAmountElements(dailyPlanCardFor(title)));
        createWait(15).withMessage("Daily Plan item is not marked completed: " + title)
                .until(ignored -> isDailyPlanItemCompleted(title, true));
    }

    public void assertDailyPlanItemNotCompleted(String title) {
        if (isScreenVisible()) backFromBookmarks();
        waitForElementAndClick(TODAY_TAB, "Cannot open Today", 10);
        String card = dailyPlanCardFor(title);
        swipeUpToFindFirstVisibleElement(new String[]{card}, "Lesson fixture is absent: " + title, 8);
        Assert.assertEquals("Completion fixture must identify one current Daily Plan card", 1, getAmountElements(card));
        Assert.assertFalse("Completion fixture lesson is already completed; restore its account snapshot",
                isDailyPlanItemCompleted(title, false));
    }

    protected boolean isDailyPlanItemCompleted(String title, boolean attachEvidence) {
        String completed = completedMarkerFor(title);
        if (isElementVisible(completed)) return true;
        List<WebElement> indicators = driver.findElements(getLocatorByString(dailyPlanCompletionIndicatorFor(title)));
        if (indicators.size() != 1) return false;
        try {
            WebElement indicator = indicators.get(0);
            if (!indicator.isDisplayed()) return false;
            BufferedImage crop = DailyPlanCompletionMarker.crop(driver.getScreenshotAs(OutputType.BYTES),
                    indicator.getRect(), driver.manage().window().getSize().width);
            if (!DailyPlanCompletionMarker.isCompleted(crop)) return false;
            if (attachEvidence) {
                ByteArrayOutputStream png = new ByteArrayOutputStream();
                ImageIO.write(crop, "png", png);
                Allure.addAttachment("Rendered completion tick on " + title, "image/png",
                        new ByteArrayInputStream(png.toByteArray()), "png");
            }
            return true;
        } catch (StaleElementReferenceException ignored) {
            return false; // Reacquire only while the existing explicit wait is polling.
        } catch (IOException error) {
            throw new IllegalStateException("Cannot inspect completion indicator on " + title, error);
        }
    }

    protected String dailyPlanCompletionIndicatorFor(String title) {
        // Observed on both builds: the status indicator immediately follows the title.
        // Its presence alone never proves completion; its rendered tick must match.
        return dailyPlanCardFor(title) + "//XCUIElementTypeStaticText[@name=" + xpathLiteral(title)
                + "]/following-sibling::*[1][self::XCUIElementTypeOther and @visible='true']";
    }

    public void assertPreviousDayItemNotCompleted(String title, String previousDayTitle) {
        selectRecurringDay(previousDayTitle, true);
        assertVisibleDayItemNotCompleted(title,
                "Finishing the saved occurrence also completed the previous day");
    }

    public void assertRecurringPreviousDayFixture(String title, String savedDayTitle,
                                                  String previousDayTitle) throws Exception {
        boolean[] previousOpened = {false};
        RestoringTestAction.run(() -> {
            previousOpened[0] = true;
            selectRecurringDay(previousDayTitle, true);
            assertVisibleDayItemNotCompleted(title,
                    "Recurring fixture previous occurrence is already completed; restore its account snapshot");
        }, () -> {
            if (previousOpened[0]) {
                selectRecurringDay(savedDayTitle, false);
            }
        });
    }

    protected void selectRecurringDay(String expectedDay, boolean previous) {
        waitForElementAndClick(previous ? previousDayControl() : nextDayControl(),
                "Cannot navigate to the configured recurring occurrence", 10);
        waitForElementPresent(exactText(expectedDay), "Configured recurring day did not open", 15);
    }

    private void assertVisibleDayItemNotCompleted(String title, String message) {
        String titleLocator = exactText(title);
        swipeUpToFindFirstVisibleElement(new String[]{titleLocator},
                "Recurring practice is absent on previous day: " + title, 8);
        Assert.assertEquals("Recurring fixture must identify one Daily Plan card", 1,
                getAmountElements(dailyPlanCardFor(title)));
        Assert.assertFalse(message, isDailyPlanItemCompleted(title, false));
    }

    public void assertNoPaywall() {
        Assert.assertFalse("Paywall blocks bookmarked content", isContentPaywallVisible());
    }

    public void assertScreenVisible() {
        waitForElementPresent(BOOKMARKS_TITLE, "My Bookmarks is not visible", 10);
    }

    public boolean isScreenVisible() {
        return isElementVisible(BOOKMARKS_TITLE);
    }

    protected void tapBlankArea(int x, int y) {
        Map<String, Object> tap = new HashMap<>();
        tap.put("x", x);
        tap.put("y", y);
        ((JavascriptExecutor) driver).executeScript("mobile: tap", tap);
    }

    protected String previousDayControl() { return "id:leftSwitcherArrow"; }
    protected String nextDayControl() { return "id:rightSwitcherArrow"; }

    protected String cardImageFor(String title) { return cardFor(title) + "//XCUIElementTypeImage"; }

    protected String cardBookmarkFor(String title) {
        return cardFor(title) + "//*[@visible='true' and @name!='empty bookmark'"
                + " and contains(translate(@name,'BOOKMARK','bookmark'),'bookmark')]";
    }

    protected String completedMarkerFor(String title) {
        return dailyPlanCardFor(title)
                + "//XCUIElementTypeImage[starts-with(@name,'dailyCompletedCheckmark') and @visible='true']";
    }

    protected void swipe(String direction) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("direction", direction);
        ((JavascriptExecutor) driver).executeScript("mobile: swipe", arguments);
    }

    private void moveToSlide(String direction, String expectedText, Boolean expectedSaved) {
        String expected = slideText(expectedText);
        for (int page = 0; page < 30; page++) {
            if (isElementVisible(expected)) return;
            String before = driver.getPageSource();
            navigateOneSlide(direction);
            createWait(10).withMessage("Content slide did not change after navigation")
                    .until(ignored -> !before.equals(driver.getPageSource()));
            if (expectedSaved != null) assertSlideBookmarkState(expectedSaved);
        }
        Assert.fail("Expected content slide was not reachable: " + expectedText);
    }

    private void navigateOneSlide(String direction) {
        String navigation = "left".equals(direction) ? CONTENT_NEXT : CONTENT_PREVIOUS;
        if (navigation != null && isElementVisible(navigation)) {
            waitForElementAndClick(navigation, "Cannot navigate to the expected content slide", 10);
        } else {
            swipe(direction);
        }
    }

    protected String slideText(String text) {
        return "xpath://XCUIElementTypeCollectionView//XCUIElementTypeStaticText[normalize-space(@name)="
                + "normalize-space(" + xpathLiteral(text) + ") and @visible='true']";
    }

    protected String exactText(String text) {
        return "xpath://XCUIElementTypeStaticText[@name=" + xpathLiteral(text) + " and @visible='true']";
    }

    protected String tooltipFor(String text) {
        return exactText(text);
    }

    protected String cardFor(String title) {
        String literal = xpathLiteral(title);
        return "xpath:(//XCUIElementTypeStaticText[@name=" + literal + " and @visible='true']"
                + "/ancestor::*[self::XCUIElementTypeButton or self::XCUIElementTypeCell or self::XCUIElementTypeOther][.//*[@name="
                + literal + "]][1])";
    }

    protected String dailyPlanCardFor(String title) {
        return "xpath://XCUIElementTypeOther[@name='DailyPlanItem' and .//XCUIElementTypeStaticText[@name="
                + xpathLiteral(title) + "]]";
    }

    protected String xpathLiteral(String value) {
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
