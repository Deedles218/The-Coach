package lib.ui.ios;

import lib.ui.ExplorePageObject;
import lib.Platform;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

public class iOSExplorePageObject extends ExplorePageObject {
    // Observed in 1.13.33 (v7) and 1.13.29 (legacy). Keep the title scoped
    // to the program card: a v7 category badge is also a StaticText node.
    private static final String PROGRAM_SECTION = "//XCUIElementTypeOther["
            + "(@name='CoachProgramDetailedSectionViewCoachProgramDetailedManV7Card' "
            + "or @name='CoachProgramDetailedSectionViewCoachProgramDetailedManCard') "
            + "and .//XCUIElementTypeStaticText[@name='Browse Programs' or @name='ALL PROGRAMS']]";
    private static final String PROGRAM_CARDS = PROGRAM_SECTION
            + "/XCUIElementTypeCollectionView//XCUIElementTypeOther["
            + "@name='CoachProgramDetailedManV7Card' or @name='CoachProgramDetailedManCard']";
    private static final String PROGRAM_TITLES = PROGRAM_CARDS + "/XCUIElementTypeStaticText[1] | "
            + PROGRAM_CARDS + "/XCUIElementTypeImage/XCUIElementTypeStaticText[1]";
    // COA-7069 gives For Her its own configuration. The observed 2.0.33 v7
    // config has programsTitle=""; only real, titled program cards are ready.
    private static final String FOR_HER_PROGRAM_SECTION = "//XCUIElementTypeOther["
            + "@name='CoachProgramDetailedSectionViewCoachProgramDetailedManV7Card']";
    private static final String FOR_HER_PROGRAM_CARDS = FOR_HER_PROGRAM_SECTION
            + "/XCUIElementTypeCollectionView//XCUIElementTypeOther["
            + "@name='CoachProgramDetailedManV7Card' and XCUIElementTypeImage"
            + " and XCUIElementTypeStaticText[1][normalize-space(@name)!='' or normalize-space(@label)!='']]";

    private static boolean isForHer() {
        return "com.vamapps.The-Coach-for-her".equals(Platform.getInstance().getIOSBundleId());
    }

    private static String programCardsXPath() {
        return isForHer() ? FOR_HER_PROGRAM_CARDS : PROGRAM_CARDS;
    }

    private static String programTitlesXPath() {
        return isForHer() ? FOR_HER_PROGRAM_CARDS + "/XCUIElementTypeStaticText[1]" : PROGRAM_TITLES;
    }

    public static String programCollectionLocator() {
        return "xpath:" + (isForHer() ? FOR_HER_PROGRAM_SECTION : PROGRAM_SECTION)
                + "/XCUIElementTypeCollectionView";
    }

    public static String programTitlesLocator() {
        return "xpath:" + programTitlesXPath();
    }

    private static void configureProgramLocators() {
        String cards = programCardsXPath();
        String titles = programTitlesXPath();
        RECOMMENDED_SECTION = isForHer() ? "xpath:" + cards
                : "xpath://XCUIElementTypeStaticText[@name='Browse Programs' or @name='ALL PROGRAMS']";
        RECOMMENDED_COLLECTION = programCollectionLocator();
        RECOMMENDED_PROGRAM_CARDS = "xpath:" + cards;
        FIRST_PROGRAM_CARD = "xpath:(" + cards + ")[1]";
        FIRST_PROGRAM_CARD_IMAGE = FIRST_PROGRAM_CARD + "//XCUIElementTypeImage";
        FIRST_PROGRAM_TITLE = "xpath:(" + titles + ")[1]";
        EXPLORE_PROGRAM_ITEMS = programTitlesLocator();
        LAST_RECOMMENDED_PROGRAM_CARD = "xpath:(" + cards + ")[last()]";
        LAST_RECOMMENDED_PROGRAM_TITLE = "xpath:(" + titles + ")[last()]";
    }

