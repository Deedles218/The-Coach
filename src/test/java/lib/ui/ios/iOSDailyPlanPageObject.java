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
        PRACTICE_SCREEN_START_BUTTON = "xpath://XCUIElementTypeButton[@name='START WORKOUT' and @visible='true']";
        PRACTICE_SCREEN_CLOSE_BUTTON = "id:ProgramCloseButtonIcon";
        KEGEL_DAILY_PLAN_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Kegel Workout') or @name='Resistance Kegel Training']";
        KEGEL_START_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, 'Kegel') and @visible='true']";
        KEGEL_START_SCREEN_DESCRIPTION = "xpath://XCUIElementTypeStaticText[(contains(@value, 'The goal of this level') or contains(@label, 'The goal of this level')) and @visible='true']";
        KEGEL_START_SCREEN_DURATION = "xpath://XCUIElementTypeStaticText[@name='Duration:' and @visible='true']";
        KEGEL_START_SCREEN_INTENSITY = "xpath://XCUIElementTypeStaticText[@name='Intensity:' and @visible='true']";
        KEGEL_START_SCREEN_EXERCISE_TITLE = "xpath://XCUIElementTypeStaticText[(contains(@name, 'Stretching') or @name='Squeeze') and @visible='true']";
        KEGEL_PLAYER_BACK_BUTTON = "id:navBarRoundClose";
        KEGEL_PLAYER_MUTE_BUTTON = "xpath://XCUIElementTypeButton[contains(@name, 'sound off')]";
        KEGEL_PLAYER_UNMUTE_BUTTON = "xpath://XCUIElementTypeButton[contains(@name, 'sound on')]";
        KEGEL_PLAYER_EXERCISE_TITLE = "xpath://XCUIElementTypeStaticText[@name='Contract' or @name='Rest' or @name='Waves' or @name='Squeeze']";
        KEGEL_PLAYER_DIFFICULTY = "xpath://XCUIElementTypeStaticText[@name='GUIDE' or contains(@name, 'How to find pelvic floor')]";
        KEGEL_PLAYER_TIMER = "xpath://XCUIElementTypeStaticText[string-length(@name) <= 2 and translate(@name, '0123456789', '') = '']";
        KEGEL_PLAYER_PAUSE_BUTTON = "id:PlayerPauseIcon";
        KEGEL_PLAYER_PLAY_BUTTON = "id:PlayerPlayIcon";
        KEGEL_PLAYER_REWIND_BUTTON = "id:RewindButtonMain";
        KEGEL_PLAYER_REWIND_BACK_BUTTON = "xpath:(//XCUIElementTypeButton[@name='RewindButtonMain' and @visible='true'])[1]";
        KEGEL_PLAYER_REWIND_FORWARD_BUTTON = "xpath:(//XCUIElementTypeButton[@name='RewindButtonMain' and @visible='true'])[2]";
        KEGEL_PLAYER_VIBRATION_BUTTON = "xpath://XCUIElementTypeButton[contains(@name, 'vibro')]";
        KEGEL_PLAYER_INFO_BUTTON = "id:ic outline info";
        KEGEL_PLAYER_INFO_TOOLTIP = "xpath://XCUIElementTypeStaticText[contains(@name, 'How to do the') and contains(@name, 'exercise correctly')]";
        KEGEL_PLAYER_PHASE_SQUEEZE_BUTTON = "xpath://XCUIElementTypeStaticText[@name='SQUEEZE']";
        KEGEL_PLAYER_PHASE_REST_BUTTON = "xpath://XCUIElementTypeStaticText[@name='REST']";
        KEGEL_PLAYER_PHASE_WAVES_BUTTON = "xpath://XCUIElementTypeStaticText[@name='WAVES']";
        KEGEL_PLAYER_EXIT_CONFIRM_TITLE = "xpath://XCUIElementTypeStaticText[(contains(@value, 'You haven') or contains(@label, 'You haven')) and (contains(@value, 'quit') or contains(@label, 'quit')) and @visible='true']";
        KEGEL_PLAYER_EXIT_CONFIRM_QUIT_BUTTON = "id:Quit";
        KEGEL_PLAYER_EXIT_CONFIRM_CONTINUE_BUTTON = "id:Continue";
        PRACTICE_COMPLETION_FEEDBACK_TITLE = "xpath://XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='YOU’RE GREAT!']";
        PRACTICE_COMPLETION_FEEDBACK_CLOSE_BUTTON = "id:ic outline close";
        CUSTOMIZATION_MOVE_TO_TOMORROW_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Move to tomorrow'] | //XCUIElementTypeButton[@name='Move to tomorrow']";
        CUSTOMIZATION_REMOVE_FROM_DAILY_PLAN_BUTTON = "xpath://XCUIElementTypeStaticText[@name='Remove from Daily Plan'] | //XCUIElementTypeButton[@name='Remove from Daily Plan']";
        CUSTOMIZATION_DELETE_CONFIRM_BUTTON = "id:DELETE ANYWAYS";
        CUSTOMIZATION_CANCEL_DELETE_BUTTON = "id:CANCEL";
        CORE_EXERCISE_RESTRICTION_POPUP_TITLE = "xpath://XCUIElementTypeStaticText[contains(@name, \"can't postpone/remove this practice\") and contains(@name, 'core exercise')]";
        CORE_EXERCISE_RESTRICTION_POPUP_BUTTON = "id:GOT IT";
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
