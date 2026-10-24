package lib.ui.ios;

import lib.ui.DailyPlanPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.By;
import org.junit.Assert;
import org.openqa.selenium.support.ui.Quotes;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import lib.TestData;

public class iOSDailyPlanPageObject extends DailyPlanPageObject {
    private int selectorTop;
    private String openedLessonTitle;
    private static final String DAILY_FIRST_LESSON_TITLE = "xpath://XCUIElementTypeCollectionView"
            + "[.//XCUIElementTypeOther[@name='DailyDaySwitcherView']"
            + " and not(.//XCUIElementTypeStaticText[starts-with(@name,'Module ')])]"
            + "//XCUIElementTypeCell//XCUIElementTypeStaticText"
            + "[following-sibling::XCUIElementTypeOther//XCUIElementTypeStaticText"
            + "[@name='Lesson 1' or @name='How it all works']]";
    private static final String MODULE_LESSON_NUMBER = "starts-with(@name,'Lesson ')"
            + " and number(substring-after(@name,'Lesson '))>0"
            + " and translate(substring-after(@name,'Lesson '),'0123456789','')=''";
    // Module lesson numbers continue across stages. Reuse DailyPlanItem and the
    // nearest section-heading contract used by dailyPlanCardLocator below.
    private static final String MODULE_FIRST_LESSON_TITLE = "(//XCUIElementTypeCollectionView"
            + "[.//XCUIElementTypeOther[@name='DailyDaySwitcherView']"
            + " and .//XCUIElementTypeStaticText[starts-with(@name,'Module ')]]"
            + "//XCUIElementTypeOther[@name='DailyPlanItem'"
            + " and ancestor::XCUIElementTypeCell/preceding-sibling::XCUIElementTypeCell"
            + "[.//XCUIElementTypeStaticText[@name='TitleBlock.Title']][1]"
            + "//XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='DAILY LESSONS']]"
            + "//XCUIElementTypeStaticText[string-length(normalize-space(@name))>0"
            + " and following-sibling::XCUIElementTypeOther//XCUIElementTypeStaticText["
            + MODULE_LESSON_NUMBER + "]])[1]";
    private static final String SELECTOR_CARDS = "id:ProgramSelectionCardView";
    private static final String SELECTOR_TITLES = "xpath://XCUIElementTypeOther[@name='ProgramSelectionCardView']//XCUIElementTypeStaticText[@name='TitleBlock.Title']";
    private static final String SELECTOR_COLLECTION = "xpath:(//XCUIElementTypeScrollView[.//XCUIElementTypeOther[@name='ProgramSelectionCardView']] | //XCUIElementTypeCollectionView[.//XCUIElementTypeOther[@name='ProgramSelectionCardView']])[last()]";
    private static final String LESSON_EXIT_CONFIRM_TITLE = "xpath://XCUIElementTypeStaticText[@name='TitleBlock.Title' "
            + "and contains(@label, 'finished') and contains(@label, 'Are you sure that you want to quit?')]";
    static {
        TAB_TODAY = "id:Today";
        SELECTED_TODAY_TAB = "xpath://XCUIElementTypeButton[@name='Today' and @value='1']";
        ACTIVE_PROGRAM_TITLE = activeProgramTitleLocator();
        // The program selector is a new app-side contract. The active-program
        // label remains a stable fallback for builds that have not migrated
        // the tap target to the dedicated identifier yet.
        PROGRAM_SELECTOR = "id:daily_plan_program_selector";
        PROGRAM_SELECTOR_MODAL = SELECTOR_CARDS;
        PROGRAM_SELECTOR_ITEMS = "id:program_selector_item";
        PROGRAM_SELECTOR_CLOSE = "xpath://XCUIElementTypeButton[@name='program_selector_close' or @name='CloseRoundBlack']";
        CURRENT_DAY_LABEL = "xpath://XCUIElementTypeOther[@name='DailyDaySwitcherView']//XCUIElementTypeStaticText[starts-with(@name, 'Day ') or starts-with(@name, 'Stage ')]";
        DAILY_PLAN_DAY_SWITCHER = CURRENT_DAY_LABEL;
        LEFT_SWITCHER_ARROW = "id:leftSwitcherArrow";
        RIGHT_SWITCHER_ARROW = "id:rightSwitcherArrow";

        DAILY_LESSONS_TITLE = "xpath://XCUIElementTypeStaticText[@label='DAILY LESSONS']";
        FIRST_LESSON_TITLE = DAILY_FIRST_LESSON_TITLE + " | " + MODULE_FIRST_LESSON_TITLE;
        FIRST_LESSON_TYPE = "xpath:(" + FIRST_LESSON_TITLE.substring("xpath:".length())
                + ")/following-sibling::XCUIElementTypeOther//XCUIElementTypeStaticText"
                + "[@name='How it all works' or (" + MODULE_LESSON_NUMBER + ")]";
        LESSON_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[@name='Starting Point' or @name='Getting started' or @name='The Basics of Sexual Desire']";
        LESSON_SCREEN_CONTENT = "xpath://XCUIElementTypeStaticText[string-length(@name) > 20 and @name!='The Basics of Sexual Desire']";
        LESSON_SCREEN_BACK_BUTTON = "xpath://XCUIElementTypeButton[@name='ProgramCloseButtonIcon' or @name='BackButton' or @name='Close' or @name='Back' or @name='ic outline close']";

        DAILY_PRACTICE_TITLE = "xpath://XCUIElementTypeStaticText[@label='DAILY PRACTICE']";
        FIRST_PRACTICE_TITLE = "xpath://XCUIElementTypeStaticText[@name='Unlock Your Pelvic Floor' or @name='Pelvic Floor Assessment' or @name='Your First Kegel Workout' or @name='Finding Pelvic Floor' or @name='Morning Kegel Workout' or @name='Mindful Eating']";
        FIRST_PRACTICE_TYPE = "xpath://XCUIElementTypeStaticText[@name='Guide' or @name='Daily workout' or contains(@name, 'day in total') or contains(@name, 'days in total') "
                + "or (@name='Level 1' and ancestor::XCUIElementTypeOther[@name='DailyPlanItem'][.//XCUIElementTypeStaticText[@name='Mindful Eating']])]";
        CUSTOMIZATION_CATCH_UP_SECTION = "xpath://XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='TO CATCH-UP' and @visible='true']";
        CUSTOMIZATION_CATCH_UP_CARD = "id:daily_plan_catch_up_card";
        CUSTOMIZATION_POSTPONED_ICON = "id:ItemMovedForward";
        CUSTOMIZATION_SINGLE_TASK = "id:daily_plan_customization_task";
        PROGRAM_PROGRESS_LABEL = "xpath://XCUIElementTypeStaticText[@visible='true' and contains(@name,'%')]";
        // The old tooltip uses the same title as the new popup; no invented id.
        LEGACY_POSTPONE_TOOLTIP = "xpath://XCUIElementTypeStaticText[@visible='true' and contains(@name,'Finish today') and contains(@name,'unlock the next day')]";
        PRACTICE_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[(contains(@name, 'Kegel') or @name='Finding Pelvic Floor') and @visible='true']";
        PRACTICE_SCREEN_GOAL_TITLE = "xpath://XCUIElementTypeStaticText[@label='GOAL' or @name='GOAL' or @name='Duration:' or @name='Intensity:']";
        PRACTICE_SCREEN_EXERCISES_TITLE = "xpath://XCUIElementTypeStaticText[@label='EXERCISES' or @name='EXERCISES' or contains(@name, 'ADVANCED')]";
        PRACTICE_SCREEN_START_BUTTON = "xpath://XCUIElementTypeButton[@name='START WORKOUT' and @visible='true']";
        PRACTICE_SCREEN_CLOSE_BUTTON = "id:ProgramCloseButtonIcon";
        KEGEL_DAILY_PLAN_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Kegel Workout') and @visible='true']";
        KEGEL_START_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Kegel') and @visible='true']";
        KEGEL_START_SCREEN_DESCRIPTION = "xpath://XCUIElementTypeOther[@name='Description']//XCUIElementTypeStaticText[@visible='true']";
        KEGEL_START_SCREEN_LEVEL = "xpath://XCUIElementTypeStaticText[contains(@name, 'Kegel') and contains(@name, 'Level') and @visible='true']";
        KEGEL_START_SCREEN_DURATION = "xpath://XCUIElementTypeStaticText[@name='Duration:' and @visible='true']";
        KEGEL_START_SCREEN_DURATION_VALUE = "xpath://XCUIElementTypeStaticText[contains(@name, 'min') and contains(@name, 'sec') and @visible='true']";
        KEGEL_START_SCREEN_INTENSITY = "xpath://XCUIElementTypeStaticText[@name='Intensity:' and @visible='true']";
        KEGEL_START_SCREEN_INTENSITY_VALUE = "xpath://XCUIElementTypeStaticText[@name='Intensity:']/following-sibling::XCUIElementTypeStaticText[1]";
        KEGEL_START_SCREEN_STRETCHING_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Stretching PF') and @visible='true']";
        KEGEL_START_SCREEN_STRETCHING_DESCRIPTION = "xpath://XCUIElementTypeStaticText[contains(@name, 'Helps relax the right muscles') and @visible='true']";
        KEGEL_START_SCREEN_EXERCISE_TITLE = "xpath://XCUIElementTypeStaticText[(contains(@name, 'Stretching') or @name='Squeeze') and @visible='true']";
        KEGEL_START_SCREEN_EXERCISE_ITEMS = "xpath://XCUIElementTypeOther[@name='KegelStartExerciseView']";
        KEGEL_START_SCREEN_EXERCISE_METADATA = "xpath://XCUIElementTypeStaticText[contains(@name, 'SEC')]";
        KEGEL_START_SCREEN_FIRST_EXERCISE = "xpath:(//XCUIElementTypeOther[@name='KegelStartExerciseView' and @visible='true'])[1]";
        KEGEL_INSTRUCTION_BACK_BUTTON = "id:GreyBackButton";
        KEGEL_INSTRUCTION_TITLE = "xpath://XCUIElementTypeButton[@name='GreyBackButton']/following::XCUIElementTypeStaticText[@visible='true'][1]";
        KEGEL_INSTRUCTION_IMAGE = "xpath://XCUIElementTypeOther[@name='LessonLandscapeImage']//XCUIElementTypeImage[@visible='true']";
        KEGEL_INSTRUCTION_HEADING = "xpath://XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='INSTRUCTION:']";
        KEGEL_INSTRUCTION_STEP_NUMBERS = "xpath://XCUIElementTypeOther[@name='NumberedTextLabelView']/XCUIElementTypeStaticText[string-length(@name)=1]";
        KEGEL_INSTRUCTION_STEP_TEXTS = "xpath://XCUIElementTypeOther[@name='NumberedTextLabelView']/XCUIElementTypeStaticText[string-length(@name)>1]";
        KEGEL_INSTRUCTION_ADJUST_HEADING = "xpath://XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='ADJUST:']";
        KEGEL_INSTRUCTION_INTENSITY_LABEL = "id:Intensity";
        KEGEL_INSTRUCTION_INTENSITY_VALUE = "xpath://XCUIElementTypeStaticText[(@name='EASY' or @name='MEDIUM' or @name='HARD') and @visible='true']";
        KEGEL_INSTRUCTION_INTENSITY_PICKER = "xpath://XCUIElementTypePickerWheel[@visible='true']";
        KEGEL_INSTRUCTION_INTENSITY_SELECT_BUTTON = "id:SELECT";
        KEGEL_INSTRUCTION_SAVE_BUTTON = "id:SAVE";
        KEGEL_MEDIA_PLAYER_BACK_BUTTON = "id:nav bar back round black";
        KEGEL_STRETCHING_COMPLETION_LETS_GO_BUTTON = "id:LET’S GO!";
        KEGEL_PLAYER_BACK_BUTTON = "id:navBarRoundClose";
        KEGEL_PLAYER_MUTE_BUTTON = "xpath://XCUIElementTypeButton[contains(@name, 'sound off')]";
        KEGEL_PLAYER_UNMUTE_BUTTON = "xpath://XCUIElementTypeButton[contains(@name, 'sound on')]";
        KEGEL_PLAYER_EXERCISE_TITLE = kegelPlayerExerciseTitleLocator();
        KEGEL_PLAYER_DIFFICULTY = "xpath://XCUIElementTypeStaticText[@name='GUIDE' or contains(@name, 'How to find pelvic floor')]";
        // Keep a future-compatible fallback for builds that expose progress as a
        // clock-like accessibility label (for example "00:12/03:52"). The current
        // iOS build does not expose this label, so the smoke assertion uses the
        // player controls as its readiness signal.
        KEGEL_PLAYER_TIMER = "xpath://XCUIElementTypeStaticText[(contains(@name, ':' ) or contains(@label, ':' )) and @visible='true']";
        KEGEL_PLAYER_PAUSE_BUTTON = "id:PlayerPauseIcon";
        KEGEL_PLAYER_PLAY_BUTTON = "id:PlayerPlayIcon";
        KEGEL_PLAYER_REWIND_BUTTON = "id:RewindButtonMain";
        KEGEL_PLAYER_REWIND_BACK_BUTTON = "xpath:(//XCUIElementTypeButton[@name='RewindButtonMain' and @visible='true'])[1]";
        KEGEL_PLAYER_REWIND_FORWARD_BUTTON = "xpath:(//XCUIElementTypeButton[@name='RewindButtonMain' and @visible='true'])[2]";
        KEGEL_PLAYER_VIBRATION_BUTTON = "xpath://XCUIElementTypeButton[contains(@name, 'vibro')]";
        KEGEL_PLAYER_VIBRATION_ON_BUTTON = "xpath://XCUIElementTypeButton[@name='vibro on']";
        KEGEL_PLAYER_VIBRATION_OFF_BUTTON = "xpath://XCUIElementTypeButton[@name='vibro off']";
        KEGEL_PLAYER_INFO_BUTTON = "id:ic outline info";
        KEGEL_PLAYER_INFO_TOOLTIP = "xpath://XCUIElementTypeStaticText[contains(@name, 'How to do the') and contains(@name, 'exercise correctly')]";
        KEGEL_PLAYER_INFO_MODAL_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Kegels') and @visible='true']";
        KEGEL_PLAYER_INFO_MODAL_CONTENT = kegelPlayerInfoContentLocator();
        KEGEL_PLAYER_INFO_MODAL_CLOSE_BUTTON = "xpath://XCUIElementTypeButton[@name='ic outline close' and @visible='true']";
        KEGEL_PLAYER_PHASE_SQUEEZE_BUTTON = "xpath://XCUIElementTypeStaticText[@name='SQUEEZE']";
        KEGEL_PLAYER_PHASE_REST_BUTTON = "xpath://XCUIElementTypeStaticText[@name='REST']";
        KEGEL_PLAYER_PHASE_WAVES_BUTTON = "xpath://XCUIElementTypeStaticText[@name='WAVES']";
        KEGEL_PLAYER_EXIT_CONFIRM_TITLE = "xpath://XCUIElementTypeStaticText[(contains(@value, 'You haven') or contains(@label, 'You haven')) and (contains(@value, 'quit') or contains(@label, 'quit')) and @visible='true']";
        KEGEL_PLAYER_EXIT_CONFIRM_QUIT_BUTTON = "id:Quit";
        KEGEL_PLAYER_EXIT_CONFIRM_CONTINUE_BUTTON = "id:Don’t quit";
        PRACTICE_COMPLETION_FEEDBACK_TITLE = "xpath://XCUIElementTypeStaticText[(@name='TitleBlock.Title' and @label='YOU’RE GREAT!') or @name='Practice complete!']";
        PRACTICE_COMPLETION_FEEDBACK_CLOSE_BUTTON = "id:ic outline close";
        PRACTICE_COMPLETION_INTENSITY_TOO_EASY = "xpath://XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='Too Easy']";
        PRACTICE_COMPLETION_INTENSITY_GREAT = "xpath://XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='Great']";
        PRACTICE_COMPLETION_INTENSITY_TOO_HARD = "xpath://XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='Too Hard']";
        CUSTOMIZATION_MOVE_TO_TOMORROW_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Move to tomorrow'] | //XCUIElementTypeButton[@name='Move to tomorrow']";
        CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Remove from Daily Plan'] | //XCUIElementTypeButton[@name='Remove from Daily Plan']";
        CUSTOMIZATION_DELETE_CONFIRM_BUTTON = "id:DELETE ANYWAYS";
        CUSTOMIZATION_CANCEL_DELETE_BUTTON = "id:CANCEL";
        CORE_EXERCISE_RESTRICTION_POPUP_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, \"can't postpone/remove this practice\") and contains(@name, 'core exercise')]";
        CORE_EXERCISE_RESTRICTION_POPUP_BUTTON = "id:GOT IT";
        LOCKED_MODULE_POPUP_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Complete current module to unlock the next one')]";
        LOCKED_MODULE_POPUP_BUTTON = "id:GOT IT";

        LOCKED_NEXT_DAY_POPUP_TITLE = "xpath://XCUIElementTypeStaticText[@visible='true' and contains(@name, 'Finish today') and contains(@name, 'unlock the next day')]";
        LOCKED_NEXT_DAY_POPUP_MESSAGE = "xpath://XCUIElementTypeStaticText[contains(@name, 'move it to tomorrow')]";
        LOCKED_NEXT_DAY_POPUP_GIF = LOCKED_NEXT_DAY_POPUP_TITLE + "/../XCUIElementTypeImage[@visible='true']";
        LOCKED_NEXT_DAY_POPUP_BUTTON = "id:GOT IT";
    }

