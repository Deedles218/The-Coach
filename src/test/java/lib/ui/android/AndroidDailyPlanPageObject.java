package lib.ui.android;

import io.qameta.allure.Step;
import lib.ui.DailyPlanPageObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.junit.Assert;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily Plan contract for the Android 1.40.x build.
 *
 * The Android screen currently exposes the program stage and Daily Practice
 * cards, while the iOS fixture-oriented object also expects Daily Lessons.
 * Keep the Android smoke contract tied to the controls actually exposed by
 * the Android accessibility tree instead of silently reusing iOS locators.
 */
public class AndroidDailyPlanPageObject extends DailyPlanPageObject {
    private static final String APP_PACKAGE = "com.vamapps.thecoach";
    private static final String ID_PREFIX = APP_PACKAGE + ":id/";

    private static final String DAILY_PRACTICE_SECTION_TITLE =
            "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tvGroupName' and @text='DAILY PRACTICE']";
    private static final String FIRST_PRACTICE_CARD =
            "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tvGroupName' and @text='DAILY PRACTICE']"
                    + "/parent::android.widget.LinearLayout/following-sibling::android.view.ViewGroup[1]"
                    + "//androidx.cardview.widget.CardView[@resource-id='" + ID_PREFIX + "cvLessonContainer']";
    private static final String FIRST_PRACTICE_TITLE_IN_SECTION =
            FIRST_PRACTICE_CARD + "//android.widget.TextView[@resource-id='" + ID_PREFIX + "tvLessonName']";
    private static final String FIRST_PRACTICE_TYPE_IN_SECTION =
            FIRST_PRACTICE_CARD + "//android.widget.TextView[@resource-id='" + ID_PREFIX + "tvTagText']";

    private static String aid(String name) { return "id:" + ID_PREFIX + name; }
    private static String at(String text) { return "xpath://*[@text='" + text + "']"; }
    private void scrollList(String id, String direction) {
        WebElement list = waitForElementVisible(aid(id), "Android scroll container absent: " + id, 10);
        Map<String,Object> args = new HashMap<String,Object>();
        args.put("elementId", ((RemoteWebElement)list).getId());
        args.put("direction", direction); args.put("percent", .8);
        ((JavascriptExecutor)driver).executeScript("mobile: scrollGesture", args);
    }
    private void findInToday(String locator) {
        for (int i=0; i<8; i++) {
            if (isElementVisible(locator)) return;
            scrollList("rvQuestions", "down");
        }
        waitForElementVisible(locator, "Required Today fixture card is absent", 1);
    }
    @Override public String getActiveProgramName() {
        scrollQuestionsToTopIfPresent();
        return waitForElementVisible("xpath://*[@resource-id='" + ID_PREFIX + "headerContainer']//*[@resource-id='" + ID_PREFIX + "tvGroupName']",
                "Today program name is absent", 10).getText();
    }
    @Override public String getProgramProgressValue() {
        scrollQuestionsToTopIfPresent();
        return waitForElementVisible(aid("tvPercentages"), "Program progress is absent", 10).getText();
    }
    @Override public void assertDailyLessonsAreDisplayed() {
        findInToday(DAILY_LESSONS_TITLE);
        super.assertDailyLessonsAreDisplayed();
    }
    @Override public void returnFromDailyLessonToDailyPlan() {
        waitForElementAndClick(aid("ivButtonClose"), "Cannot close Android lesson", 10);
        if (isElementVisible(aid("tvDialogtitle"))) {
            waitForElementAndClick(aid("btnYes"), "Cannot quit unfinished lesson", 10);
        }
        waitForElementVisible(CURRENT_DAY_LABEL, "Today did not return", 15);
    }
    @Override public void openFirstDailyPractice() {
        assertDailyPracticeIsDisplayed();
        waitForElementAndClick(FIRST_PRACTICE_CARD, "Cannot open practice", 10);
        waitForFirstElementPresent(new String[]{aid("tvGoalTitle"),aid("viewPlayer"),aid("tvControlHeader")}, "Practice start or guide video is absent", 20);
    }
    @Override public void assertDailyPracticeScreenIsDisplayed() {
        if(isElementVisible(aid("tvControlHeader"))) {
            waitForElementVisible(aid("tvTextContent"),"Practice instructions are absent",10);
            waitForElementEnabled(aid("bContinueLesson"),"Practice Continue is unavailable",10);
            return;
        }
        if(isElementVisible(aid("viewPlayer"))) {
            new AndroidExplorePageObject(driver).assertVideoPlayerControlsAreDisplayed();
            return;
        }
        waitForElementVisible(aid("tvTitle"), "Practice title absent", 10);
        waitForElementVisible(aid("tvGoalBody"), "Practice goal absent", 10);
        waitForElementEnabled(aid("btStartWork"), "Practice start unavailable", 10);
    }
    @Override public void returnFromDailyPracticeToDailyPlan() {
        waitForElementAndClick(isElementVisible(aid("tvControlHeader"))?aid("ivButtonClose"):aid("btnNavigateUp"), "Cannot close practice", 10);
        waitForFirstElementPresent(new String[]{aid("tvDialogtitle"),aid("rvQuestions")},"Practice did not reach exit confirmation or Today",10);
        if(isElementVisible(aid("tvDialogtitle")))waitForElementAndClick(aid("btnYes"),"Cannot quit unfinished practice",10);
        waitForElementVisible(aid("rvQuestions"), "Today did not return", 15);
    }
    @Override public void assertRightArrowDoesNotChangeCurrentDayWhenLocked() {
        // Modules permit navigation inside a module; only the next module is locked.
        AndroidModulesPageObject modules = new AndroidModulesPageObject(driver);
        lib.ModuleStage original = modules.position();
        try {
            for(int n=original.stage;n<original.total;n++) {
                modules.next(); modules.waitForPosition(original.at(n+1));
            }
            modules.next(); modules.assertBlocked(original.at(original.total));
        } finally { modules.restore(original); }
        if (original.stage == 1) assertLeftArrowKeepsCurrentDaySelected();
    }
    @Override public void mobileSwipeUp() { scrollList("rvQuestions", "down"); }
    @Override public void mobileSwipeDown() { scrollList("rvQuestions", "up"); }

