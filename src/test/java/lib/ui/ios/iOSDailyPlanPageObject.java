package lib.ui.ios;

import lib.ui.DailyPlanPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSDailyPlanPageObject extends DailyPlanPageObject {
    static {
        TAB_TODAY = "id:Today";
        SELECTED_TODAY_TAB = "xpath://XCUIElementTypeButton[@name='Today' and @value='1']";
        CURRENT_DAY_LABEL = "xpath://XCUIElementTypeStaticText[(starts-with(@name, 'Day ') or starts-with(@name, 'Stage ')) and contains(@name, ' of ')]";
        DAILY_PLAN_DAY_SWITCHER = CURRENT_DAY_LABEL;
        LEFT_SWITCHER_ARROW = "id:leftSwitcherArrow";
        RIGHT_SWITCHER_ARROW = "id:rightSwitcherArrow";

        DAILY_LESSONS_TITLE = "xpath://XCUIElementTypeStaticText[@label='DAILY LESSONS']";
        FIRST_LESSON_TITLE = "xpath://XCUIElementTypeStaticText[@name='Getting started']";
        FIRST_LESSON_TYPE = "xpath://XCUIElementTypeStaticText[@name='Lesson 1']";
        LESSON_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[@name='Getting started']";
        LESSON_SCREEN_CONTENT = "xpath://XCUIElementTypeStaticText[string-length(@name) > 20]";
        LESSON_SCREEN_BACK_BUTTON = "xpath://XCUIElementTypeButton[@name='ProgramCloseButtonIcon' or @name='BackButton' or @name='Close' or @name='Back']";

        DAILY_PRACTICE_TITLE = "xpath://XCUIElementTypeStaticText[@label='DAILY PRACTICE']";
        FIRST_PRACTICE_TITLE = "xpath://XCUIElementTypeStaticText[@name='Finding Pelvic Floor' or @name='Morning Kegel Workout']";
        FIRST_PRACTICE_TYPE = "xpath://XCUIElementTypeStaticText[@name='Guide' or contains(@name, 'days in total')]";
        PRACTICE_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[(contains(@name, 'Kegel') or @name='Finding Pelvic Floor') and @visible='true']";
        PRACTICE_SCREEN_GOAL_TITLE = "xpath://XCUIElementTypeStaticText[@label='GOAL' or @name='GOAL' or @name='Duration:' or @name='Intensity:']";
        PRACTICE_SCREEN_EXERCISES_TITLE = "xpath://XCUIElementTypeStaticText[@label='EXERCISES' or @name='EXERCISES' or contains(@name, 'ADVANCED')]";
        PRACTICE_SCREEN_START_BUTTON = "id:START WORKOUT";
        PRACTICE_SCREEN_CLOSE_BUTTON = "id:ProgramCloseButtonIcon";
        LOCKED_MODULE_POPUP_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Complete current module to unlock the next one')]";
        LOCKED_MODULE_POPUP_BUTTON = "id:GOT IT";

        LOCKED_NEXT_DAY_POPUP_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Finish today') and contains(@name, 'unlock the next day')]";
        LOCKED_NEXT_DAY_POPUP_MESSAGE = "xpath://XCUIElementTypeStaticText[contains(@name, 'move it to tomorrow')]";
        LOCKED_NEXT_DAY_POPUP_BUTTON = "id:GOT IT";
    }

    public iOSDailyPlanPageObject(RemoteWebDriver driver) {
        super(driver);
    }
}
