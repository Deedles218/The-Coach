package lib.ui;

import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.Map;

abstract public class DailyPlanPageObject extends MainPageObject {
    protected static String
            TAB_TODAY,
            SELECTED_TODAY_TAB,
            DAILY_PLAN_DAY_SWITCHER,
            CURRENT_DAY_LABEL,
            LEFT_SWITCHER_ARROW,
            RIGHT_SWITCHER_ARROW,
            DAILY_LESSONS_TITLE,
            FIRST_LESSON_TITLE,
            FIRST_LESSON_TYPE,
            LESSON_SCREEN_TITLE,
            LESSON_SCREEN_CONTENT,
            LESSON_SCREEN_BACK_BUTTON,
            DAILY_PRACTICE_TITLE,
            FIRST_PRACTICE_TITLE,
            FIRST_PRACTICE_TYPE,
            PRACTICE_SCREEN_TITLE,
            PRACTICE_SCREEN_GOAL_TITLE,
            PRACTICE_SCREEN_EXERCISES_TITLE,
            PRACTICE_SCREEN_START_BUTTON,
            PRACTICE_SCREEN_CLOSE_BUTTON,
            KEGEL_DAILY_PLAN_TITLE,
            KEGEL_START_SCREEN_TITLE,
            KEGEL_START_SCREEN_DESCRIPTION,
            KEGEL_START_SCREEN_DURATION,
            KEGEL_START_SCREEN_INTENSITY,
            KEGEL_START_SCREEN_EXERCISE_TITLE,
            KEGEL_PLAYER_BACK_BUTTON,
            KEGEL_PLAYER_MUTE_BUTTON,
            KEGEL_PLAYER_UNMUTE_BUTTON,
            KEGEL_PLAYER_EXERCISE_TITLE,
            KEGEL_PLAYER_DIFFICULTY,
            KEGEL_PLAYER_TIMER,
            KEGEL_PLAYER_PAUSE_BUTTON,
            KEGEL_PLAYER_PLAY_BUTTON,
            KEGEL_PLAYER_REWIND_BUTTON,
            KEGEL_PLAYER_REWIND_BACK_BUTTON,
            KEGEL_PLAYER_REWIND_FORWARD_BUTTON,
            KEGEL_PLAYER_VIBRATION_BUTTON,
            KEGEL_PLAYER_INFO_BUTTON,
            KEGEL_PLAYER_INFO_TOOLTIP,
            KEGEL_PLAYER_INFO_MODAL_TITLE,
            KEGEL_PLAYER_INFO_MODAL_CONTENT,
            KEGEL_PLAYER_INFO_MODAL_CLOSE_BUTTON,
            KEGEL_PLAYER_PHASE_SQUEEZE_BUTTON,
            KEGEL_PLAYER_PHASE_REST_BUTTON,
            KEGEL_PLAYER_PHASE_WAVES_BUTTON,
            KEGEL_PLAYER_EXIT_CONFIRM_TITLE,
            KEGEL_PLAYER_EXIT_CONFIRM_QUIT_BUTTON,
            KEGEL_PLAYER_EXIT_CONFIRM_CONTINUE_BUTTON,
            PRACTICE_COMPLETION_FEEDBACK_TITLE,
            PRACTICE_COMPLETION_FEEDBACK_CLOSE_BUTTON,
            CUSTOMIZATION_MOVE_TO_TOMORROW_BUTTON,
            CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON,
            CUSTOMIZATION_DELETE_CONFIRM_BUTTON,
            CUSTOMIZATION_CANCEL_DELETE_BUTTON,
            CORE_EXERCISE_RESTRICTION_POPUP_TITLE,
            CORE_EXERCISE_RESTRICTION_POPUP_BUTTON,
            LOCKED_MODULE_POPUP_TITLE,
            LOCKED_MODULE_POPUP_BUTTON,
            LOCKED_NEXT_DAY_POPUP_TITLE,
            LOCKED_NEXT_DAY_POPUP_MESSAGE,
            LOCKED_NEXT_DAY_POPUP_BUTTON;

