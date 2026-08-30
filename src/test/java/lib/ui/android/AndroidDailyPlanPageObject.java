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

    static {
        TAB_TODAY = "id:" + ID_PREFIX + "nav_graph_daily";
        SELECTED_TODAY_TAB =
                "xpath://android.widget.FrameLayout[@resource-id='" + ID_PREFIX + "nav_graph_daily' and @selected='true']";
        ACTIVE_PROGRAM_TITLE = "id:" + ID_PREFIX + "tvProgramName";
        CURRENT_DAY_LABEL = "id:" + ID_PREFIX + "tvDaysNumber";
        DAILY_PLAN_DAY_SWITCHER = CURRENT_DAY_LABEL;
        LEFT_SWITCHER_ARROW = "id:" + ID_PREFIX + "ivBack";
        RIGHT_SWITCHER_ARROW = "id:" + ID_PREFIX + "ivNext";

        // Daily Lessons are not exposed by the current Android screen. Keep
        // these unset because the Android suite has a separate, explicit
        // Daily Practice assertion rather than a false iOS parity assertion.
        DAILY_LESSONS_TITLE = null;
        FIRST_LESSON_TITLE = null;
        FIRST_LESSON_TYPE = null;
        LESSON_SCREEN_TITLE = null;
        LESSON_SCREEN_CONTENT = null;
        LESSON_SCREEN_BACK_BUTTON = null;

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

        // The current Android smoke path intentionally stops at the start
        // screen. These fields are initialized to safe locators so inherited
        // cleanup never dereferences null locators if a test fails there.
        KEGEL_MEDIA_PLAYER_BACK_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_STRETCHING_COMPLETION_LETS_GO_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_BACK_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_MUTE_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_UNMUTE_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_EXERCISE_TITLE = "id:" + ID_PREFIX + "tvTitle";
        KEGEL_PLAYER_DIFFICULTY = "id:" + ID_PREFIX + "tvTitle";
        KEGEL_PLAYER_TIMER = "id:" + ID_PREFIX + "tvTitle";
        KEGEL_PLAYER_PAUSE_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_PLAY_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_REWIND_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_REWIND_BACK_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_REWIND_FORWARD_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_VIBRATION_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_VIBRATION_ON_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_VIBRATION_OFF_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_INFO_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_INFO_TOOLTIP = "id:" + ID_PREFIX + "tvTitle";
        KEGEL_PLAYER_INFO_MODAL_TITLE = "id:" + ID_PREFIX + "tvTitle";
        KEGEL_PLAYER_INFO_MODAL_CONTENT = "id:" + ID_PREFIX + "tvTitle";
        KEGEL_PLAYER_INFO_MODAL_CLOSE_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_PHASE_SQUEEZE_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_PHASE_REST_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_PHASE_WAVES_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        KEGEL_PLAYER_EXIT_CONFIRM_TITLE = "id:" + ID_PREFIX + "tvDialogtitle";
        KEGEL_PLAYER_EXIT_CONFIRM_QUIT_BUTTON = "id:" + ID_PREFIX + "btnYes";
        KEGEL_PLAYER_EXIT_CONFIRM_CONTINUE_BUTTON = "id:" + ID_PREFIX + "btnNo";
        PRACTICE_COMPLETION_FEEDBACK_TITLE = "id:" + ID_PREFIX + "tvTitle";
        PRACTICE_COMPLETION_FEEDBACK_CLOSE_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        PRACTICE_COMPLETION_INTENSITY_TOO_EASY = "id:" + ID_PREFIX + "tvTitle";
        PRACTICE_COMPLETION_INTENSITY_GREAT = "id:" + ID_PREFIX + "tvTitle";
        PRACTICE_COMPLETION_INTENSITY_TOO_HARD = "id:" + ID_PREFIX + "tvTitle";
        CUSTOMIZATION_MOVE_TO_TOMORROW_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        CUSTOMIZATION_DELETE_CONFIRM_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        CUSTOMIZATION_CANCEL_DELETE_BUTTON = "id:" + ID_PREFIX + "btnNavigateUp";
        CORE_EXERCISE_RESTRICTION_POPUP_TITLE = "id:" + ID_PREFIX + "tvDialogtitle";
        CORE_EXERCISE_RESTRICTION_POPUP_BUTTON = "id:" + ID_PREFIX + "btnNo";
        LOCKED_MODULE_POPUP_TITLE = "id:" + ID_PREFIX + "tvDialogtitle";
        LOCKED_MODULE_POPUP_BUTTON = "id:" + ID_PREFIX + "btnNo";
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
        waitForElementPresent(FIRST_PRACTICE_TITLE, "Custom Kegel Training card is not displayed", 15);
        waitForElementPresent(FIRST_PRACTICE_TYPE, "Daily Practice card type is not displayed", 15);
        Assert.assertTrue(
                "Custom Kegel Training card must be tappable",
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