    static {
        TAB_EXPLORE = "id:Explore";
        SELECTED_EXPLORE_TAB = "xpath://XCUIElementTypeButton[@name='Explore' and @value='1']";
        EXPLORE_ENTRY_POINT = "xpath://XCUIElementTypeButton[@name='Explore' or @name='CloseRoundBlack' or @name='ProgramCloseButtonIcon' or @name='navBarRoundBack']";
        TAB_PROGRAMS = "xpath://XCUIElementTypeButton[@name='Programs']";
        TAB_TOOLS = "xpath://XCUIElementTypeButton[@name='Tools']";

        configureProgramLocators();
        SEXUAL_HEALTH_SECTION = "id:SEXUAL HEALTH";
        COURSES_SECTION = "id:COURSES";
        COURSES_COLLECTION = "xpath://XCUIElementTypeStaticText[@name='COURSES']/following-sibling::XCUIElementTypeCollectionView";
        COURSE_CARDS = "xpath://XCUIElementTypeStaticText[@name='COURSES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='CardCourseView']";
        BODY_PRACTICES_SECTION = "id:BODY PRACTICES";
        BODY_PRACTICES_COLLECTION = "xpath://XCUIElementTypeStaticText[@name='BODY PRACTICES']/following-sibling::XCUIElementTypeCollectionView";
        BODY_PRACTICE_CARDS = "xpath://XCUIElementTypeStaticText[@name='BODY PRACTICES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='ExploreExerciseManCellView']";
        FIRST_BODY_PRACTICE_CARD = "xpath:(//XCUIElementTypeStaticText[@name='BODY PRACTICES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='ExploreExerciseManCellView'])[1]";
        MIND_PRACTICES_SECTION = "id:MIND PRACTICES";
        MIND_PRACTICES_COLLECTION = "xpath://XCUIElementTypeStaticText[@name='MIND PRACTICES']/following-sibling::XCUIElementTypeCollectionView";
        MIND_PRACTICE_CARDS = "xpath://XCUIElementTypeStaticText[@name='MIND PRACTICES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='ExploreExerciseManCellView']";
        FIRST_MIND_PRACTICE_CARD = "xpath:(//XCUIElementTypeStaticText[@name='MIND PRACTICES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='ExploreExerciseManCellView'])[1]";
        RETIRED_MAIN_PROGRAMS_SECTION = "id:MAIN PROGRAMS";
        RETIRED_MORE_TOOLS_SECTION = "id:MORE TOOLS";
        COMPLETED_PROGRAMS_SECTION = "id:COMPLETED PROGRAMS";
        COMING_SOON_SECTION = "id:COMING SOON";

        PROGRAM_DETAIL_TITLE = "id:NewCoachProgramHeaderView";
        PROGRAM_DETAIL_CONTENT = "id:DailyDaySwitcherView";
        PROGRAM_DETAIL_CLOSE_BUTTON = "id:CloseRoundBlack";

        FIRST_COURSE_CARD = "xpath:(//XCUIElementTypeOther[@name='CardCourseView'])[1]";
        FIRST_COURSE_CARD_IMAGE = "xpath:(//XCUIElementTypeOther[@name='CardCourseView'])[1]//XCUIElementTypeImage[1]";
        FIRST_COURSE_TITLE = "id:Perform Better In Bed";
        LAST_COURSE_CARD = "xpath:(//XCUIElementTypeStaticText[@name='COURSES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='CardCourseView'])[last()]";
        LAST_COURSE_TITLE = "id:Intermittent Fasting";
        COURSE_DETAIL_TITLE = "id:The_Coach.VideoCourseDetailView";
        COURSE_DETAIL_TYPE = "id:COURSE";
        COURSE_DETAIL_CONTENT = "id:BEFORE YOUR START";
        COURSE_DETAIL_BACK_BUTTON = "id:navBarRoundBack";

        CUSTOM_KEGEL_CARD = "id:Custom Kegel";
        CUSTOM_KEGEL_TITLE = "id:Custom Kegel Workout";
        CUSTOM_KEGEL_START_BUTTON = "id:START WORKOUT";
        CUSTOM_KEGEL_CLOSE_BUTTON = "id:ProgramCloseButtonIcon";
        LAST_BODY_PRACTICE_CARD = "xpath:(//XCUIElementTypeStaticText[@name='BODY PRACTICES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='ExploreExerciseManCellView'])[last()]";
        LAST_BODY_PRACTICE_TITLE = "id:Jacobson Relaxation";
        LAST_BODY_PRACTICE_DETAIL_TITLE = "id:Jacobson Relaxation Exercise";
        LAST_MIND_PRACTICE_CARD = "xpath:(//XCUIElementTypeStaticText[@name='MIND PRACTICES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='ExploreExerciseManCellView'])[last()]";
        LAST_MIND_PRACTICE_TITLE = "id:Triple Column";
        LAST_MIND_PRACTICE_DETAIL_TITLE = "id:Triple Column Practice";
        PRACTICE_DETAIL_HEADER = "id:DailyStartHeaderView";
        PRACTICE_DETAIL_GOAL = "id:GOAL";
        PRACTICE_DETAIL_START_BUTTON = "id:START EXERCISE";
        CONCEPT_POPUP_CONFIRM_BUTTON = "id:Got it";

        OVERALL_HEALTH_PROGRAM = "xpath://XCUIElementTypeOther[@name='overall_health_program'] | //XCUIElementTypeStaticText[@name='Overall Health']";
        PROGRAM_SETTINGS_BUTTON = "id:CoachProgramSettingsIcon";
        PROGRAM_SETTINGS_SCREEN = "id:Removed exercises";
        REMOVED_EXERCISES_BUTTON = "xpath://XCUIElementTypeButton[@name='Removed exercises'] | //XCUIElementTypeStaticText[@name='Removed exercises']";
        REMOVED_EXERCISES_SCREEN = "id:Restore";
        REMOVED_EXERCISE_CARD = "id:DailyPlanItem";
        REMOVED_EXERCISE_CHECKBOX = "id:SettingsCheckBoxInactive";
        RECOVER_REMOVED_EXERCISE_BUTTON = "id:Restore";
        PROGRAM_DAY_CONTAINERS = "id:program_day_container";
        RECOVERED_EXERCISE_CARDS = "id:recovered_exercise_card";
    }

