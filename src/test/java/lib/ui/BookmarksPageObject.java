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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
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
        CoachFlowPageObjectFactory.get(driver).openExplore();
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

    public void assertOpenedContentCannotBeSaved() {
        Assert.assertFalse("Kegel content exposes an inactive bookmark", isElementPresent(INACTIVE_BOOKMARK));
        Assert.assertFalse("Kegel content exposes an active bookmark", isElementPresent(ACTIVE_BOOKMARK));
    }

    @Step("Close opened content")
    public void closeOpenedContent() {
        waitForElementAndClick(CONTENT_CLOSE, "Cannot close content", 10);
        confirmContentExitIfNeeded();
        waitForFirstElementPresent(new String[]{TODAY_TAB, BOOKMARKS_TITLE},
                "Neither Today nor My Bookmarks returned after closing content", 15);
    }

    public void closeOpenedContentIfPresent() {
        dismissContentPaywallIfPresent();
        if (isElementPresent(CONTENT_EXIT_QUIT)) {
            confirmContentExitIfNeeded();
            waitForFirstElementPresent(new String[]{TODAY_TAB, BOOKMARKS_TITLE},
                    "Neither Today nor My Bookmarks returned after confirming content exit", 15);
            return;
        }
        if (!isElementPresent(CONTENT_CLOSE)
                || (!isElementPresent(INACTIVE_BOOKMARK) && !isElementPresent(ACTIVE_BOOKMARK))) return;
        waitForElementAndClick(CONTENT_CLOSE, "Cannot close interrupted bookmarked content", 10);
        confirmContentExitIfNeeded();
        waitForFirstElementPresent(new String[]{TODAY_TAB, BOOKMARKS_TITLE},
                "Neither Today nor My Bookmarks returned after closing content", 15);
    }

    private void confirmContentExitIfNeeded() {
        if (isElementPresent(CONTENT_EXIT_QUIT)) {
            waitForElementAndClick(CONTENT_EXIT_QUIT, "Cannot confirm leaving bookmarked content", 10);
            createWait(10).withMessage("Content exit dialog stayed visible")
                    .until(ignored -> !isElementVisible(CONTENT_EXIT_QUIT));
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

    public boolean hasCard(String title) {
        return isElementVisible(exactText(title));
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

    public void assertCardGroupAndCategory(String title, String group, String category) {
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
        String heading = group.startsWith("MODULE ") ? module
                : "not(" + module + ") and @name!='Lessons' and @name!='Practices'";
        return cardFor(title) + "/preceding-sibling::XCUIElementTypeStaticText[" + heading + "][1]"
                + "[@visible='true' and @name=" + xpathLiteral(group) + "]";
    }

    public void assertCategoryAbsent(String category) {
        Assert.assertFalse("Empty category must be hidden: " + category, isElementPresent(exactText(category)));
    }

    @Step("Open bookmark card: {title}")
    public void openCard(String title) {
        waitForElementAndClick(exactText(title), "Cannot open bookmark card: " + title, 10);
        createWait(20).withMessage("Bookmark card did not open content").until(ignored ->
                isElementVisible(INACTIVE_BOOKMARK) || isElementVisible(ACTIVE_BOOKMARK)
                        || isContentPaywallVisible());
        if (isContentPaywallVisible()) {
            // Capture the blocking state before fixture cleanup dismisses it.
            Allure.addAttachment("Paywall blocking bookmarked content", "application/xml",
                    TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
            Allure.addAttachment("Paywall blocking bookmarked content", "image/png",
                    new ByteArrayInputStream(driver.getScreenshotAs(OutputType.BYTES)), "png");
            Assert.fail("Subscription paywall blocked bookmarked content: " + title);
        }
    }

    protected boolean isContentPaywallVisible() {
        return isElementVisible(PAYWALL);
    }

    protected abstract void dismissContentPaywallIfPresent();

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
        moveToSlide("left", expectedSlideText, true);
        assertOpenedContentSaved();
    }

    public void swipeLeftToSlide(String expectedSlideText) {
        moveToSlide("left", expectedSlideText, false);
    }

    public void swipeRightAndAssertSaved(String expectedSlideText) {
        moveToSlide("right", expectedSlideText, true);
        assertOpenedContentSaved();
    }

    public void assertOpenedAtSlide(String title, String slideText) {
        waitForElementPresent(exactText(title), "Wrong bookmarked content opened: " + title, 10);
        waitForElementPresent(slideText(slideText), "Content did not open at the saved slide: " + slideText, 10);
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

    private boolean isDailyPlanItemCompleted(String title, boolean attachEvidence) {
        String completed = dailyPlanCardFor(title)
                + "//XCUIElementTypeImage[starts-with(@name,'dailyCompletedCheckmark') and @visible='true']";
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
        waitForElementAndClick("id:leftSwitcherArrow", "Cannot open previous Daily Plan day", 10);
        waitForElementPresent(exactText(previousDayTitle), "Previous Daily Plan day did not open", 15);
        assertVisibleDayItemNotCompleted(title,
                "Finishing the saved occurrence also completed the previous day");
    }

    public void assertRecurringPreviousDayFixture(String title, String savedDayTitle,
                                                  String previousDayTitle) throws Exception {
        boolean[] previousOpened = {false};
        RestoringTestAction.run(() -> {
            waitForElementAndClick("id:leftSwitcherArrow", "Cannot inspect the previous occurrence fixture", 10);
            previousOpened[0] = true;
            waitForElementPresent(exactText(previousDayTitle), "Previous fixture day did not open", 15);
            assertVisibleDayItemNotCompleted(title,
                    "Recurring fixture previous occurrence is already completed; restore its account snapshot");
        }, () -> {
            if (previousOpened[0]) {
                waitForElementAndClick("id:rightSwitcherArrow", "Cannot restore the saved occurrence day", 10);
                waitForElementPresent(exactText(savedDayTitle), "Saved occurrence day did not restore", 15);
            }
        });
    }

    private void assertVisibleDayItemNotCompleted(String title, String message) {
        String titleLocator = exactText(title);
        swipeUpToFindFirstVisibleElement(new String[]{titleLocator},
                "Recurring practice is absent on previous day: " + title, 8);
        String completed = dailyPlanCardFor(title)
                + "//XCUIElementTypeImage[starts-with(@name,'dailyCompletedCheckmark') and @visible='true']";
        Assert.assertFalse(message, isElementVisible(completed));
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

    private void swipe(String direction) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("direction", direction);
        ((JavascriptExecutor) driver).executeScript("mobile: swipe", arguments);
    }

    private void moveToSlide(String direction, String expectedText, boolean verifySaved) {
        String expected = slideText(expectedText);
        for (int page = 0; page < 30; page++) {
            if (isElementVisible(expected)) return;
            String before = driver.getPageSource();
            String navigation = "left".equals(direction) ? CONTENT_NEXT : CONTENT_PREVIOUS;
            if (isElementVisible(navigation)) {
                waitForElementAndClick(navigation, "Cannot navigate to the expected content slide", 10);
            } else {
                swipe(direction);
            }
            createWait(10).withMessage("Content slide did not change after navigation")
                    .until(ignored -> !before.equals(driver.getPageSource()));
            if (verifySaved) assertOpenedContentSaved();
        }
        Assert.fail("Expected content slide was not reachable: " + expectedText);
    }

    protected String slideText(String text) {
        return "xpath://XCUIElementTypeCollectionView//XCUIElementTypeStaticText[normalize-space(@name)="
                + "normalize-space(" + xpathLiteral(text) + ") and @visible='true']";
    }

    protected String exactText(String text) {
        return "xpath://XCUIElementTypeStaticText[@name=" + xpathLiteral(text) + " and @visible='true']";
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