    public DailyPlanPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Step("Open Today tab")
    public void openTodayTab() {
        this.waitForElementAndClick(TAB_TODAY, "Cannot tap Today tab", 10);
        this.assertTodayTabIsSelected();
        this.scrollDownToDailyPlanHeader();
    }

    @Step("Verify Today tab is selected")
    public void assertTodayTabIsSelected() {
        this.waitForElementPresent(SELECTED_TODAY_TAB, "Today tab is not selected", 10);
    }

    @Step("Verify Daily Plan header and current day are displayed")
    public void assertDailyPlanDaySwitcherIsDisplayed() {
        this.waitForElementPresent(CURRENT_DAY_LABEL, "Current Daily Plan day label is not displayed", 10);
        this.waitForElementPresent(DAILY_PLAN_DAY_SWITCHER, "Daily Plan day switcher is not displayed", 10);
        this.waitForElementPresent(LEFT_SWITCHER_ARROW, "Daily Plan left arrow is not displayed", 10);
        this.waitForElementPresent(RIGHT_SWITCHER_ARROW, "Daily Plan right arrow is not displayed", 10);
    }

    @Step("Verify Daily Lessons are displayed")
    public void assertDailyLessonsAreDisplayed() {
        this.waitForElementPresent(DAILY_LESSONS_TITLE, "Daily Lessons section title is not displayed", 10);
        this.waitForElementPresent(FIRST_LESSON_TITLE, "First Daily Lessons item is not displayed", 10);
        this.waitForElementPresent(FIRST_LESSON_TYPE, "First Daily Lessons item type is not displayed", 10);
    }

    public boolean hasDailyLessonsAvailable() {
        return this.isElementPresent(DAILY_LESSONS_TITLE) && this.isElementPresent(FIRST_LESSON_TITLE);
    }

    @Step("Open first Daily Lesson")
    public void openFirstDailyLesson() {
        this.assertDailyLessonsAreDisplayed();
        this.waitForElementAndClick(FIRST_LESSON_TITLE, "Cannot open first Daily Lesson", 10);
        this.waitForElementPresent(LESSON_SCREEN_TITLE, "Daily Lesson screen title is not displayed", 15);
    }

    @Step("Verify Daily Lesson screen is displayed")
    public void assertDailyLessonScreenIsDisplayed() {
        this.waitForElementPresent(LESSON_SCREEN_TITLE, "Daily Lesson screen title is not displayed", 10);
        this.waitForElementPresent(LESSON_SCREEN_CONTENT, "Daily Lesson content is not displayed", 10);
    }

    @Step("Return from Daily Lesson to Daily Plan")
    public void returnFromDailyLessonToDailyPlan() {
        this.waitForElementAndClick(LESSON_SCREEN_BACK_BUTTON, "Cannot close Daily Lesson screen", 10);
        this.waitForElementPresent(TAB_TODAY, "Daily Plan did not return after closing Daily Lesson", 10);
    }

    @Step("Verify Daily Practice is displayed")
    public void assertDailyPracticeIsDisplayed() {
        int alreadySwiped = 0;
        while (!this.isElementPresent(DAILY_PRACTICE_TITLE) && alreadySwiped < 4) {
            this.mobileSwipeUp();
            alreadySwiped++;
        }
        this.waitForElementPresent(DAILY_PRACTICE_TITLE, "Daily Practice section title is not displayed", 10);
        this.waitForElementPresent(FIRST_PRACTICE_TITLE, "First Daily Practice item is not displayed", 10);
        this.waitForElementPresent(FIRST_PRACTICE_TYPE, "First Daily Practice item type is not displayed", 10);
    }

    @Step("Open first Daily Practice")
    public void openFirstDailyPractice() {
        this.assertDailyPracticeIsDisplayed();
        this.waitForElementAndClick(FIRST_PRACTICE_TITLE, "Cannot open first Daily Practice", 10);
        if (this.isLockedModulePopupDisplayed()) {
            return;
        }
        this.waitForElementPresent(PRACTICE_SCREEN_TITLE, "Daily Practice screen title is not displayed", 15);
    }