    public iOSExplorePageObject(RemoteWebDriver driver) {
        super(driver);
        // Base locators are static in the existing architecture; reset all
        // program selectors together when attaching to another app variant.
        configureProgramLocators();
    }

    @Override
    protected void dismissExploreWelcomeIfPresent() {
        new iOSCoachFlowPageObject(driver).dismissExploreWelcomeIfPresent();
    }

    private static final String EXPLORE_HEADING = "xpath://XCUIElementTypeStaticText[@name='Explore' and @visible='true']";

    @Override
    @io.qameta.allure.Step("Open Explore and dismiss its abandoned-checkout banner when present")
    public void openExploreTab() {
        super.openExploreTab();
        String close = "id:AbandonedCartCloseButtonIcon";
        if (isElementVisible(close)) {
            waitForElementAndClick(close, "Cannot dismiss the Explore checkout banner", 5);
            waitForElementNotVisible(close, "Explore checkout banner stayed visible", 5);
        }
    }

    protected static boolean usesMaleProductionContract() {
        return "com.vamapps.The-Coach".equals(Platform.getInstance().getIOSBundleId());
    }

    protected String[] configuredSectionTitles() {
        return usesMaleProductionContract()
                ? new String[]{"Quick Tips", "Master Classes", "Private Coaching Sessions", "Body Practices"}
                : new String[]{"Quick Tips", "Master Classes", "Private coaching session"};
    }