    public iOSDailyPlanPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    public static String firstLessonTitleLocator() {
        return DAILY_FIRST_LESSON_TITLE + " | " + MODULE_FIRST_LESSON_TITLE;
    }

    public static String lessonExitConfirmationLocator() {
        return KEGEL_PLAYER_EXIT_CONFIRM_TITLE + " | "
                + LESSON_EXIT_CONFIRM_TITLE.substring("xpath:".length());
    }

    @Override
    public void openFirstDailyLesson() {
        assertDailyLessonsAreDisplayed();
        WebElement title = waitForElementPresent(FIRST_LESSON_TITLE, "First lesson title is missing", 10);
        openedLessonTitle = getElementAccessibleName(title);
        Assert.assertFalse("First lesson title is empty", openedLessonTitle.isEmpty());
        title.click();
        waitForElementNotVisible("id:Today", "Daily Lesson did not replace Today", 15);
        waitForOpenedLessonTitle();
    }

    @Override
    public void assertDailyLessonScreenIsDisplayed() {
        waitForOpenedLessonTitle();
        waitForElementPresent(LESSON_SCREEN_BACK_BUTTON, "Daily Lesson close control is missing", 10);
        createWait(10).withMessage("Daily Lesson body content is not displayed").until(ignored -> {
            for (WebElement text : driver.findElements(getLocatorByString(LESSON_SCREEN_CONTENT))) {
                if (text.isDisplayed() && isLessonBodyText(getElementAccessibleName(text), openedLessonTitle)) {
                    return true;
                }
            }
            return false;
        });
    }

