package lib.ui.android;

import io.qameta.allure.Step;
import lib.ui.ExplorePageObject;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Explore contract for the configurable Android catalog shipped in 1.40.x.
 * The locators intentionally use the resource ids observed in the Android
 * accessibility tree; card titles remain the data-driven selector because
 * individual cards do not expose a unique resource id.
 */
public class AndroidExplorePageObject extends ExplorePageObject {
    private static final String APP_PACKAGE = "com.vamapps.thecoach";
    private static final String ID_PREFIX = APP_PACKAGE + ":id/";

    private static final String EXPLORE_TITLE = "id:" + ID_PREFIX + "tvLabel";
    private static final String CATALOG_CONTAINER = "id:" + ID_PREFIX + "catalogContainer";
    private static final String BROWSE_PROGRAMS_TITLE =
            "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tvActiveTitle' and @text='Browse Programs']";
    private static final String LEGACY_ALL_PROGRAMS_TITLE =
            "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tvActiveTitle' and @text='ALL PROGRAMS']";
    private static final String LEGACY_ACTIVE_PROGRAMS_COLLECTION = "id:" + ID_PREFIX + "rvActivePrograms";
    private static final String COURSES_SECTION_TITLE =
            "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tv_section_title' and @text='COURSES']";

    private static final String QUICK_TIPS_SECTION_TITLE = sectionTitle("Quick Tips");
    private static final String MASTER_CLASSES_SECTION_TITLE = sectionTitle("Master Classes");
    private static final String PRIVATE_COACHING_SECTION_TITLE = sectionTitle("Private coaching session");

    private static final String QUICK_TIPS_CARDS = sectionCards("Quick Tips") + "//androidx.cardview.widget.CardView";
    private static final String MASTER_CLASSES_CARDS = sectionCards("Master Classes") + "//android.widget.LinearLayout[@clickable='true']";
    private static final String PRIVATE_COACHING_CARDS = sectionCards("Private coaching session") + "//androidx.cardview.widget.CardView";

    private static final String FIRST_QUICK_TIP_CARD = first(QUICK_TIPS_CARDS);
    private static final String FIRST_MASTER_CLASS_CARD =
            "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tv_title' and @text='Sensual Massage Masterclass']/../..";
    private static final String MASTER_CLASS_BANNER = "id:" + ID_PREFIX + "iv_banner";
    private static final String PRIVATE_COACHING_AVATAR = "id:" + ID_PREFIX + "iv_avatar";
    private static final String PRIVATE_COACHING_JOIN_BUTTON = "id:" + ID_PREFIX + "btn_join";

    private static final String CARD_TITLE_NODES =
            "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tv_title']";
    private static final String EXPLORE_WEBVIEW = "xpath://android.webkit.WebView";
    private static final String PRIVATE_COACHING_WEBVIEW =
            "xpath://android.webkit.WebView[contains(@text,'Part of a Last Longer')]";
    private static final String PRIVATE_COACHING_BOOK_BUTTON =
            "xpath://android.view.View[@content-desc='Book 1:1 a private consultation']";
    private static final String DETAIL_CLOSE_BUTTON = "id:" + ID_PREFIX + "btnClose";
    private static final String DETAIL_NAVIGATE_UP_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
    private static final String DETAIL_ACTION_BUTTON = "id:" + ID_PREFIX + "btnAction";