    private String coachingSectionTitle() {
        return usesMaleProductionContract() ? "Private Coaching Sessions" : "Private coaching session";
    }

    protected String configuredSection(String title) {
        return configuredSection(title, true);
    }

    private String configuredSection(String title, boolean visible) {
        String visibility = visible ? " and @visible='true'" : "";
        if ("Master Classes".equals(title)) {
            return "xpath://XCUIElementTypeStaticText[translate(@label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ ',"
                    + "'abcdefghijklmnopqrstuvwxyz')='masterclasses'" + visibility + "]";
        }
        return "xpath://XCUIElementTypeStaticText[translate(@label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='"
                + title.toLowerCase(java.util.Locale.ROOT) + "'" + visibility + "]";
    }

    protected String configuredCards(String title) {
        // Scope cards to the nearest section containing its heading and collection,
        // rather than using unrelated images/buttons elsewhere on the page.
        // The heading can scroll offscreen while its card content is visible.
        return configuredSection(title, false) + "/ancestor::XCUIElementTypeOther[.//XCUIElementTypeCollectionView][1]"
                + "//XCUIElementTypeCollectionView/*[not(@type='XCUIElementTypeOther' and contains(@name,'scroll bar'))]";
    }

    private void revealConfiguredSection(String title) {
        if (!isElementVisible(configuredSection(title))) rewindConfiguredCatalog();
        swipeUpToFindFirstVisibleElement(new String[]{configuredSection(title)}, "Explore section absent: " + title, 8);
        waitForElementVisible(configuredSection(title), "Explore section not visible: " + title, 10);
    }

    private void rewindConfiguredCatalog() {
        for (int swipe = 0; swipe < 20; swipe++) {
            String before = configuredViewport();
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("direction", "down");
            ((JavascriptExecutor) driver).executeScript("mobile: swipe", arguments);
            final String[] previous = {null};
            createWait(10).withMessage("Explore scroll did not settle").until(d -> {
                String current = configuredViewport();
                boolean stable = current.equals(previous[0]);
                previous[0] = current;
                return stable;
            });
            if (before.equals(configuredViewport())) return;
        }
        throw new AssertionError("Explore top was not reached");
    }

    private String configuredViewport() {
        StringBuilder viewport = new StringBuilder();
        for (WebElement text : driver.findElements(org.openqa.selenium.By.xpath(
                "//XCUIElementTypeStaticText[@visible='true']")))
            viewport.append(text.getText()).append(text.getRect());
        return viewport.toString();
    }

