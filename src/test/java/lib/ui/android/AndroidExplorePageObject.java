package lib.ui.android;

import io.qameta.allure.Step;
import lib.AppearanceSnapshot;
import lib.DailyPlanCompletionMarker;
import lib.Platform;
import lib.ui.ExplorePageObject;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Explore contract for the configurable Android catalog shipped in 1.40.x.
 * The locators intentionally use the resource ids observed in the Android
 * accessibility tree; card titles remain the data-driven selector because
 * individual cards do not expose a unique resource id.
 */
public class AndroidExplorePageObject extends ExplorePageObject {
    private static final String APP_PACKAGE = Platform.getInstance().getAndroidAppPackage();
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
    private static final String EXPLORE_TAB_WEBVIEW = "id:" + ID_PREFIX + "webview_container";
    private static final String VIDEO_PLAYER = "id:" + ID_PREFIX + "viewPlayer";
    private static final String VIDEO_PLAY_PAUSE_BUTTON = "id:" + ID_PREFIX + "exo_play_pause";
    private static final String TODAY_TAB = "id:" + ID_PREFIX + "nav_graph_daily";
    private static final String PRIVATE_COACHING_WEBVIEW =
            "xpath://android.webkit.WebView[contains(@text,'Part of a Last Longer')]";
    private static final String PRIVATE_COACHING_BOOK_BUTTON =
            "xpath://android.view.View[@content-desc='Book 1:1 a private consultation']";
    private static final String DETAIL_CLOSE_BUTTON = "id:" + ID_PREFIX + "btnClose";
    private static final String DETAIL_NAVIGATE_UP_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
    private static final String DETAIL_ACTION_BUTTON = "id:" + ID_PREFIX + "btnAction";
    private static final String EXIT_ACTIVITY_DIALOG = "id:" + ID_PREFIX + "tvDialogtitle";
    private static final String EXIT_ACTIVITY_CONFIRM_BUTTON = "id:" + ID_PREFIX + "btnYes";

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

    private static final String PROGRAM_TITLES = "xpath://*[@resource-id='"+ID_PREFIX+"rvActivePrograms']//*[@resource-id='"+ID_PREFIX+"tvTitle']";
    private boolean carousel(String locator,String direction) {
        return carousel(locator, direction, .85);
    }

    private boolean carousel(String locator, String direction, double fraction) {
        WebElement collection=waitForElementVisible(locator,"Explore carousel absent",10);
        Map<String,Object> args=new HashMap<String,Object>();
        args.put("elementId",((RemoteWebElement)collection).getId());args.put("direction",direction);args.put("percent",fraction);
        boolean more=Boolean.TRUE.equals(((JavascriptExecutor)driver).executeScript("mobile: scrollGesture",args));
        final String[] previous={null};
        createWait(10).until(d -> {
            String current = visibleProgramViewport();
            boolean stable = current.equals(previous[0]); previous[0] = current; return stable;
        });
        return more;
    }