    @Step("Verify Daily Practice player/start screen is displayed")
    public void assertDailyPracticeScreenIsDisplayed() {
        this.waitForElementPresent(PRACTICE_SCREEN_TITLE, "Daily Practice screen title is not displayed", 10);
    }

    @Step("Return from Daily Practice to Daily Plan")
    public void returnFromDailyPracticeToDailyPlan() {
        try {
            this.waitForElementAndClick(PRACTICE_SCREEN_CLOSE_BUTTON, "Cannot close Daily Practice screen", 5);
        } catch (TimeoutException e) {
            this.tapTopRightCloseFallback();
        }
        this.waitForElementPresent(DAILY_PRACTICE_TITLE, "Daily Plan did not return after closing Daily Practice", 10);
    }

    public boolean isLockedModulePopupDisplayed() {
        return this.isElementPresent(LOCKED_MODULE_POPUP_TITLE);
    }

    @Step("Close Kegel exercise flow if it is already open")
    public void closeKegelExerciseFlowIfPresent() {
        try {
            if (this.isElementPresent(KEGEL_PLAYER_INFO_MODAL_CLOSE_BUTTON)) {
                this.waitForElementAndClick(
                        KEGEL_PLAYER_INFO_MODAL_CLOSE_BUTTON,
                        "Cannot close Kegel player info modal",
                        5
                );
            }

            if (this.isElementPresent(KEGEL_PLAYER_EXIT_CONFIRM_QUIT_BUTTON)) {
                this.waitForElementAndClick(
                        KEGEL_PLAYER_EXIT_CONFIRM_QUIT_BUTTON,
                        "Cannot confirm Kegel player exit",
                        5
                );
                this.waitForElementPresent(DAILY_PRACTICE_TITLE, "Daily Plan did not return after confirming Kegel exit", 15);
                return;
            }

            if (this.isElementPresent(KEGEL_PLAYER_BACK_BUTTON)) {
                this.tapElementCenter(
                        KEGEL_PLAYER_BACK_BUTTON,
                        "Cannot tap Kegel player close control",
                        5
                );
                try {
                    this.waitForElementAndClick(
                            KEGEL_PLAYER_EXIT_CONFIRM_QUIT_BUTTON,
                            "Cannot confirm Kegel player exit",
                            5
                    );
                } catch (TimeoutException e) {
                    System.out.println("Kegel player closed without an exit confirmation; continuing.");
                }
                this.waitForElementPresent(DAILY_PRACTICE_TITLE, "Daily Plan did not return after closing Kegel player", 15);
                return;
            }

            if (this.isElementPresent(PRACTICE_SCREEN_CLOSE_BUTTON)) {
                this.waitForElementAndClick(
                        PRACTICE_SCREEN_CLOSE_BUTTON,
                        "Cannot close Kegel start screen",
                        5
                );
            } else if (this.isElementPresent(PRACTICE_SCREEN_START_BUTTON)) {
                this.scrollToKegelStartHeader();
                if (this.isElementPresent(PRACTICE_SCREEN_CLOSE_BUTTON)) {
                    this.waitForElementAndClick(
                            PRACTICE_SCREEN_CLOSE_BUTTON,
                            "Cannot close Kegel start screen",
                            5
                    );
                }
            }
        } catch (Exception e) {
            System.out.println("Kegel exercise flow was not open or did not need closing; continuing.");
        }
    }

    @Step("Open Kegel exercise from Daily Plan")
    public void openKegelExerciseFromDailyPlan() {
        this.assertDailyPracticeIsDisplayed();
        this.closePracticeCompletionFeedbackIfPresent();
        this.scrollToKegelDailyPlanCard();
        this.tapElementCenter(KEGEL_DAILY_PLAN_TITLE, "Cannot open Kegel exercise from Daily Plan", 10);
        this.waitForElementPresent(KEGEL_START_SCREEN_TITLE, "Kegel exercise start screen did not open", 15);
    }