    @Override public void assertConfiguredSectionsAreDisplayed() {
        for (String title : configuredSectionTitles())
            revealConfiguredSection(title);
    }
    @Override public void assertCoursesSectionIsRemoved() {
        if (usesMaleProductionContract()) assertSectionsAreAbsent(new String[]{"COURSES"});
        else assertRemovedCoursesAndPracticesAreAbsent();
    }
    @Override public void assertBrowseProgramsReplacesLegacyBlock() {
        waitForElementVisible("id:Browse Programs", "Explore v7 must show Browse Programs", 10);
        assertElementNotPresent("id:ALL PROGRAMS", "Explore v7 must not show the legacy ALL PROGRAMS heading");
        assertMainProgramsRecommendedSection();
        assertRetiredExploreUiIsAbsent();
    }
    @Override public void assertConfiguredCardTemplates() {
        for (String title : configuredSectionTitles()) {
            revealConfiguredSection(title);
            String cards = configuredCards(title);
            waitForElementVisible(cards, "No configured cards in " + title, 10);
            WebElement image = waitForElementVisible(cards + "//XCUIElementTypeImage",
                    "Configured card has no image in " + title, 10);
            Assert.assertTrue("Configured image has empty bounds in " + title,
                    image.getRect().getWidth() > 0 && image.getRect().getHeight() > 0);
        }
        revealConfiguredSection(coachingSectionTitle());
        waitForElementEnabled(coachingAction(), "Private coaching action is missing", 10);
    }
    @Override public void assertConfiguredCardTitlesAreNonEmpty() {
        for (String title : configuredSectionTitles()) {
            revealConfiguredSection(title);
            String visibleTitles = configuredCards(title) + "//XCUIElementTypeStaticText[@visible='true']";
            swipeUpToFindFirstVisibleElement(new String[]{visibleTitles},
                    "Configured card titles are below the viewport in " + title, 8);
            List<WebElement> titles = driver.findElements(getLocatorByString(visibleTitles));
            Assert.assertFalse("Configured cards have no titles in " + title, titles.isEmpty());
            for (WebElement node : titles) Assert.assertFalse("Empty configured card title", node.getText().trim().isEmpty());
        }
    }
    @Override public void openQuickTipVideoAndVerifyPlayer() {
        revealConfiguredSection("Quick Tips");
        if (usesMaleProductionContract()) {
            // Production v7 routes this specific card to coach_lesson, not a video.
            waitForElementAndClick(quickTipCardLocator(), "Configured Finish Too Soon card absent", 10);
            waitForElementNotVisible(SELECTED_EXPLORE_TAB, "Quick Tip left user on the Explore catalog", 15);
            waitForElementVisible(quickTipArticleTitleLocator(), "Quick Tip opened the wrong article", 15);
            waitForElementEnabled("id:ic outline close", "Quick Tip article close control absent", 10);
            waitForElementEnabled("id:CONTINUE", "Quick Tip article Continue control absent", 10);
            createWait(10).withMessage("Quick Tip article body is not displayed").until(ignored -> {
                for (WebElement text : driver.findElements(org.openqa.selenium.By.className("XCUIElementTypeStaticText"))) {
                    if (text.isDisplayed() && iOSDailyPlanPageObject.isLessonBodyText(
                            getElementAccessibleName(text), "When You’re About to Finish Too Soon")) return true;
                }
                return false;
            });
            return; // Never advance or complete the article during this opening check.
        }
        waitForElementAndClick(configuredCards("Quick Tips"), "Quick Tip card absent", 10);
        waitForElementNotVisible(EXPLORE_HEADING, "Quick Tip left user on Explore", 15);
        new lib.ui.VideoLessonPlayerPageObject(driver).waitUntilReady();
    }
    protected String quickTipCardLocator() {
        return configuredCards("Quick Tips") + "//XCUIElementTypeStaticText[@label='Finish Too Soon' and @visible='true']";
    }
    protected String quickTipArticleTitleLocator() {
        return "id:When You’re About to Finish Too Soon";
    }
    @Override public void openMasterClassAndVerifyDestination() {
        revealConfiguredSection("Master Classes");
        waitForElementAndClick(configuredCards("Master Classes"), "Master Class card absent", 10);
        waitForElementNotVisible(EXPLORE_HEADING, "Master Class left user on Explore", 15);
        waitForFirstElementPresent(new String[]{"xpath://XCUIElementTypeWebView",
                COURSE_DETAIL_TITLE, "id:LessonVideoView",
                "xpath://XCUIElementTypeNavigationBar[@name='The_Coach.SimplePlayerView']"},
                "Master Class did not open lesson or WebView content", 20);
    }
    @Override public void openPrivateCoachingAndVerifyWebView() {
        revealConfiguredSection(coachingSectionTitle());
        waitForElementAndClick(coachingAction(), "Configured private coaching action absent", 10);
        waitForElementNotVisible(EXPLORE_HEADING, "Coaching left user on Explore", 15);
        String webView = "xpath://XCUIElementTypeWebView";
        String booking = coachingBookingLocator();
        // XCUITest can flatten WKWebView into native Link nodes (as observed in Shop).
        waitForFirstElementPresent(new String[]{webView, booking}, "Private coaching web content absent", 20);
        if (usesMaleProductionContract()) {
            waitForElementVisible(booking, "Connell relationship coaching booking action absent", 20);
            return; // Only inspect the landing page; never tap a booking link.
        }
        waitForFirstElementPresent(new String[]{booking,
                webView + "//*[@label='Book 1:1 a private consultation' or @name='Book 1:1 a private consultation']"},
                "Private coaching booking action absent", 20);
    }
    protected String coachingBookingLocator() {
        String label = usesMaleProductionContract() ? "Book a private 1:1 session" : "Book 1:1 a private consultation";
        return "xpath://XCUIElementTypeLink[@label='" + label + "' or @name='" + label + "']";
    }
    protected String coachingAction() {
        return configuredCards(coachingSectionTitle())
                + "//*[(@type='XCUIElementTypeButton' or @type='XCUIElementTypeStaticText') "
                + "and translate(@label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='"
                + (usesMaleProductionContract() ? "book" : "learn more") + "' and @visible='true']";
    }
    @Override public void closePrivateCoachingWebView() {
        if (!isElementVisible("id:TopBrowserBar")) {
            closeTransientDetailIfPresent();
            return;
        }
        closeExploreBrowser();
        waitForElementNotVisible("id:TopBrowserBar", "Coaching browser did not close", 10);
        waitForElementVisible(SELECTED_EXPLORE_TAB, "Coaching close did not restore the selected Explore tab", 15);
        waitForElementVisible(EXPLORE_HEADING, "Coaching close did not return to Explore", 15);
    }

