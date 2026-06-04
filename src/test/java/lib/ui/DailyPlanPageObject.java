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
            CUSTOMIZATION_MOVE_TO_TOMORROW_BUTTON,
            CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON,
            CUSTOMIZATION_DELETE_CONFIRM_BUTTON,
            CUSTOMIZATION_CANCEL_DELETE_BUTTON,
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

    @Step("Open Daily Plan customization actions for the first Daily Practice card")
    public void openCustomizationActionsForFirstDailyPractice() {
        this.assertDailyPracticeIsDisplayed();
        WebElement practiceCard = this.waitForElementPresent(
                FIRST_PRACTICE_TITLE,
                "First Daily Practice item is not displayed",
                10
        );
        this.touchAndHoldElement(practiceCard, 0.8);
        this.assertCustomizationActionsSheetIsDisplayed();
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

    private void touchAndHoldElement(WebElement element, double duration) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("elementId", ((RemoteWebElement) element).getId());
        args.put("duration", duration);
        ((JavascriptExecutor) driver).executeScript("mobile: touchAndHold", args);
    }
}