    private String visibleProgramViewport() {
        StringBuilder viewport = new StringBuilder();
        for (WebElement title : driver.findElements(getLocatorByString(PROGRAM_TITLES))) {
            viewport.append(title.getText()).append(title.getRect());
        }
        return viewport.toString();
    }
    @Override public List<String> getProgramNamesFromExplore() {
        scrollUpToTop();waitForElementVisible(BROWSE_PROGRAMS_TITLE,"Browse Programs absent",10);
        // A recycled horizontal carousel retains its previous offset between tab visits.
        for(int n=0;n<15;n++) {
            if(!carousel(LEGACY_ACTIVE_PROGRAMS_COLLECTION,"left")) break;
        }
        java.util.Set<String> names=new java.util.LinkedHashSet<String>();
        java.util.List<String> before=new java.util.ArrayList<String>();
        for(int n=0;n<30;n++) {
            java.util.List<String> visible=getElementAccessibleNames(PROGRAM_TITLES);
            Assert.assertFalse("Browse Programs has no program names",visible.isEmpty());
            names.addAll(visible);
            if(n>0&&visible.equals(before))return new java.util.ArrayList<String>(names);
            before=visible;carousel(LEGACY_ACTIVE_PROGRAMS_COLLECTION,"right");
        }
        throw new AssertionError("Explore program list did not reach its end");
    }
    @Override public void assertCurrentSectionsAreDisplayed() { super.assertCurrentSectionsAreDisplayed(); }
    @Override public void assertRemovedCoursesAndPracticesAreAbsent() {
        assertAbsentAcrossExplore(this::assertRetiredSectionHeadingsAbsent);
    }
    @Override public void assertActiveProgramBlockIsAbsent() {
        waitForElementVisible(CATALOG_CONTAINER, "Explore catalog is not displayed", 10);
        String active = "xpath://android.widget.TextView[translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ',"
                + "'abcdefghijklmnopqrstuvwxyz')='active program' or @text='Активная программа']";
        assertAbsentAcrossExplore(() -> Assert.assertFalse("Active Program is visible on Explore", isElementVisible(active)));
    }
    private void assertAbsentAcrossExplore(Runnable assertion) {
        scrollUpToTop();
        for(int n=0;n<20;n++) {
            assertion.run();
            if(!scrollExplore("down")) {
                assertion.run();scrollUpToTop();return;
            }
        }
        throw new AssertionError("Explore bottom was not reached; absence is not proven");
    }
    private void assertRetiredSectionHeadingsAbsent() {
        for(String heading:new String[]{"Courses","Body Practices","Mind Practices"})
            Assert.assertFalse("Retired Explore section reappeared: "+heading,isElementVisible(sectionTitle(heading)));
    }
    @Override public void assertRetiredExploreUiIsAbsent() {
        assertBrowseProgramsReplacesLegacyBlock();
        Assert.assertFalse("Legacy Tools tab is displayed",isElementVisible(TAB_TOOLS));
        Assert.assertFalse("Legacy Programs tab is displayed",isElementVisible(TAB_PROGRAMS));
    }
    @Override public void assertMainProgramsRecommendedSection() {
        scrollUpToTop();waitForElementVisible(BROWSE_PROGRAMS_TITLE,"Browse Programs absent",10);
        assertBrowseProgramCardsPresent();
    }