    protected static boolean isLessonBodyText(String text, String selectedTitle) {
        return text != null && text.trim().length() > 20 && !text.trim().equals(selectedTitle);
    }

    private void waitForOpenedLessonTitle() {
        Assert.assertNotNull("Open a Daily Lesson before verifying its title", openedLessonTitle);
        createWait(15).withMessage("Opened Daily Lesson does not show its selected title").until(ignored -> {
            for (WebElement text : driver.findElements(By.className("XCUIElementTypeStaticText"))) {
                if (text.isDisplayed() && openedLessonTitle.equals(getElementAccessibleName(text))) return true;
            }
            return false;
        });
    }

    public static String activeProgramTitleLocator() {
        // Current builds expose the program in the Today header next to
        // Bookmarks. Titles restored from accounts are not a fixed catalog.
        String header = "//XCUIElementTypeButton[@name='nav bar bookmarks' and @visible='true']"
                + "/parent::XCUIElementTypeOther/XCUIElementTypeOther[1]/XCUIElementTypeOther[1]"
                + "/XCUIElementTypeStaticText[@visible='true' and //XCUIElementTypeButton"
                + "[@name='Today' and @value='1' and @visible='true']]";
        // Keep the observed older-build labels only when this header is absent.
        return "xpath:" + header + " | //XCUIElementTypeStaticText[not(//XCUIElementTypeButton[@name='nav bar bookmarks' and @visible='true']) and //XCUIElementTypeButton[@name='Today' and @value='1' and @visible='true'] and @visible='true' and ("
                + "@name='LL' or @label='LL' or @name='KIH' or @label='KIH' "
                + "or @name='OH' or @label='OH' or @name='SIAS' or @label='SIAS' "
                + "or contains(translate(@name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'LAST LONGER') "
                + "or contains(translate(@name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'KEEP IT HARD') "
                + "or contains(translate(@name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'OVERALL HEALTH') "
                + "or contains(translate(@name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'SEX IS A SKILL') "
                + "or contains(translate(@name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'UNHOOKED') "
                + "or @name='Kegel Challenge' or @label='Kegel Challenge' "
                + "or @name='Path to Desire' or @name='Kegel Journey' "
                + "or contains(translate(@label, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'UNHOOKED'))]";
    }