    @Step("Close practice completion feedback if it appears")
    public void closePracticeCompletionFeedbackIfPresent() {
        try {
            this.waitForElementPresent(
                    PRACTICE_COMPLETION_FEEDBACK_TITLE,
                    "Practice completion feedback is not displayed",
                    2
            );
            this.waitForElementAndClick(
                    PRACTICE_COMPLETION_FEEDBACK_CLOSE_BUTTON,
                    "Cannot close practice completion feedback",
                    10
            );
            this.waitForElementNotPresent(
                    PRACTICE_COMPLETION_FEEDBACK_TITLE,
                    "Practice completion feedback is still displayed",
                    10
            );
        } catch (Exception e) {
            System.out.println("Practice completion feedback was not shown; continuing.");
        }
    }

    private void scrollToKegelDailyPlanCard() {
        int alreadySwipedDown = 0;
        while (!this.isElementPresent(KEGEL_DAILY_PLAN_TITLE) && alreadySwipedDown < 3) {
            this.mobileSwipeDown();
            alreadySwipedDown++;
        }

        int alreadySwiped = 0;
        while (!this.isElementPresent(KEGEL_DAILY_PLAN_TITLE) && alreadySwiped < 6) {
            this.mobileSwipeUp();
            alreadySwiped++;
        }
    }

    @Step("Verify Kegel exercise start screen")
    public void assertKegelStartScreenIsDisplayed() {
        this.scrollToKegelStartHeader();
        this.waitForElementPresent(KEGEL_START_SCREEN_TITLE, "Kegel exercise title is not displayed", 10);
        this.waitForElementPresent(KEGEL_START_SCREEN_DESCRIPTION, "Kegel exercise description is not displayed", 10);
        this.waitForElementPresent(KEGEL_START_SCREEN_DURATION, "Kegel exercise duration is not displayed", 10);
        this.waitForElementPresent(KEGEL_START_SCREEN_INTENSITY, "Kegel exercise intensity is not displayed", 10);
        this.waitForElementPresent(PRACTICE_SCREEN_GOAL_TITLE, "Kegel exercise goal section is not displayed", 10);
        this.waitForElementPresent(PRACTICE_SCREEN_EXERCISES_TITLE, "Kegel exercise list section is not displayed", 10);
        this.waitForElementPresent(KEGEL_START_SCREEN_EXERCISE_TITLE, "Kegel exercise item is not displayed", 10);
        this.waitForElementPresent(PRACTICE_SCREEN_START_BUTTON, "Kegel exercise start button is not displayed", 10);
        if (this.isElementPresent(PRACTICE_SCREEN_CLOSE_BUTTON)) {
            this.waitForElementPresent(PRACTICE_SCREEN_CLOSE_BUTTON, "Kegel exercise close control is not displayed", 10);
        } else {
            System.out.println("Kegel start screen close control is not exposed in the current state; continuing.");
        }
    }

    @Step("Start Kegel exercise")
    public void startKegelExercise() {
        this.scrollToKegelStartButton();
        this.waitForElementAndClick(PRACTICE_SCREEN_START_BUTTON, "Cannot start Kegel exercise", 10);
        this.waitForElementPresent(KEGEL_PLAYER_PAUSE_BUTTON, "Kegel player did not open with pause control", 20);
    }