    static {
        TAB_TODAY = "id:" + ID_PREFIX + "nav_graph_daily";
        SELECTED_TODAY_TAB =
                "xpath://android.widget.FrameLayout[@resource-id='" + ID_PREFIX + "nav_graph_daily' and @selected='true']";
        ACTIVE_PROGRAM_TITLE = "id:" + ID_PREFIX + "tvProgramName";
        CURRENT_DAY_LABEL = "id:" + ID_PREFIX + "tvDaysNumber";
        DAILY_PLAN_DAY_SWITCHER = CURRENT_DAY_LABEL;
        LEFT_SWITCHER_ARROW = "id:" + ID_PREFIX + "ivBack";
        RIGHT_SWITCHER_ARROW = "id:" + ID_PREFIX + "ivNext";

        // Lesson IDs observed on the Last Longer first-stage fixture.
        DAILY_LESSONS_TITLE = at("DAILY LESSONS");
        FIRST_LESSON_TITLE = "xpath:(//*[@resource-id='" + ID_PREFIX + "cvLessonContainer' and .//*[@resource-id='" + ID_PREFIX + "tvTagText' and starts-with(@text,'Lesson ')]])[1]//*[@resource-id='" + ID_PREFIX + "tvLessonName']";
        FIRST_LESSON_TYPE = "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tvTagText' and starts-with(@text,'Lesson ')]";
        LESSON_SCREEN_TITLE = aid("tvControlHeader");
        LESSON_SCREEN_CONTENT = aid("tvTextContent");
        LESSON_SCREEN_BACK_BUTTON = aid("ivButtonClose");

        DAILY_PRACTICE_TITLE =
                "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tvGroupName' and @text='DAILY PRACTICE']";
        // The backend can return a time-of-day suffix (for example
        // "Custom Kegel (Morning)" or "Custom Kegel (Evening)") and can
        // rename the generated practice. Assert the stable card contract and
        // the Daily tag rather than freezing one fixture title.
        FIRST_PRACTICE_TITLE = FIRST_PRACTICE_TITLE_IN_SECTION;
        FIRST_PRACTICE_TYPE = FIRST_PRACTICE_TYPE_IN_SECTION;
        PRACTICE_SCREEN_TITLE =
                "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tvTitle' and @text='CUSTOM KEGEL WORKOUT']";
        PRACTICE_SCREEN_GOAL_TITLE = "id:" + ID_PREFIX + "tvGoalTitle";
        PRACTICE_SCREEN_EXERCISES_TITLE = "id:" + ID_PREFIX + "tvExercisesTitle";
        PRACTICE_SCREEN_START_BUTTON = "id:" + ID_PREFIX + "btStartWork";
        PRACTICE_SCREEN_CLOSE_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_DAILY_PLAN_TITLE = FIRST_PRACTICE_TITLE;
        KEGEL_START_SCREEN_TITLE = PRACTICE_SCREEN_TITLE;
        KEGEL_START_SCREEN_DESCRIPTION = "id:" + ID_PREFIX + "tvGoalBody";
        KEGEL_START_SCREEN_LEVEL = null;
        KEGEL_START_SCREEN_DURATION = "id:" + ID_PREFIX + "tvWorkutDuration";
        KEGEL_START_SCREEN_DURATION_VALUE =
                "xpath://android.widget.TextView[@resource-id='" + ID_PREFIX + "tvWorkutDuration' and contains(@text,'sec')]";
        KEGEL_START_SCREEN_INTENSITY = "id:" + ID_PREFIX + "tvWorkutIntensity";
        KEGEL_START_SCREEN_INTENSITY_VALUE = KEGEL_START_SCREEN_INTENSITY;
        KEGEL_START_SCREEN_STRETCHING_TITLE = "id:" + ID_PREFIX + "tvMainTitleStr";
        KEGEL_START_SCREEN_STRETCHING_DESCRIPTION = "id:" + ID_PREFIX + "tvInfoTextStr";
        KEGEL_START_SCREEN_EXERCISE_TITLE = "id:" + ID_PREFIX + "tvMainTitleStr";
        KEGEL_START_SCREEN_EXERCISE_ITEMS = "id:" + ID_PREFIX + "svStrecting";
        KEGEL_START_SCREEN_EXERCISE_METADATA = "id:" + ID_PREFIX + "tvValue";
        KEGEL_START_SCREEN_FIRST_EXERCISE = "id:" + ID_PREFIX + "rvActions";

        // Player operations live in AndroidKegelPageObject. No unrelated button
        // may act as a fallback for an unimplemented control.
        CUSTOMIZATION_MOVE_TO_TOMORROW_BUTTON = null; // Not implemented in the Android product.
        CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON = null;
        CUSTOMIZATION_DELETE_CONFIRM_BUTTON = null;
        CUSTOMIZATION_CANCEL_DELETE_BUTTON = null;
        CORE_EXERCISE_RESTRICTION_POPUP_TITLE = aid("tvDialogtitle");
        CORE_EXERCISE_RESTRICTION_POPUP_BUTTON = aid("btnNo");
        LOCKED_MODULE_POPUP_TITLE = "xpath://*[@resource-id='" + ID_PREFIX + "tvTitle' and @text='Complete current module to unlock the next one']";
        LOCKED_MODULE_POPUP_BUTTON = aid("okButton");
        LOCKED_NEXT_DAY_POPUP_TITLE = "id:" + ID_PREFIX + "tvDialogtitle";
        LOCKED_NEXT_DAY_POPUP_MESSAGE = "id:" + ID_PREFIX + "tvDialogtitle";
        LOCKED_NEXT_DAY_POPUP_BUTTON = "id:" + ID_PREFIX + "btnNo";
    }

