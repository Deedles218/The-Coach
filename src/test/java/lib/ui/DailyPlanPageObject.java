package lib.ui;

import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.JavascriptExecutor;
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
            DAILY_PRACTICE_TITLE,
            FIRST_PRACTICE_TITLE,
            FIRST_PRACTICE_TYPE,
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

    @Step("Verify right arrow does not move to the next day when current day is locked")
    public void assertRightArrowDoesNotChangeCurrentDayWhenLocked() {
        String dayBeforeTap = this.getCurrentDayLabel();
        this.waitForElementAndClick(RIGHT_SWITCHER_ARROW, "Cannot tap Daily Plan right arrow", 10);
        this.closeLockedNextDayPopupIfPresent();
        Assert.assertEquals(
                "Daily Plan day changed after tapping locked right arrow",
                dayBeforeTap,
                this.getCurrentDayLabel()
        );
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
}