    protected void closeExploreBrowser() {
        // Business assertion: do not replace a failed close with a recovery restart.
        new iOSCoachFlowPageObject(driver).tapHittableControl(new String[]{
                "xpath://XCUIElementTypeOther[@name='TopBrowserBar']//XCUIElementTypeButton[@name='Close' or @name='Закрыть']"},
                "Explore browser close is not hittable", 10);
    }
    @Override public void closeTransientDetailIfPresent() {
        // The article reader also renders an Explore heading. It is not the
        // catalog until the selected Explore tab has actually returned.
        if (isElementVisible(EXPLORE_HEADING) && isElementVisible(SELECTED_EXPLORE_TAB)) return;
        if (isElementVisible("id:TopBrowserBar")) {
            new iOSCoachFlowPageObject(driver).closeProfileBrowserIfPresent();
            waitForElementVisible(SELECTED_EXPLORE_TAB, "Browser did not restore the selected Explore tab", 15);
            waitForElementVisible(EXPLORE_HEADING, "Browser did not return to Explore", 15);
            return;
        }
        if (isElementVisible("xpath://XCUIElementTypeNavigationBar[@name='The_Coach.SimplePlayerView']")) {
            new lib.ui.VideoLessonPlayerPageObject(driver).closePlayer();
            waitForElementVisible(SELECTED_EXPLORE_TAB, "Video did not restore the selected Explore tab", 15);
            waitForElementVisible(EXPLORE_HEADING, "Video did not return to Explore", 15);
            return;
        }
        String[] closeControls = new String[]{"id:navBarRoundBack", "id:CloseRoundBlack",
                "id:navBarRoundClose", "id:ic_outline_close", "id:ProgramCloseButtonIcon",
                "id:ic outline close"};
        boolean closable = false;
        for (String control : closeControls) if (isElementVisible(control)) closable = true;
        if (!closable) return;
        waitForFirstElementAndClick(closeControls,
                "Cannot close Explore destination", 10);
        String lessonExit = iOSDailyPlanPageObject.lessonExitConfirmationLocator();
        createWait(10).withMessage("Explore destination neither closed nor requested unfinished exit").until(ignored ->
                isElementVisible(SELECTED_EXPLORE_TAB) || isElementVisible("id:QUIT") || isElementVisible(lessonExit));
        if (isElementVisible("id:QUIT")) waitForElementAndClick("id:QUIT", "Cannot quit unfinished video", 10);
        else if (isElementVisible(lessonExit))
            waitForElementAndClick("id:Quit", "Cannot quit unfinished Explore article", 10);
        waitForElementVisible(SELECTED_EXPLORE_TAB, "Closing destination did not restore the selected Explore tab", 15);
        waitForElementVisible(EXPLORE_HEADING, "Closing destination did not return to Explore", 15);
    }