    private static final String PLAYER_CONTAINER = "//XCUIElementTypeOther[XCUIElementTypeButton[@name='navBarRoundClose']"
            + " and .//XCUIElementTypeButton[@name='PlayerPauseIcon' or @name='PlayerPlayIcon']]";

    public static String kegelPlayerExerciseTitleLocator() {
        return "xpath:" + PLAYER_CONTAINER + "/XCUIElementTypeStaticText[@visible='true' and ("
                + "@name='Contract' or starts-with(@name,'Contract ') or @name='Rest' or @name='Waves' or @name='Squeeze')]";
    }

    public static String kegelPlayerWorkoutPhaseLocator() {
        return "xpath:" + PLAYER_CONTAINER + "/XCUIElementTypeCollectionView/XCUIElementTypeCell"
                + "/XCUIElementTypeStaticText[@visible='true' and (@name='SQUEEZE' or @name='WAVES'"
                + " or @name='SLOW LADDER' or @name='KICK' or @name='FLICKER' or @name='CLASP' or @name='INVERTED CLASP')]";
    }

    public static String kegelPlayerInfoContentLocator() {
        return "xpath://XCUIElementTypeCollectionView[.//XCUIElementTypeStaticText[contains(@name,'Kegels')]]"
                + "//XCUIElementTypeStaticText[@visible='true' and (contains(@name,'Contract your pelvic floor muscle')"
                + " or (parent::XCUIElementTypeOther[@name='LessonBulletTextView'] and string-length(normalize-space(@name))>20))]";
    }