    public AndroidDailyPlanPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Open Android Today tab")
    public void openTodayTab() {
        waitForFirstElementAndClick(
                new String[]{TAB_TODAY},
                "Cannot tap Android Today tab",
                15
        );
        waitForElementPresent(SELECTED_TODAY_TAB, "Android Today tab is not selected", 15);
        scrollQuestionsToTopIfPresent();
        waitForElementPresent(CURRENT_DAY_LABEL, "Android Daily Plan stage is not displayed", 15);
    }

    @Override
    @Step("Verify Android Daily Plan smoke content")
    public void assertDailyPracticeIsDisplayed() {
        scrollToDailyPracticeSection();
        waitForElementPresent(DAILY_PRACTICE_SECTION_TITLE, "DAILY PRACTICE section is not displayed", 15);
        waitForElementPresent(FIRST_PRACTICE_TITLE, "Daily Practice card is not displayed", 15);
        waitForElementPresent(FIRST_PRACTICE_TYPE, "Daily Practice card type is not displayed", 15);
        Assert.assertTrue(
                "Daily Practice card must be tappable",
                isElementEnabled(waitForElementPresent(FIRST_PRACTICE_CARD, "Daily Practice card is not displayed", 15))
        );
    }

    @Override
    @Step("Open Android Custom Kegel start screen")
    public void openKegelExerciseFromDailyPlan() {
        assertDailyPracticeIsDisplayed();
        waitForElementAndClick(
                FIRST_PRACTICE_CARD,
                "Cannot open Custom Kegel Training from Android Daily Practice",
                15
        );
        waitForElementPresent(
                KEGEL_START_SCREEN_TITLE,
                "Android Custom Kegel start screen did not open",
                20
        );
        waitForLoadingToDisappearIfPresent(
                TEST_ID_LOADING,
                "Android Custom Kegel start screen is still loading",
                20
        );
    }