    @Override public void assertRemovedCoursesAndPracticesAreAbsent() {
        if (usesMaleProductionContract()) {
            // The approved production contract retains Body Practices, not legacy Courses.
            assertConfiguredSectionsAreDisplayed();
            assertSectionsAreAbsent(new String[]{"COURSES"});
        } else assertSectionsAreAbsent(new String[]{"COURSES", "BODY PRACTICES", "MIND PRACTICES"});
    }

    private void assertSectionsAreAbsent(String[] titles) {
        waitForElementVisible(SELECTED_EXPLORE_TAB,"Explore is not selected",10);
        // Scan to both ends; checking only the first viewport would miss lazy sections.
        for(String direction:new String[]{"down","up"}) {
            String previous=""; boolean end=false;
            for(int n=0;n<20;n++) {
                for(String title:titles)
                    Assert.assertFalse("Retired Explore section reappeared: "+title,
                        isElementVisible("xpath://XCUIElementTypeStaticText[translate(@label,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='"+title+"']"));
                StringBuilder viewport=new StringBuilder();
                for(WebElement e:driver.findElements(org.openqa.selenium.By.xpath("//XCUIElementTypeStaticText[@visible='true']")))
                    viewport.append(e.getText()).append(e.getRect());
                if(previous.equals(viewport.toString())) {end=true;break;}
                previous=viewport.toString();
                Map<String,Object> args=new HashMap<String,Object>();args.put("direction",direction);
                ((JavascriptExecutor)driver).executeScript("mobile: swipe",args);
            }
            Assert.assertTrue("Explore end was not reached; absence is not proven",end);
        }
    }

    @Override
    public void openOverallHealthProgramSettings() {
        openFixtureProgramSettings("Overall Health");
    }

    public void openKegelProgramSettings() { openFixtureProgramSettings("Kegel Challenge"); }

    private void openFixtureProgramSettings(String program) {
        String collection = programCollectionLocator();
        String title = "xpath:(" + programTitlesXPath() + ")[@label="
                + lib.ui.LocalizationPageObject.xpathLiteral(program) + " and @visible='true']";
        new iOSProgramListReader(driver).rewind(programTitlesLocator(), collection);
        for (int i = 0; i < 12 && !isElementVisible(title); i++) {
            Rectangle bounds = waitForElementPresent(collection, "Program carousel is not visible", 15).getRect();
            Map<String, Object> gesture = new HashMap<String, Object>();
            gesture.put("duration", .65);
            gesture.put("fromX", bounds.getX() + bounds.getWidth() * .8);
            gesture.put("toX", bounds.getX() + bounds.getWidth() * .2);
            gesture.put("fromY", bounds.getY() + bounds.getHeight() * .5);
            gesture.put("toY", bounds.getY() + bounds.getHeight() * .5);
            ((JavascriptExecutor) driver).executeScript("mobile: dragFromToForDuration", gesture);
        }
        waitForElementAndClick(title, program + " is absent from the program carousel", 10);
        // Catalog selection returns before the fetched detail screen appears.
        // Wait for that screen transition before looking for its Settings action.
        waitForElementPresent(PROGRAM_DETAIL_TITLE, program + " detail did not load", 30);
        waitForElementAndClick(PROGRAM_SETTINGS_BUTTON, "Cannot open " + program + " Settings", 15);
        waitForElementPresent(PROGRAM_SETTINGS_SCREEN, program + " Settings did not open", 10);
    }