    private static final String STRETCHING_SWITCH =
            "xpath://XCUIElementTypeOther[@name='KegelComponentViewSwitcherView']//XCUIElementTypeSwitch";

    @Override
    protected boolean isKegelStretchingVideoExpected() {
        if (getAmountElements(STRETCHING_SWITCH) == 0) {
            waitForElementVisible(PRACTICE_SCREEN_START_BUTTON, "Kegel workout start control is missing", 10);
            Assert.assertTrue("A workout without stretching must expose its exercise list",
                    getAmountElements(KEGEL_START_SCREEN_EXERCISE_ITEMS) > 0);
            return false;
        }
        String value = waitForElementVisible(STRETCHING_SWITCH,
                "Kegel stretching preference is not displayed", 10).getAttribute("value");
        Assert.assertTrue("Kegel stretching switch has an unknown state", "0".equals(value) || "1".equals(value));
        return "1".equals(value);
    }

    @Override
    protected void assertKegelPlayerPhaseControlsAreDisplayed() {
        waitForElementPresent("xpath:" + PLAYER_CONTAINER
                        + "/XCUIElementTypeCollectionView/XCUIElementTypeCell/XCUIElementTypeStaticText[@name='REST']",
                "Kegel player Rest phase control is not displayed", 10);
        waitForElementPresent(kegelPlayerWorkoutPhaseLocator(),
                "Kegel player workout phase control is not displayed", 10);
    }