    private void assertBrowseProgramCardsPresent() {
        Assert.assertFalse("Browse Programs has no cards",getElementAccessibleNames(PROGRAM_TITLES).isEmpty());
        WebElement image=waitForElementVisible("xpath://*[@resource-id='"+ID_PREFIX+"rvActivePrograms']//*[@resource-id='"+ID_PREFIX+"ivMainImage']","Program image absent",10);
        Assert.assertTrue("Program image is empty",image.getRect().width>0&&image.getRect().height>0);
    }
    private void openLast(String collection,String titles) {
        java.util.List<String> before=new java.util.ArrayList<String>();
        for(int n=0;n<30;n++) {
            java.util.List<String> names=getElementAccessibleNames(titles);
            Assert.assertFalse("Carousel has no named cards",names.isEmpty());
            if(n>0&&names.equals(before)) {
                java.util.List<WebElement> elements=driver.findElements(getLocatorByString(titles));
                WebElement last=elements.get(elements.size()-1);String name=last.getText();last.click();
                assertDestination(name);return;
            }
            before=names;carousel(collection,"right");
        }
        Assert.fail("Carousel end was not reached");
    }
    private static String xpathValue(String value) {
        if(!value.contains("'"))return "'"+value+"'";
        if(!value.contains("\""))return "\""+value+"\"";
        return "concat('"+value.replace("'", "',\"'\",'")+"')";
    }
    private void assertDestination(String title) {
        waitForElementNotVisible(BROWSE_PROGRAMS_TITLE,"Card did not leave Explore",15);
        waitForElementVisible("xpath://*[@text="+xpathValue(title)+"]","Destination title differs from selected card",15);
        waitForFirstElementPresent(new String[]{"id:"+ID_PREFIX+"rvQuestions","id:"+ID_PREFIX+"rvProgram","id:"+ID_PREFIX+"tvGoalBody","id:"+ID_PREFIX+"rvLessonDetail","xpath://android.webkit.WebView"},"Destination content is absent",15);
    }
    @Override public void openFirstRecommendedProgramAndVerifyDetails() {
        assertMainProgramsRecommendedSection();WebElement title=waitForElementVisible(PROGRAM_TITLES,"Program card absent",10);
        String name=title.getText();title.click();assertDestination(name);
    }
    @Override public void swipeToAndOpenLastRecommendedProgram() {
        assertMainProgramsRecommendedSection();openLast(LEGACY_ACTIVE_PROGRAMS_COLLECTION,PROGRAM_TITLES);
    }
    @Override public void closeProgramDetails() {closeAndroidDetail();}
    @Override public void closeCourseDetails() {closeAndroidDetail();}
    @Override public void closePracticeDetails() {closeAndroidDetail();}
    @Override public void closeCustomKegel() {closeAndroidDetail();}
    private void closeAndroidDetail() {
        waitForFirstElementAndClick(new String[]{DETAIL_NAVIGATE_UP_BUTTON,DETAIL_CLOSE_BUTTON},"Cannot close Explore detail",10);
        if(isElementVisible(EXIT_ACTIVITY_DIALOG))waitForElementAndClick(EXIT_ACTIVITY_CONFIRM_BUTTON,"Cannot quit unfinished activity",10);
        waitForElementVisible(EXPLORE_TITLE,"Explore did not return",15);
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
        waitForFirstElementPresent(new String[]{EXPLORE_TITLE, EXPLORE_TAB_WEBVIEW},
                "Android Explore did not open", 20);
        closeExploreTabWebViewIfPresent();
        waitForElementPresent(EXPLORE_TITLE, "Android Explore title is not displayed", 20);
        waitForElementPresent(CATALOG_CONTAINER, "Android Explore catalog did not load", 20);
        scrollUpToTop();
        waitForFirstElementPresent(
                new String[]{BROWSE_PROGRAMS_TITLE, PROGRAM_TITLES},
                "Android Explore program catalog did not load",
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
        closeExploreTabWebViewIfPresent();
        if (isElementPresent(EXPLORE_TITLE)) {
            return;
        }

        if (isElementPresent(VIDEO_PLAYER)) {
            closeVideoPlayer();
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

        // The Today screen also exposes btnAction, but there it is the
        // Profile button. Only use this legacy detail action when the bottom
        // navigation is absent, which is how the Android detail screen is
        // exposed in the current accessibility tree.
        if (isElementPresent(DETAIL_ACTION_BUTTON)
                && !isElementPresent("id:" + ID_PREFIX + "nav_graph_daily")
                && !isElementPresent("id:" + ID_PREFIX + "nav_graph_explore")
                && !isElementPresent("id:" + ID_PREFIX + "nav_graph_shop")) {
            waitForElementAndClick(
                    DETAIL_ACTION_BUTTON,
                    "Cannot close the previous Android Explore detail",
                    10
            );
        }
    }

    private boolean closeExploreTabWebViewIfPresent() {
        if (isElementVisible(SELECTED_EXPLORE_TAB) && isElementVisible(EXPLORE_TAB_WEBVIEW)) {
            // This destination is pushed onto Explore's own back stack and has no Close control.
            driver.navigate().back();
            waitForElementNotVisible(EXPLORE_TAB_WEBVIEW, "Android Explore WebView did not close", 10);
            return true;
        }
        return false;
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
        // manProd 1.40.21 reuses this resource for the new Browse Programs carousel.
        waitForElementVisible(LEGACY_ACTIVE_PROGRAMS_COLLECTION, "Browse Programs carousel is absent", 10);
    }

    @Step("Verify Courses section is removed")
    public void assertCoursesSectionIsRemoved() {
        assertAbsentAcrossExplore(() -> Assert.assertFalse("Courses is visible on Explore",
                isElementVisible(sectionTitle("Courses"))));
    }

    @Step("Verify readable title, description and details on each Browse Programs card")
    public void assertBrowseProgramsCardsAreReadable() {
        scrollUpToTop();
        assertBrowseProgramCardsPresent();
        boolean atStart = false;
        for (int swipe = 0; swipe < 15; swipe++) {
            String before = visibleProgramViewport();
            if (!carousel(LEGACY_ACTIVE_PROGRAMS_COLLECTION, "left") && before.equals(visibleProgramViewport())) {
                atStart = true;
                break;
            }
        }
        Assert.assertTrue("Browse Programs did not return to its first card", atStart);
        By cards = By.xpath("//*[@resource-id='" + ID_PREFIX + "rvActivePrograms']/android.view.ViewGroup");
        List<WebElement> firstViewport = driver.findElements(cards);
        Assert.assertFalse("Browse Programs has no rendered cards", firstViewport.isEmpty());
        int fullCardWidth = firstViewport.get(0).getRect().width;
        int viewportWidth = waitForElementVisible(LEGACY_ACTIVE_PROGRAMS_COLLECTION,
                "Browse Programs carousel is absent", 10).getRect().width;
        // Keep each move within the range in which the next card can fit completely.
        double fraction = Math.max(.08, Math.min(.28, (viewportWidth - fullCardWidth) / (double) viewportWidth));
        Set<String> seen = new LinkedHashSet<>();
        Set<String> checked = new LinkedHashSet<>();
        String previousViewport = null;
        boolean endConfirmation = false;
        boolean canScrollFurther = true;
        for (int swipe = 0; swipe < 64; swipe++) {
            StringBuilder viewport = new StringBuilder();
            for (WebElement card : driver.findElements(cards)) {
                List<WebElement> titles = card.findElements(By.id(ID_PREFIX + "tvTitle"));
                String name = titles.isEmpty() ? "" : titles.get(0).getText();
                Rectangle box = card.getRect();
                viewport.append(name).append(box);
                if (!name.trim().isEmpty()) seen.add(name);
                if (box.width < fullCardWidth - 2) continue; // Adjacent cards are intentionally clipped by the carousel.
                WebElement title = card.findElement(By.id(ID_PREFIX + "tvTitle"));
                if (!checked.add(title.getText())) continue;
                List<WebElement> fields = new ArrayList<>();
                fields.add(title);
                fields.add(card.findElement(By.id(ID_PREFIX + "tvDescription")));
                for (String optional : new String[]{"tvTimeText", "tvExcerciceText"}) {
                    for (WebElement field : card.findElements(By.id(ID_PREFIX + optional))) {
                        if (field.isDisplayed()) fields.add(field);
                    }
                }
                assertCardFieldsFit(box, fields.toArray(new WebElement[0]));
            }
            String currentViewport = viewport.toString();
            boolean unchanged = currentViewport.equals(previousViewport);
            if (unchanged && endConfirmation && !canScrollFurther) {
                Assert.assertTrue("Browse Programs must display multiple readable cards", checked.size() > 1);
                Assert.assertEquals("Not every visible card was verified at full width", seen, checked);
                return;
            }
            // UiAutomator can report false while the last item is still only partly on screen.
            // Confirm a stationary viewport with one larger gesture before accepting the end.
            endConfirmation = unchanged;
            previousViewport = currentViewport;
            canScrollFurther = carousel(LEGACY_ACTIVE_PROGRAMS_COLLECTION, "right", endConfirmation ? .85 : fraction);
        }
        Assert.fail("Browse Programs carousel did not reach its last card; observed " + seen);
    }

    @Step("Verify the configured coaching card shows its image, title, expert and action")
    public void assertCoachingCardIsReadable() throws IOException {
        String title = System.getProperty("coach.explore.coaching.title", "Relationships: 1:1 Session");
        String cardLocator = "xpath://*[@resource-id='" + ID_PREFIX + "catalogContainer']"
                + "//androidx.cardview.widget.CardView[.//*[@resource-id='" + ID_PREFIX + "tv_title'"
                + " and @text=" + xpathValue(title) + "]]";
        scrollDownUntil(cardLocator, 8);
        WebElement card = waitForElementVisible(cardLocator, "Configured coaching card is absent", 10);
        Assert.assertEquals("The coaching title must identify one card", 1, getAmountElements(cardLocator));
        WebElement author = card.findElement(By.id(ID_PREFIX + "tv_doctor"));
        WebElement join = card.findElement(By.id(ID_PREFIX + "btn_join"));
        Assert.assertEquals("The reviewed coaching expert text differs",
                System.getProperty("coach.explore.coaching.expert", "Connell Barrett"), author.getText());
        Assert.assertEquals("The coaching action differs",
                System.getProperty("coach.explore.coaching.action", "JOIN"), join.getText());
        Assert.assertTrue("The coaching action is disabled", join.isEnabled());
        assertCardFieldsFit(card.getRect(), card.findElement(By.id(ID_PREFIX + "tv_title")), author, join);
        WebElement image = card.findElement(By.id(ID_PREFIX + "iv_avatar"));
        Assert.assertTrue("Coaching card image is absent", image.isDisplayed());
        Assert.assertTrue("Coaching card image has not rendered", AppearanceSnapshot.hasVisibleContrast(
                DailyPlanCompletionMarker.crop(driver.getScreenshotAs(OutputType.BYTES), image.getRect(),
                        driver.manage().window().getSize().width)));
    }

    private void assertCardFieldsFit(Rectangle card, WebElement... fields) {
        List<Rectangle> occupied = new ArrayList<>();
        for (WebElement field : fields) {
            Rectangle bounds = field.getRect();
            Assert.assertTrue("Card text or control is empty", field.isDisplayed()
                    && !field.getText().trim().isEmpty() && bounds.width > 0 && bounds.height > 0);
            Assert.assertTrue("Card content extends outside its card", bounds.x >= card.x && bounds.y >= card.y
                    && bounds.x + bounds.width <= card.x + card.width && bounds.y + bounds.height <= card.y + card.height);
            for (Rectangle previous : occupied) {
                Assert.assertFalse("Card content overlaps", bounds.x < previous.x + previous.width
                        && previous.x < bounds.x + bounds.width && bounds.y < previous.y + previous.height
                        && previous.y < bounds.y + bounds.height);
            }
            occupied.add(bounds);
        }
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

    @Step("Open the Quick Tips video card and verify the Android video player")
    public void openQuickTipVideoAndVerifyPlayer() {
        scrollUpToTop();
        WebElement card = waitForElementPresent(FIRST_QUICK_TIP_CARD, "Quick Tips card is not displayed", 10);
        card.click();
        waitForElementPresent(VIDEO_PLAYER, "Quick Tips video card did not open the Android video player", 20);
        assertVideoPlayerControlsAreDisplayed();
    }

    @Step("Verify Android video player controls")
    public void assertVideoPlayerControlsAreDisplayed() {
        WebElement playPause = waitForElementPresent(
                VIDEO_PLAY_PAUSE_BUTTON,
                "Android video player play/pause control is not displayed",
                10
        );
        Assert.assertTrue("Android video player play/pause control must be enabled", playPause.isEnabled());
        Assert.assertTrue(
                "Android video player play/pause control must expose an accessibility label",
                playPause.getAttribute("contentDescription") != null
                        && !playPause.getAttribute("contentDescription").trim().isEmpty()
        );
        WebElement navigateUp = waitForElementPresent(
                DETAIL_NAVIGATE_UP_BUTTON,
                "Android video player back control is not displayed",
                10
        );
        Assert.assertTrue("Android video player back control must be enabled", navigateUp.isEnabled());
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
        String configuredTitle = System.getProperty("coach.explore.webview.title");
        String cardAction = PRIVATE_COACHING_JOIN_BUTTON;
        String heading = PRIVATE_COACHING_WEBVIEW;
        String booking = PRIVATE_COACHING_BOOK_BUTTON;
        if (configuredTitle == null) {
            scrollDownUntil(cardAction, 6);
        } else {
            heading = webViewText(requiredWebViewFixture("heading"));
            booking = webViewText(requiredWebViewFixture("action"));
            cardAction = configuredWebViewCardAction(configuredTitle);
        }
        // The Android Button is exposed as visible/enabled in the hierarchy,
        // but Appium's generic isEnabled() can briefly report false while the
        // horizontal RecyclerView finishes binding. Wait for visibility, then
        // use the semantic element click.
        WebElement joinButton = waitForElementPresent(
                cardAction,
                "Cannot find Private coaching session action",
                10
        );
        joinButton.click();
        waitForElementPresent(EXPLORE_WEBVIEW, "Private coaching card did not open a WebView", 15);
        waitForElementPresent(heading, "Private coaching WebView content is not displayed", 15);
        waitForElementPresent(booking, "Private coaching WebView booking action is not displayed", 15);
    }

    private String configuredWebViewCardAction(String title) {
        String collection = sectionCards(requiredWebViewFixture("section"));
        scrollDownUntil(collection, 8);
        String action = collection + "//androidx.cardview.widget.CardView[.//*[@resource-id='"
                + ID_PREFIX + "tv_title' and @text=" + xpathValue(title) + "]]"
                + "//*[@resource-id='" + ID_PREFIX + "btn_join']";
        for (int swipe = 0; swipe < 10 && carousel(collection, "left"); swipe++) {
            // Start from the first card when Explore retains a horizontal offset.
        }
        for (int swipe = 0; swipe < 10 && !isElementVisible(action); swipe++) {
            if (!carousel(collection, "right")) break;
        }
        return action;
    }

    private static String requiredWebViewFixture(String field) {
        String value = System.getProperty("coach.explore.webview." + field);
        Assert.assertTrue("Configure the reviewed WebView " + field, value != null && !value.trim().isEmpty());
        return value;
    }

    private static String webViewText(String expected) {
        String literal = xpathValue(expected);
        return EXPLORE_WEBVIEW + "[@package='" + APP_PACKAGE + "']//*[@text=" + literal + " or @content-desc=" + literal + "]";
    }

    @Step("Close private coaching WebView")
    public void closePrivateCoachingWebView() {
        if (!closeExploreTabWebViewIfPresent()) {
            waitForElementAndClick(DETAIL_CLOSE_BUTTON, "Cannot close private coaching WebView", 10);
        }
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
                        VIDEO_PLAYER,
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

    private void closeVideoPlayer() {
        waitForElementAndClick(
                DETAIL_NAVIGATE_UP_BUTTON,
                "Cannot navigate back from the Android video player",
                10
        );
        if (isElementPresent(EXIT_ACTIVITY_DIALOG)) {
            waitForElementAndClick(
                    EXIT_ACTIVITY_CONFIRM_BUTTON,
                    "Cannot confirm closing the unfinished Android video activity",
                    10
            );
        }
        waitForFirstElementPresent(
                new String[]{EXPLORE_TITLE, TAB_EXPLORE, TODAY_TAB},
                "Android video player did not close",
                15
        );
    }

    private static String sectionTitle(String title) {
        return "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tv_section_title' and translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='" + title.toLowerCase(java.util.Locale.ROOT) + "']";
    }

    private static String sectionCards(String title) {
        return "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tv_section_title' and translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='" + title.toLowerCase(java.util.Locale.ROOT) + "']/following-sibling::androidx.recyclerview.widget.RecyclerView[@resource-id='" + ID_PREFIX + "rv_horizontal_cards']";
    }

    private static String first(String xpathLocator) {
        return "xpath:(" + xpathLocator.substring("xpath:".length()) + ")[1]";
    }
}