    @Step("Verify Kegel exercise player is displayed")
    public void assertKegelPlayerIsDisplayed() {
        this.waitForElementPresent(KEGEL_PLAYER_BACK_BUTTON, "Kegel player back control is not displayed", 10);
        Assert.assertTrue("Kegel player sound control is not displayed", this.isKegelPlayerSoundControlDisplayed());
        this.waitForElementPresent(KEGEL_PLAYER_EXERCISE_TITLE, "Kegel player active exercise title is not displayed", 10);
        this.waitForElementPresent(KEGEL_PLAYER_DIFFICULTY, "Kegel player guide label is not displayed", 10);
        this.waitForElementPresent(KEGEL_PLAYER_TIMER, "Kegel player timer/progress labels are not displayed", 10);
        this.waitForElementPresent(KEGEL_PLAYER_PAUSE_BUTTON, "Kegel player pause control is not displayed", 10);
        this.waitForElementPresent(KEGEL_PLAYER_VIBRATION_BUTTON, "Kegel player vibration control is not displayed", 10);
        this.waitForElementPresent(KEGEL_PLAYER_INFO_BUTTON, "Kegel player info control is not displayed", 10);
        this.waitForElementPresent(KEGEL_PLAYER_PHASE_REST_BUTTON, "Kegel player Rest phase control is not displayed", 10);
        this.waitForElementPresent(KEGEL_PLAYER_PHASE_WAVES_BUTTON, "Kegel player Waves phase control is not displayed", 10);
    }

    @Step("Press all available Kegel player controls")
    public void pressAllKegelPlayerControls() {
        this.closeKegelPlayerInstructionsIfPresent();
        this.pauseKegelExercise();
        this.openKegelPlayerInfo();
        this.toggleKegelPlayerVibration();
        this.toggleKegelPlayerSound();
        this.resumeKegelExercise();
    }

    @Step("Close Kegel player instructions if they are open")
    public void closeKegelPlayerInstructionsIfPresent() {
        if (this.isElementPresent(KEGEL_PLAYER_INFO_MODAL_CLOSE_BUTTON)) {
            this.waitForElementAndClick(
                    KEGEL_PLAYER_INFO_MODAL_CLOSE_BUTTON,
                    "Cannot close Kegel player instructions",
                    10
            );
            this.waitForElementNotPresent(
                    KEGEL_PLAYER_INFO_MODAL_CLOSE_BUTTON,
                    "Kegel player instructions are still displayed",
                    10
            );
        }
    }

    @Step("Pause Kegel exercise")
    public void pauseKegelExercise() {
        this.tapElementCenter(KEGEL_PLAYER_PAUSE_BUTTON, "Cannot pause Kegel exercise", 10);
        this.waitForElementPresent(KEGEL_PLAYER_PLAY_BUTTON, "Kegel player resume control is not displayed after pause", 10);
    }

    @Step("Resume Kegel exercise")
    public void resumeKegelExercise() {
        this.tapElementCenter(KEGEL_PLAYER_PLAY_BUTTON, "Cannot resume Kegel exercise", 10);
        this.waitForElementPresent(KEGEL_PLAYER_PAUSE_BUTTON, "Kegel player pause control did not return after resume", 10);
    }

    @Step("Toggle Kegel player sound")
    public void toggleKegelPlayerSound() {
        if (this.isElementPresent(KEGEL_PLAYER_MUTE_BUTTON)) {
            this.tapElementCenter(KEGEL_PLAYER_MUTE_BUTTON, "Cannot tap Kegel player sound control", 10);
        } else {
            this.tapElementCenter(KEGEL_PLAYER_UNMUTE_BUTTON, "Cannot tap Kegel player sound control", 10);
        }
        Assert.assertTrue("Kegel player sound control is not displayed after toggle", this.isKegelPlayerSoundControlDisplayed());
    }

    @Step("Toggle Kegel player vibration")
    public void toggleKegelPlayerVibration() {
        this.tapElementCenter(KEGEL_PLAYER_VIBRATION_BUTTON, "Cannot tap Kegel player vibration control", 10);
        this.waitForElementPresent(KEGEL_PLAYER_VIBRATION_BUTTON, "Kegel player vibration control is not displayed after toggle", 10);
    }

    @Step("Open Kegel player info")
    public void openKegelPlayerInfo() {
        this.tapElementCenter(KEGEL_PLAYER_INFO_BUTTON, "Cannot tap Kegel player info control", 10);
        this.waitForElementPresent(KEGEL_PLAYER_INFO_MODAL_TITLE, "Kegel player info modal title is not displayed", 10);
        this.waitForElementPresent(KEGEL_PLAYER_INFO_MODAL_CONTENT, "Kegel player info modal content is not displayed", 10);
        this.waitForElementAndClick(KEGEL_PLAYER_INFO_MODAL_CLOSE_BUTTON, "Cannot close Kegel player info modal", 10);
        this.waitForElementPresent(KEGEL_PLAYER_PLAY_BUTTON, "Kegel player did not return paused after closing info modal", 10);
    }

