package lib.ui.ios;

import lib.ui.DailyPlanPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSDailyPlanPageObject extends DailyPlanPageObject {
    static {
        TAB_TODAY = "id:Today";
        SELECTED_TODAY_TAB = "xpath://XCUIElementTypeButton[@name='Today' and @value='1']";
        CURRENT_DAY_LABEL = "xpath://XCUIElementTypeStaticText[starts-with(@name, 'Day ') and contains(@name, ' of ')]";
        DAILY_PLAN_DAY_SWITCHER = CURRENT_DAY_LABEL;
        LEFT_SWITCHER_ARROW = "id:leftSwitcherArrow";
        RIGHT_SWITCHER_ARROW = "id:rightSwitcherArrow";

        DAILY_LESSONS_TITLE = "xpath://XCUIElementTypeStaticText[@label='DAILY LESSONS']";
        FIRST_LESSON_TITLE = "xpath://XCUIElementTypeStaticText[@name='Getting started']";
        FIRST_LESSON_TYPE = "xpath://XCUIElementTypeStaticText[@name='Lesson 1']";

        DAILY_PRACTICE_TITLE = "xpath://XCUIElementTypeStaticText[@label='DAILY PRACTICE']";
        FIRST_PRACTICE_TITLE = "xpath://XCUIElementTypeStaticText[@name='Finding Pelvic Floor']";
        FIRST_PRACTICE_TYPE = "xpath://XCUIElementTypeStaticText[@name='Guide']";

        LOCKED_NEXT_DAY_POPUP_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Finish today') and contains(@name, 'unlock the next day')]";
        LOCKED_NEXT_DAY_POPUP_MESSAGE = "xpath://XCUIElementTypeStaticText[contains(@name, 'move it to tomorrow')]";
        LOCKED_NEXT_DAY_POPUP_BUTTON = "id:GOT IT";
    }

    public iOSDailyPlanPageObject(RemoteWebDriver driver) {
        super(driver);
    }
}