    static {
        TAB_EXPLORE = "id:" + ID_PREFIX + "nav_graph_explore";
        SELECTED_EXPLORE_TAB =
                "xpath://android.widget.FrameLayout[@resource-id='" + ID_PREFIX + "nav_graph_explore' and @selected='true']";
        EXPLORE_ENTRY_POINT = EXPLORE_TITLE;

        // These fields are not used by the Android-specific contract methods,
        // but are initialized to keep the shared ExplorePageObject safe for
        // callers that inspect the common navigation state.
        TAB_PROGRAMS = "xpath://android.widget.FrameLayout[@content-desc='Programs']";
        TAB_TOOLS = "xpath://android.widget.FrameLayout[@content-desc='Tools']";
        RECOMMENDED_SECTION = BROWSE_PROGRAMS_TITLE;
        RECOMMENDED_COLLECTION = LEGACY_ACTIVE_PROGRAMS_COLLECTION;
        RECOMMENDED_PROGRAM_CARDS = LEGACY_ACTIVE_PROGRAMS_COLLECTION + "//android.view.ViewGroup[@clickable='true']";
        SEXUAL_HEALTH_SECTION = null;
        COURSES_SECTION = COURSES_SECTION_TITLE;
        COURSES_COLLECTION = null;
        COURSE_CARDS = null;
        BODY_PRACTICES_SECTION = null;
        BODY_PRACTICES_COLLECTION = null;
        BODY_PRACTICE_CARDS = null;
        FIRST_BODY_PRACTICE_CARD = null;
        MIND_PRACTICES_SECTION = null;
        MIND_PRACTICES_COLLECTION = null;
        MIND_PRACTICE_CARDS = null;
        FIRST_MIND_PRACTICE_CARD = null;
        RETIRED_MAIN_PROGRAMS_SECTION = LEGACY_ALL_PROGRAMS_TITLE;
        RETIRED_MORE_TOOLS_SECTION = "xpath://android.widget.TextView[@text='More tools']";
        COMPLETED_PROGRAMS_SECTION = "xpath://android.widget.TextView[@text='COMPLETED PROGRAMS']";
        COMING_SOON_SECTION = "xpath://android.widget.TextView[@text='COMING SOON']";
        FIRST_PROGRAM_CARD = null;
        FIRST_PROGRAM_CARD_IMAGE = null;
        FIRST_PROGRAM_TITLE = null;
        LAST_RECOMMENDED_PROGRAM_CARD = null;
        LAST_RECOMMENDED_PROGRAM_TITLE = null;
        PROGRAM_DETAIL_TITLE = null;
        PROGRAM_DETAIL_CONTENT = null;
        PROGRAM_DETAIL_CLOSE_BUTTON = DETAIL_CLOSE_BUTTON;
        FIRST_COURSE_CARD = null;
        FIRST_COURSE_CARD_IMAGE = null;
        FIRST_COURSE_TITLE = null;
        LAST_COURSE_CARD = null;
        LAST_COURSE_TITLE = null;
        COURSE_DETAIL_TITLE = null;
        COURSE_DETAIL_TYPE = null;
        COURSE_DETAIL_CONTENT = null;
        COURSE_DETAIL_BACK_BUTTON = DETAIL_CLOSE_BUTTON;
        CUSTOM_KEGEL_CARD = null;
        CUSTOM_KEGEL_TITLE = null;
        CUSTOM_KEGEL_START_BUTTON = null;
        CUSTOM_KEGEL_CLOSE_BUTTON = DETAIL_CLOSE_BUTTON;
        LAST_BODY_PRACTICE_CARD = null;
        LAST_BODY_PRACTICE_TITLE = null;
        LAST_BODY_PRACTICE_DETAIL_TITLE = null;
        LAST_MIND_PRACTICE_CARD = null;
        LAST_MIND_PRACTICE_TITLE = null;
        LAST_MIND_PRACTICE_DETAIL_TITLE = null;
        PRACTICE_DETAIL_HEADER = null;
        PRACTICE_DETAIL_GOAL = null;
        PRACTICE_DETAIL_START_BUTTON = null;
        CONCEPT_POPUP_CONFIRM_BUTTON = null;
    }

    public AndroidExplorePageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    public boolean isExploreContextAvailable() {
        return isElementPresent(EXPLORE_TITLE) || isElementPresent(CATALOG_CONTAINER);
    }

    @Override
    @Step("Open Android Explore tab")
    public void openExploreTab() {
        closeTransientDetailIfPresent();
        if (!isElementPresent(EXPLORE_TITLE)) {
            waitForElementAndClick(TAB_EXPLORE, "Cannot tap Android Explore tab", 15);
        }
        waitForElementPresent(EXPLORE_TITLE, "Android Explore title is not displayed", 20);
        waitForElementPresent(CATALOG_CONTAINER, "Android Explore catalog did not load", 20);
        scrollUpToTop();
        waitForElementPresent(
                QUICK_TIPS_SECTION_TITLE,
                "Android Explore remote-configured sections did not load",
                30
        );
    }

    /**
     * A failed navigation assertion can leave the next test in a detail
     * bottom sheet/WebView while the app itself remains authenticated.
     * Close that transient surface before the shared auth setup evaluates the
     * current screen.
     */
    public void closeTransientDetailIfPresent() {
        if (isElementPresent(EXPLORE_TITLE)) {
            return;
        }

        if (isElementPresent(DETAIL_CLOSE_BUTTON)) {
            waitForElementAndClick(
                    DETAIL_CLOSE_BUTTON,
                    "Cannot close the previous Android Explore WebView",
                    10
            );
            return;
        }

        if (isElementPresent(DETAIL_NAVIGATE_UP_BUTTON)) {
            waitForElementAndClick(
                    DETAIL_NAVIGATE_UP_BUTTON,
                    "Cannot navigate back from the previous Android Explore detail",
                    10
            );
            return;
        }

        if (isElementPresent(DETAIL_ACTION_BUTTON)) {
            waitForElementAndClick(
                    DETAIL_ACTION_BUTTON,
                    "Cannot close the previous Android Explore detail",
                    10
            );
        }
    }