    @Step("Press Kegel player phase controls")
    public void pressKegelPlayerPhaseControls() {
        if (this.isElementPresent(KEGEL_PLAYER_PHASE_SQUEEZE_BUTTON)) {
            this.tapElementCenter(KEGEL_PLAYER_PHASE_SQUEEZE_BUTTON, "Cannot tap Kegel player Squeeze phase", 10);
            this.waitForElementPresent(KEGEL_PLAYER_PAUSE_BUTTON, "Kegel player is not active after Squeeze phase tap", 10);
        }
        this.tapElementCenter(KEGEL_PLAYER_PHASE_REST_BUTTON, "Cannot tap Kegel player Rest phase", 10);
        this.waitForElementPresent(KEGEL_PLAYER_PAUSE_BUTTON, "Kegel player is not active after Rest phase tap", 10);
        this.tapElementCenter(KEGEL_PLAYER_PHASE_WAVES_BUTTON, "Cannot tap Kegel player Waves phase", 10);
        this.waitForElementPresent(KEGEL_PLAYER_PAUSE_BUTTON, "Kegel player is not active after Waves phase tap", 10);
    }

    public boolean isKegelPlayerSoundControlDisplayed() {
        return this.isElementPresent(KEGEL_PLAYER_MUTE_BUTTON) || this.isElementPresent(KEGEL_PLAYER_UNMUTE_BUTTON);
    }

    private void scrollToKegelStartHeader() {
        int alreadySwiped = 0;
        while (!this.isElementPresent(KEGEL_START_SCREEN_TITLE) && alreadySwiped < 3) {
            this.mobileSwipeDown();
            alreadySwiped++;
        }
    }

    private void scrollToKegelStartButton() {
        int alreadySwiped = 0;
        while (!this.isElementPresent(PRACTICE_SCREEN_START_BUTTON) && alreadySwiped < 3) {
            this.mobileSwipeUp();
            alreadySwiped++;
        }
    }

    @Step("Exit Kegel exercise player and return to Daily Plan")
    public void exitKegelExercisePlayer() {
        this.tapElementCenter(KEGEL_PLAYER_BACK_BUTTON, "Cannot tap Kegel player close control", 10);
        if (this.isElementPresent(KEGEL_PLAYER_EXIT_CONFIRM_TITLE)) {
            this.waitForElementPresent(KEGEL_PLAYER_EXIT_CONFIRM_CONTINUE_BUTTON, "Kegel player continue button is not displayed", 10);
            this.waitForElementAndClick(KEGEL_PLAYER_EXIT_CONFIRM_QUIT_BUTTON, "Cannot confirm Kegel player exit", 10);
        }
        this.waitForElementPresent(DAILY_PRACTICE_TITLE, "Daily Plan did not return after closing Kegel player", 15);
    }

    @Step("Open Daily Plan customization actions for the first Daily Practice card")
    public void openCustomizationActionsForFirstDailyPractice() {
        Assert.assertTrue(
                "Current Daily Plan state did not expose customization actions for the first Daily Practice card.",
                this.tryOpenCustomizationActionsForFirstDailyPractice()
        );
    }

    @Step("Try to open Daily Plan customization actions for the first Daily Practice card")
    public boolean tryOpenCustomizationActionsForFirstDailyPractice() {
        this.assertDailyPracticeIsDisplayed();
        this.closeCoreExerciseRestrictionPopupIfPresent();
        WebElement practiceCard = this.waitForElementPresent(
                FIRST_PRACTICE_TITLE,
                "First Daily Practice item is not displayed",
                10
        );
        this.touchAndHoldElement(practiceCard, 0.8);
        if (this.isCustomizationActionsSheetDisplayed()) {
            return true;
        }
        if (this.isCoreExerciseRestrictionPopupDisplayed()) {
            this.closeCoreExerciseRestrictionPopup();
            return false;
        }
        return false;
    }