    @Override
    public void selectRemovedExerciseAndRecover() {
        String card = "xpath://XCUIElementTypeOther[@name='DailyPlanItem' "
                + "and .//XCUIElementTypeStaticText[@name='Check Your Meal Plan']]";
        waitForElementPresent(card, "The prepared removed meal-plan card is absent", 15);
        Assert.assertEquals("The removed fixture card must be unambiguous", 1, getAmountElements(card));
        waitForElementAndClick(card + "//XCUIElementTypeImage[@name='SettingsCheckBoxInactive']",
                "The fixture checkbox must initially be unchecked", 10);
        waitForElementPresent(card + "//XCUIElementTypeImage[@name='SettingsCheckBoxActive']",
                "The fixture checkbox did not become checked", 10);
        // In 1.13.29 the action is labelled Restore (RECOVER in the original TC).
        waitForElementEnabled(RECOVER_REMOVED_EXERCISE_BUTTON, "Restore did not become enabled", 10);
        waitForElementAndClick(RECOVER_REMOVED_EXERCISE_BUTTON, "Cannot restore the selected fixture card", 10);
        waitForElementNotPresent(REMOVED_EXERCISES_SCREEN, "Removed exercises modal did not close", 15);
        waitForElementPresent(PROGRAM_SETTINGS_SCREEN, "Recovery did not return to program Settings", 10);
    }

    @Override
    @io.qameta.allure.Step("Verify the restored fixture card is rendered on Overall Health day 2")
    public void assertRecoveredFixtureCardIsRendered() {
        waitForElementAndClick("id:navBarRoundBack", "Cannot return to the restored program", 10);
        waitForElementPresent("id:CoachProgramSettingsIcon", "Overall Health detail did not reappear", 10);
        waitForElementPresent("id:Day 1", "Expected a fresh Overall Health day-1 view", 10);
        waitForElementAndClick("id:rightSwitcherArrow", "Cannot open the prepared second day", 10);
        waitForElementPresent("id:Day 2", "Overall Health day 2 did not open", 15);
        String card = "xpath://XCUIElementTypeOther[@name='DailyPlanItem' and @visible='true' "
                + "and .//XCUIElementTypeStaticText[@name='Check Your Meal Plan' and @visible='true']]";
        for (int i = 0; i < 6 && !isElementVisible(card); i++) swipeUpQuick();
        waitForElementPresent(card, "The restored card was not rendered on day 2", 10);
        Assert.assertEquals("The restored card is duplicated in the day-2 UI", 1, getAmountElements(card));
        io.qameta.allure.Allure.addAttachment("Restored card rendered on day 2", "image/png",
                new java.io.ByteArrayInputStream(((org.openqa.selenium.TakesScreenshot) driver)
                        .getScreenshotAs(org.openqa.selenium.OutputType.BYTES)), "png");
    }

    @Override
    public List<String> getProgramNamesFromExplore() {
        String collection = programCollectionLocator();
        waitForElementPresent(collection, "The full program carousel is not exposed in this Explore variant", 15);
        String titles = programTitlesLocator();
        iOSProgramListReader reader = new iOSProgramListReader(driver);
        reader.rewind(titles, collection);
        return reader.collect(titles, collection, true);
    }

    @Override
    @io.qameta.allure.Step("Read the complete recycled carousel and open its last program")
    public void swipeToAndOpenLastRecommendedProgram() {
        List<String> names = getProgramNamesFromExplore();
        String last = names.get(names.size() - 1);
        waitForElementAndClick("xpath:(" + programTitlesXPath() + ")[@label="
                        + lib.ui.LocalizationPageObject.xpathLiteral(last) + " and @visible='true']",
                "Cannot open the last program in the complete carousel", 10);
        waitForElementPresent(PROGRAM_DETAIL_TITLE, "Last program detail screen did not open", 15);
        waitForElementPresent(PROGRAM_DETAIL_CONTENT, "Last program detail content is not displayed", 10);
    }
}