    @Step("Verify required configurable Explore sections")
    public void assertConfiguredSectionsAreDisplayed() {
        waitForElementPresent(EXPLORE_TITLE, "Explore title is not displayed", 10);
        waitForElementPresent(QUICK_TIPS_SECTION_TITLE, "Quick Tips section is not displayed", 10);
        scrollDownUntil(MASTER_CLASSES_SECTION_TITLE, 5);
        waitForElementPresent(MASTER_CLASSES_SECTION_TITLE, "Master Classes section is not displayed", 10);
        scrollDownUntil(PRIVATE_COACHING_SECTION_TITLE, 5);
        waitForElementPresent(PRIVATE_COACHING_SECTION_TITLE, "Private coaching session section is not displayed", 10);
    }

    @Step("Verify Browse Programs replaces the legacy active-program block")
    public void assertBrowseProgramsReplacesLegacyBlock() {
        waitForElementPresent(BROWSE_PROGRAMS_TITLE, "Explore must show the configured Browse Programs title", 10);
        Assert.assertFalse(
                "Legacy ALL PROGRAMS title must not be displayed",
                isElementPresent(LEGACY_ALL_PROGRAMS_TITLE)
        );
        Assert.assertFalse(
                "Legacy active-program RecyclerView must not be displayed",
                isElementPresent(LEGACY_ACTIVE_PROGRAMS_COLLECTION)
        );
    }

    @Step("Verify Courses section is removed")
    public void assertCoursesSectionIsRemoved() {
        Assert.assertFalse("Courses section must not be displayed", isElementPresent(COURSES_SECTION_TITLE));
    }

    @Step("Verify configured card templates and titles")
    public void assertConfiguredCardTemplates() {
        waitForElementPresent(QUICK_TIPS_CARDS, "Quick Tips must contain square cards", 10);
        int quickTipsCardCount = getAmountElements(QUICK_TIPS_CARDS);
        Assert.assertTrue("Quick Tips must contain at least one square card", quickTipsCardCount > 0);

        scrollDownUntil(MASTER_CLASSES_CARDS, 5);
        waitForElementPresent(MASTER_CLASSES_CARDS, "Master Classes must contain challenge cards", 10);
        int masterClassCardCount = getAmountElements(MASTER_CLASSES_CARDS);
        Assert.assertTrue("Master Classes must contain at least one challenge card", masterClassCardCount > 0);

        scrollDownUntil(PRIVATE_COACHING_JOIN_BUTTON, 5);
        waitForElementPresent(PRIVATE_COACHING_JOIN_BUTTON, "Private coaching card must contain Learn More", 10);
        int privateCoachingCardCount = getAmountElements(PRIVATE_COACHING_CARDS);
        Assert.assertTrue("Private coaching must contain at least one coaching card", privateCoachingCardCount > 0);

        WebElement masterBanner = waitForElementPresent(MASTER_CLASS_BANNER, "Challenge card has no banner image", 10);
        Assert.assertTrue("Challenge card banner must have a visible area", masterBanner.getSize().getWidth() > 0 && masterBanner.getSize().getHeight() > 0);
        WebElement coachingAvatar = waitForElementPresent(PRIVATE_COACHING_AVATAR, "Coaching card has no avatar image", 10);
        Assert.assertTrue("Coaching card avatar must have a visible area", coachingAvatar.getSize().getWidth() > 0 && coachingAvatar.getSize().getHeight() > 0);
    }

    @Step("Verify card titles are available for analytics")
    public void assertConfiguredCardTitlesAreNonEmpty() {
        scrollDownUntil(MASTER_CLASSES_SECTION_TITLE, 5);
        List<WebElement> titles = driver.findElements(By.xpath("//android.widget.TextView[@resource-id='" + ID_PREFIX + "tv_title']"));
        Assert.assertFalse("Configured cards must expose at least one title", titles.isEmpty());
        for (WebElement title : titles) {
            Assert.assertFalse("Configured card title must not be empty", title.getText().trim().isEmpty());
        }
    }