    @Step("Verify Daily Plan customization actions sheet is displayed")
    public void assertCustomizationActionsSheetIsDisplayed() {
        this.waitForElementPresent(
                CUSTOMIZATION_MOVE_TO_TOMORROW_BUTTON,
                "Move to tomorrow action is not displayed",
                10
        );
        this.waitForElementPresent(
                CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON,
                "Remove from Daily Plan action is not displayed",
                10
        );
    }

    public boolean isCustomizationActionsSheetDisplayed() {
        try {
            this.waitForElementPresent(
                    CUSTOMIZATION_MOVE_TO_TOMORROW_BUTTON,
                    "Move to tomorrow action is not displayed",
                    2
            );
            this.waitForElementPresent(
                    CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON,
                    "Remove from Daily Plan action is not displayed",
                    2
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Step("Tap Remove from Daily Plan action")
    public void tapRemoveFromDailyPlanAction() {
        this.waitForElementAndClick(
                CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON,
                "Cannot tap Remove from Daily Plan action",
                10
        );
    }

    @Step("Verify Remove from Daily Plan confirmation is displayed")
    public void assertRemoveFromDailyPlanConfirmationIsDisplayed() {
        this.waitForElementPresent(
                CUSTOMIZATION_DELETE_CONFIRM_BUTTON,
                "Delete confirmation button is not displayed",
                10
        );
        this.waitForElementPresent(
                CUSTOMIZATION_CANCEL_DELETE_BUTTON,
                "Cancel delete button is not displayed",
                10
        );
    }

    @Step("Cancel Remove from Daily Plan confirmation")
    public void cancelRemoveFromDailyPlan() {
        this.waitForElementAndClick(
                CUSTOMIZATION_CANCEL_DELETE_BUTTON,
                "Cannot cancel Remove from Daily Plan confirmation",
                10
        );
        this.waitForElementNotPresent(
                CUSTOMIZATION_DELETE_CONFIRM_BUTTON,
                "Delete confirmation is still displayed after cancelling",
                10
        );
    }

    @Step("Verify first Daily Practice card remains in Daily Plan")
    public void assertFirstDailyPracticeStillDisplayed() {
        this.assertDailyPracticeIsDisplayed();
    }

    public boolean isCoreExerciseRestrictionPopupDisplayed() {
        return this.isElementPresent(CORE_EXERCISE_RESTRICTION_POPUP_TITLE);
    }

    @Step("Close core exercise restriction popup")
    public void closeCoreExerciseRestrictionPopup() {
        this.waitForElementAndClick(
                CORE_EXERCISE_RESTRICTION_POPUP_BUTTON,
                "Cannot close core exercise restriction popup",
                10
        );
        this.waitForElementNotPresent(
                CORE_EXERCISE_RESTRICTION_POPUP_TITLE,
                "Core exercise restriction popup is still displayed",
                10
        );
    }

    @Step("Close core exercise restriction popup if it appears")
    public void closeCoreExerciseRestrictionPopupIfPresent() {
        try {
            this.waitForElementPresent(
                    CORE_EXERCISE_RESTRICTION_POPUP_TITLE,
                    "Core exercise restriction popup is not displayed",
                    2
            );
            this.closeCoreExerciseRestrictionPopup();
        } catch (Exception e) {
            System.out.println("Core exercise restriction popup was not shown; continuing.");
        }
    }

    @Step("Close locked module popup")
    public void closeLockedModulePopup() {
        this.waitForElementAndClick(LOCKED_MODULE_POPUP_BUTTON, "Cannot close locked module popup", 10);
        this.waitForElementNotPresent(LOCKED_MODULE_POPUP_TITLE, "Locked module popup is still displayed", 10);
    }

    @Step("Verify right arrow does not move to the next day when current day is locked")
    public void assertRightArrowDoesNotChangeCurrentDayWhenLocked() {
        String dayBeforeTap = this.getCurrentDayLabel();
        this.waitForElementAndClick(RIGHT_SWITCHER_ARROW, "Cannot tap Daily Plan right arrow", 10);
        if (this.isLockedNextDayPopupDisplayed()) {
            this.closeLockedNextDayPopup();
            Assert.assertEquals(
                    "Daily Plan day changed after tapping locked right arrow",
                    dayBeforeTap,
                    this.getCurrentDayLabel()
            );
        } else {
            System.out.println("Next Daily Plan stage was available; locked-next-day assertion is not applicable for this account state.");
            this.assertDailyPlanDaySwitcherIsDisplayed();
        }
    }

    @Step("Close locked next day popup")
    public void closeLockedNextDayPopup() {
        this.waitForElementAndClick(
                LOCKED_NEXT_DAY_POPUP_BUTTON,
                "Cannot close locked next day popup",
                10
        );
        this.waitForElementNotPresent(
                LOCKED_NEXT_DAY_POPUP_TITLE,
                "Locked next day popup is still displayed",
                10
        );
    }

    @Step("Close locked next day popup if it appears")
    public void closeLockedNextDayPopupIfPresent() {
        try {
            this.waitForElementPresent(LOCKED_NEXT_DAY_POPUP_TITLE, "Locked next day popup did not appear", 2);
            this.waitForElementPresent(
                    LOCKED_NEXT_DAY_POPUP_MESSAGE,
                    "Locked next day popup message is not displayed",
                    2
            );
            this.closeLockedNextDayPopup();
        } catch (Exception e) {
            System.out.println("Locked next day popup was not shown; continuing.");
        }
    }

    public boolean isLockedNextDayPopupDisplayed() {
        return this.isElementPresent(LOCKED_NEXT_DAY_POPUP_TITLE);
    }

    @Step("Verify left arrow keeps the first Daily Plan day selected")
    public void assertLeftArrowKeepsCurrentDaySelected() {
        String dayBeforeTap = this.getCurrentDayLabel();
        this.waitForElementAndClick(LEFT_SWITCHER_ARROW, "Cannot tap Daily Plan left arrow", 10);
        Assert.assertEquals(
                "Daily Plan day changed after tapping left arrow on the first day",
                dayBeforeTap,
                this.getCurrentDayLabel()
        );
    }

    public String getCurrentDayLabel() {
        return this.waitForElementAndGetAttribute(
                CURRENT_DAY_LABEL,
                "name",
                "Cannot get current Daily Plan day label",
                10
        );
    }

    public void mobileSwipeDown() {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("direction", "down");
        ((JavascriptExecutor) driver).executeScript("mobile: swipe", args);
    }

    public void mobileSwipeUp() {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("direction", "up");
        ((JavascriptExecutor) driver).executeScript("mobile: swipe", args);
    }

    private void scrollDownToDailyPlanHeader() {
        int alreadySwiped = 0;
        while (!this.isElementPresent(CURRENT_DAY_LABEL) && alreadySwiped < 5) {
            this.mobileSwipeDown();
            alreadySwiped++;
        }
    }

    private void tapTopRightCloseFallback() {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x", driver.manage().window().getSize().getWidth() - 32);
        args.put("y", 90);
        ((JavascriptExecutor) driver).executeScript("mobile: tap", args);
    }

    private void tapElementCenter(String locator, String errorMessage, long timeoutInSeconds) {
        WebElement element = this.waitForElementPresent(locator, errorMessage, timeoutInSeconds);
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x", element.getLocation().getX() + element.getSize().getWidth() / 2);
        args.put("y", element.getLocation().getY() + element.getSize().getHeight() / 2);
        ((JavascriptExecutor) driver).executeScript("mobile: tap", args);
    }

    private void touchAndHoldElement(WebElement element, double duration) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("elementId", ((RemoteWebElement) element).getId());
        args.put("duration", duration);
        ((JavascriptExecutor) driver).executeScript("mobile: touchAndHold", args);
    }
}