    public static String dailyPlanCardLocator(String section, String title) {
        Assert.assertTrue("Daily Plan card title must be configured", title != null && !title.trim().isEmpty());
        return "xpath://XCUIElementTypeOther[@name='DailyPlanItem' and .//XCUIElementTypeStaticText[@name="
                + Quotes.escape(title) + "] and ancestor::XCUIElementTypeCell/preceding-sibling::XCUIElementTypeCell"
                + "[.//XCUIElementTypeStaticText[@name='TitleBlock.Title']][1]"
                + "//XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label=" + Quotes.escape(section) + "]]";
    }

    public static String unfinishedDailyPracticeCardLocator(String title) {
        return dailyPlanCardLocator("DAILY PRACTICE", title)
                + "[not(.//XCUIElementTypeImage[starts-with(@name,'dailyCompletedCheckmark') and @visible='true'])]";
    }

    @Step("Select the observed Daily Plan day without completing activities: {expectedDay}")
    public void selectObservedDay(String expectedDay) {
        Assert.assertTrue("Configure the observed Daily Plan fixture day", expectedDay != null && !expectedDay.trim().isEmpty());
        String target = expectedDay.trim().replaceAll("\\s+", " ");
        java.util.regex.Matcher desired = java.util.regex.Pattern.compile("^(Day|Stage) (\\d+)(?: of \\d+)?$").matcher(target);
        Assert.assertTrue("Unsupported Daily Plan fixture day: " + target, desired.matches());
        int targetNumber = Integer.parseInt(desired.group(2));
        for (int step = 0; step <= 60; step++) {
            String before = getCurrentDayLabel().trim().replaceAll("\\s+", " ");
            if (before.equals(target)) return;
            java.util.regex.Matcher current = java.util.regex.Pattern.compile("^(Day|Stage) (\\d+)(?: of \\d+)?$").matcher(before);
            Assert.assertTrue("Current Daily Plan day is unreadable: " + before, current.matches());
            Assert.assertEquals("Fixture day belongs to a different program day format", desired.group(1), current.group(1));
            int currentNumber = Integer.parseInt(current.group(2));
            Assert.assertNotEquals("Fixture day total differs from the selected program", targetNumber, currentNumber);
            waitForElementAndClick(currentNumber < targetNumber ? RIGHT_SWITCHER_ARROW : LEFT_SWITCHER_ARROW,
                    "Cannot navigate to the configured Daily Plan day", 10);
            createWait(15).withMessage("Daily Plan did not leave " + before).until(ignored ->
                    !before.equals(getCurrentDayLabel().trim().replaceAll("\\s+", " ")));
        }
        Assert.fail("Configured Daily Plan day is not reachable: " + target);
    }