    @Step("Open a Quick Tips card and verify lesson/practice navigation")
    public void openQuickTipAndVerifyDestination() {
        scrollUpToTop();
        WebElement card = waitForElementPresent(FIRST_QUICK_TIP_CARD, "Quick Tips card is not displayed", 10);
        card.click();
        waitForExploreDestinationAfterCardTap(
                "Quick Tips card must open a lesson, practice, or WebView after one tap"
        );
    }

    @Step("Open a Master Class card and verify lesson/WebView navigation")
    public void openMasterClassAndVerifyDestination() {
        scrollDownUntil(FIRST_MASTER_CLASS_CARD, 5);
        WebElement card = waitForElementPresent(FIRST_MASTER_CLASS_CARD, "Master Classes card is not displayed", 10);
        card.click();
        waitForExploreDestinationAfterCardTap(
                "Master Classes card must open a lesson or WebView after one tap"
        );
    }

    @Step("Open private coaching card and verify WebView")
    public void openPrivateCoachingAndVerifyWebView() {
        scrollDownUntil(PRIVATE_COACHING_JOIN_BUTTON, 6);
        // The Android Button is exposed as visible/enabled in the hierarchy,
        // but Appium's generic isEnabled() can briefly report false while the
        // horizontal RecyclerView finishes binding. Wait for visibility, then
        // use the semantic element click.
        WebElement joinButton = waitForElementPresent(
                PRIVATE_COACHING_JOIN_BUTTON,
                "Cannot find Private coaching session action",
                10
        );
        joinButton.click();
        waitForElementPresent(EXPLORE_WEBVIEW, "Private coaching card did not open a WebView", 15);
        waitForElementPresent(PRIVATE_COACHING_WEBVIEW, "Private coaching WebView content is not displayed", 15);
        waitForElementPresent(PRIVATE_COACHING_BOOK_BUTTON, "Private coaching WebView booking action is not displayed", 15);
    }

    @Step("Close private coaching WebView")
    public void closePrivateCoachingWebView() {
        waitForElementAndClick(DETAIL_CLOSE_BUTTON, "Cannot close private coaching WebView", 10);
        waitForElementPresent(EXPLORE_TITLE, "Explore did not return after closing private coaching WebView", 10);
    }

    private void waitForExploreDestinationAfterCardTap(String errorMessage) {
        waitForElementNotPresent(
                EXPLORE_TITLE,
                errorMessage + ": card left the user on Explore",
                15
        );
        waitForFirstElementPresent(
                new String[]{
                        EXPLORE_WEBVIEW,
                        "xpath://android.widget.TextView[contains(@text,'START EXERCISE')]",
                        "xpath://android.widget.TextView[contains(@text,'START PRACTICE')]",
                        "id:" + ID_PREFIX + "lesson_screen",
                        "id:" + ID_PREFIX + "practice_screen"
                },
                errorMessage,
                15
        );
    }

    private void scrollDownUntil(String locator, int maxSwipes) {
        int swipeCount = 0;
        while (!isElementPresent(locator) && swipeCount < maxSwipes) {
            if (!scrollExplore("down")) {
                break;
            }
            swipeCount++;
        }
        waitForElementPresent(locator, "Cannot find Explore element after scrolling: " + locator, 10);
    }

    private void scrollUpToTop() {
        for (int index = 0; index < 5; index++) {
            if (!scrollExplore("up")) {
                return;
            }
        }
    }

    private boolean scrollExplore(String direction) {
        List<WebElement> scrollViews = driver.findElements(By.className("android.widget.ScrollView"));
        for (WebElement scrollView : scrollViews) {
            try {
                Map<String, Object> args = new HashMap<String, Object>();
                args.put("elementId", ((RemoteWebElement) scrollView).getId());
                args.put("direction", direction);
                args.put("percent", 0.8);
                Object result = ((JavascriptExecutor) driver).executeScript("mobile: scrollGesture", args);
                return !(result instanceof Boolean) || (Boolean) result;
            } catch (WebDriverException ignored) {
                // Retry through the next visible ScrollView if the hierarchy
                // was re-rendered between locating and scrolling.
            }
        }
        return false;
    }

    private static String sectionTitle(String title) {
        return "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tv_section_title' and @text='" + title + "']";
    }

    private static String sectionCards(String title) {
        return "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tv_section_title' and @text='" + title + "']/following-sibling::androidx.recyclerview.widget.RecyclerView[@resource-id='" + ID_PREFIX + "rv_horizontal_cards']";
    }

    private static String first(String xpathLocator) {
        return "xpath:(" + xpathLocator.substring("xpath:".length()) + ")[1]";
    }
}