    @Override
    @Step("Verify Android Custom Kegel start screen")
    public void assertKegelStartScreenIsDisplayed() {
        waitForElementPresent(KEGEL_START_SCREEN_TITLE, "Custom Kegel title is not displayed", 15);
        waitForElementPresent(KEGEL_START_SCREEN_DESCRIPTION, "Custom Kegel goal is not displayed", 15);
        waitForElementPresent(KEGEL_START_SCREEN_DURATION, "Custom Kegel duration is not displayed", 15);
        waitForElementPresent(KEGEL_START_SCREEN_INTENSITY, "Custom Kegel intensity is not displayed", 15);
        waitForElementPresent(PRACTICE_SCREEN_GOAL_TITLE, "Custom Kegel GOAL section is not displayed", 15);
        waitForElementPresent(PRACTICE_SCREEN_EXERCISES_TITLE, "Custom Kegel EXERCISES section is not displayed", 15);
        waitForElementPresent(KEGEL_START_SCREEN_EXERCISE_TITLE, "Custom Kegel exercise item is not displayed", 15);
        Assert.assertTrue(
                "Custom Kegel START WORKOUT button must be enabled",
                isElementEnabled(waitForElementPresent(PRACTICE_SCREEN_START_BUTTON, "START WORKOUT button is not displayed", 15))
        );
        Assert.assertTrue(
                "Custom Kegel close control must be enabled",
                isElementEnabled(waitForElementPresent(PRACTICE_SCREEN_CLOSE_BUTTON, "Custom Kegel close control is not displayed", 15))
        );
    }

    @Override
    @Step("Close Android Custom Kegel start screen")
    public void closeKegelExerciseFlowIfPresent() {
        if (isElementPresent(KEGEL_START_SCREEN_TITLE)) {
            waitForElementAndClick(
                    PRACTICE_SCREEN_CLOSE_BUTTON,
                    "Cannot close Android Custom Kegel start screen",
                    10
            );
            waitForElementPresent(DAILY_PRACTICE_TITLE, "Daily Practice did not return after closing Custom Kegel", 15);
        }
    }

    private void scrollQuestionsToTopIfPresent() {
        List<WebElement> scrollViews = driver.findElements(By.id(ID_PREFIX + "rvQuestions"));
        for (WebElement scrollView : scrollViews) {
            try {
                Map<String, Object> args = new HashMap<String, Object>();
                args.put("elementId", ((RemoteWebElement) scrollView).getId());
                args.put("direction", "up");
                args.put("percent", 1.0);
                ((JavascriptExecutor) driver).executeScript("mobile: scrollGesture", args);
                return;
            } catch (WebDriverException ignored) {
                // The list can be replaced while Today is loading.
            }
        }
    }

    private void scrollToDailyPracticeSection() {
        for (int attempt = 0; attempt < 6; attempt++) {
            if (isElementPresent(FIRST_PRACTICE_TITLE_IN_SECTION)
                    && isElementPresent(FIRST_PRACTICE_TYPE_IN_SECTION)) {
                return;
            }

            List<WebElement> scrollViews = driver.findElements(By.id(ID_PREFIX + "rvQuestions"));
            for (WebElement scrollView : scrollViews) {
                try {
                    Map<String, Object> args = new HashMap<String, Object>();
                    args.put("elementId", ((RemoteWebElement) scrollView).getId());
                    args.put("direction", "down");
                    args.put("percent", 0.85);
                    ((JavascriptExecutor) driver).executeScript("mobile: scrollGesture", args);
                    break;
                } catch (WebDriverException ignored) {
                    // Today can re-render the RecyclerView while the next
                    // practice card is being materialized.
                }
            }
        }
    }
}