    @Step("Hold the exact unfinished card in Daily Practice: {title}")
    public void holdIncompletePracticeCard(String title) {
        String target = unfinishedDailyPracticeCardLocator(title);
        for (int swipe = 0; swipe < 5 && !isElementVisible(target); swipe++) mobileSwipeUp();
        Assert.assertEquals("Fixture needs exactly one unfinished practice in DAILY PRACTICE: " + title,
                1, getAmountElements(target));
        WebElement card = waitForElementVisible(target, "Configured unfinished practice is not visible: " + title, 10);
        touchAndHoldElement(card, .8);
        Allure.addAttachment("Daily Practice hold result", "application/xml",
                TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
    }

    @Step("Verify the same unfinished practice remains after cancellation: {title}")
    public void assertIncompletePracticeStillDisplayed(String title) {
        String target = unfinishedDailyPracticeCardLocator(title);
        waitForElementVisible(target, "The selected unfinished practice disappeared after cancellation: " + title, 10);
        Assert.assertEquals("The selected practice was removed or duplicated", 1, getAmountElements(target));
    }

    @Step("Verify mandatory Kegel rejects postponement and removal: {title}")
    public void assertCorePracticeCannotBeCustomized(String title) {
        String before = getCurrentDayLabel();
        holdIncompletePracticeCard(title);
        waitForElementVisible(CORE_EXERCISE_RESTRICTION_POPUP_TITLE,
                "Mandatory Kegel did not show the postpone/remove restriction", 10);
        waitForElementEnabled(CORE_EXERCISE_RESTRICTION_POPUP_BUTTON, "Restriction acknowledgment is unavailable", 10);
        Assert.assertFalse("Mandatory Kegel unexpectedly exposed a remove action",
                isElementVisible(CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON));
        Assert.assertFalse("Mandatory Kegel unexpectedly exposed a postpone action",
                isElementVisible(CUSTOMIZATION_MOVE_TO_TOMORROW_BUTTON));
        closeCoreExerciseRestrictionPopup();
        assertIncompletePracticeStillDisplayed(title);
        Assert.assertEquals("Mandatory Kegel changed the selected day", before, getCurrentDayLabel());
    }

    @Step("Verify a Daily Lesson exposes no postpone/remove actions")
    public void assertDailyLessonCannotBeCustomized() {
        assertDailyLessonsAreDisplayed();
        String before = getCurrentDayLabel();
        String title = getElementAccessibleName(waitForElementVisible(FIRST_LESSON_TITLE, "First lesson is not visible", 10));
        String target = dailyPlanCardLocator("DAILY LESSONS", title);
        Assert.assertEquals("Lesson fixture must identify one card in DAILY LESSONS", 1, getAmountElements(target));
        touchAndHoldElement(waitForElementVisible(target, "Cannot hold the lesson card", 10), .8);
        Allure.addAttachment("Daily Lesson hold result", "application/xml",
                TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
        Assert.assertFalse("A lesson unexpectedly exposed customization controls",
                isCustomizationActionsSheetDisplayed() || isElementVisible(CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON)
                        || isCoreExerciseRestrictionPopupDisplayed());
        waitForElementVisible(target, "Lesson disappeared after holding its card", 10);
        Assert.assertEquals("Holding a lesson changed the selected day", before, getCurrentDayLabel());
    }

    @Step("Dismiss Daily Plan customization without deleting or postponing")
    public void dismissCustomizationIfPresent() {
        if (isElementVisible(CUSTOMIZATION_DELETE_CONFIRM_BUTTON)) {
            cancelRemoveFromDailyPlan();
        } else if (isCoreExerciseRestrictionPopupDisplayed()) {
            closeCoreExerciseRestrictionPopup();
        } else if (isElementVisible(CUSTOMIZATION_MOVE_TO_TOMORROW_BUTTON)
                || isElementVisible(CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON)) {
            waitForElementAndClick(TAB_TODAY, "Cannot dismiss the customization sheet", 10);
            waitForElementNotVisible(CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON,
                    "Customization sheet did not dismiss", 10);
        }
    }

    boolean isDailyLessonVisible() {
        return (isElementVisible(LESSON_SCREEN_TITLE) && isElementVisible(LESSON_SCREEN_BACK_BUTTON))
                || isLessonExitConfirmationVisible();
    }

    boolean isKegelExerciseFlowVisible() {
        return isElementVisible(KEGEL_PLAYER_PAUSE_BUTTON) || isElementVisible(KEGEL_PLAYER_PLAY_BUTTON)
                || isElementVisible(KEGEL_MEDIA_PLAYER_BACK_BUTTON) || isElementVisible(PRACTICE_SCREEN_START_BUTTON)
                || isElementVisible(PRACTICE_SCREEN_CLOSE_BUTTON) || isElementVisible(KEGEL_STRETCHING_COMPLETION_LETS_GO_BUTTON)
                || isElementVisible(PRACTICE_COMPLETION_FEEDBACK_TITLE)
                || (isElementVisible(KEGEL_PLAYER_INFO_MODAL_TITLE) && isElementVisible(KEGEL_PLAYER_INFO_MODAL_CLOSE_BUTTON));
    }

    void closeCompletedPracticeIfPresent() {
        if (isElementVisible(PRACTICE_COMPLETION_FEEDBACK_TITLE)) closePracticeCompletionFeedbackIfPresent();
    }

    void closeDailyLessonIfPresent() {
        if (isDailyLessonVisible()) returnFromDailyLessonToDailyPlan();
    }

    private boolean isLessonExitConfirmationVisible() {
        return isElementVisible(lessonExitConfirmationLocator());
    }

    @Override
    @io.qameta.allure.Step("Close iOS Daily Lesson and confirm unfinished exit")
    public void returnFromDailyLessonToDailyPlan() {
        String confirmation = lessonExitConfirmationLocator();
        if (!isElementVisible(confirmation)) {
            waitForElementAndClick(LESSON_SCREEN_BACK_BUTTON, "Cannot close Daily Lesson", 10);
        }
        waitForFirstElementPresent(new String[]{"id:Today", confirmation},
                "Daily Lesson neither closed nor requested exit confirmation", 10);
        if (isElementVisible(confirmation)) {
            waitForElementAndClick(KEGEL_PLAYER_EXIT_CONFIRM_QUIT_BUTTON,
                    "Cannot confirm Daily Lesson exit", 10);
        }
        waitForElementPresent("id:Today", "Today did not return after closing Daily Lesson", 15);
        waitForElementNotVisible(confirmation,
                "Lesson exit dialog remained open", 10);
    }

    @Override
    protected void assertLockedNextDayPopupImage() {
        WebElement tutorial = waitForElementPresent(LOCKED_NEXT_DAY_POPUP_GIF,
                "Postpone tutorial image is missing from the popup", 10);
        final Rectangle bounds = tutorial.getRect();
        final int[][] initialFrame = {null};
        createWait(12).withMessage("Postpone GIF is blank or is not animating").until(webDriver -> {
            try {
                BufferedImage screenshot = ImageIO.read(new ByteArrayInputStream(
                        ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
                double scale = screenshot.getWidth() / (double) driver.manage().window().getSize().getWidth();
                int[] pixels = new int[32 * 32];
                Set<Integer> colors = new HashSet<Integer>();
                for (int y = 0; y < 32; y++) {
                    for (int x = 0; x < 32; x++) {
                        // Ignore borders/the close icon; inspect the image interior.
                        int sx = (int) ((bounds.getX() + bounds.getWidth() * (.16 + .68 * x / 31)) * scale);
                        int sy = (int) ((bounds.getY() + bounds.getHeight() * (.16 + .68 * y / 31)) * scale);
                        int color = screenshot.getRGB(sx, sy) & 0x00F8F8F8;
                        pixels[y * 32 + x] = color;
                        colors.add(color);
                    }
                }
                if (colors.size() < 8) return false;
                if (initialFrame[0] == null) {
                    initialFrame[0] = pixels;
                    return false;
                }
                int changed = 0;
                for (int i = 0; i < pixels.length; i++) if (pixels[i] != initialFrame[0][i]) changed++;
                return changed >= 8;
            } catch (java.io.IOException error) {
                throw new IllegalStateException("Cannot inspect the rendered postpone GIF", error);
            }
        });
    }

    @Override
    public void openProgramSelector() {
        super.openProgramSelector();
        selectorTop = waitForElementPresent(SELECTOR_CARDS, "Program selector cards did not appear", 10).getRect().y;
    }

    @Override
    public List<String> getProgramNamesFromSelector() {
        return new iOSProgramListReader(driver).collect(SELECTOR_TITLES, SELECTOR_COLLECTION, false);
    }

    @Override
    public void closeProgramSelector() {
        Map<String,Object> tap = new HashMap<String,Object>();
        tap.put("x", driver.manage().window().getSize().getWidth() / 2);
        tap.put("y", Math.max(20, selectorTop - 50));
        ((JavascriptExecutor) driver).executeScript("mobile: tap", tap);
        waitForElementNotPresent(SELECTOR_CARDS, "Program selector did not close after tapping outside", 10);
    }
}
