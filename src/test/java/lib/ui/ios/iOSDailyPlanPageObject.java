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

public class iOSDailyPlanPageObject extends DailyPlanPageObject {
    private int selectorTop;
    private static final String SELECTOR_CARDS = "id:ProgramSelectionCardView";
    private static final String SELECTOR_TITLES = "xpath://XCUIElementTypeOther[@name='ProgramSelectionCardView']//XCUIElementTypeStaticText[@name='TitleBlock.Title']";
    private static final String SELECTOR_COLLECTION = "xpath:(//XCUIElementTypeScrollView[.//XCUIElementTypeOther[@name='ProgramSelectionCardView']] | //XCUIElementTypeCollectionView[.//XCUIElementTypeOther[@name='ProgramSelectionCardView']])[last()]";
    static {
        TAB_TODAY = "id:Today";
        SELECTED_TODAY_TAB = "xpath://XCUIElementTypeButton[@name='Today' and @value='1']";
        ACTIVE_PROGRAM_TITLE = "xpath://XCUIElementTypeStaticText[@visible='true' and ("
                + "@name='LL' or @label='LL' or @name='KIH' or @label='KIH' "
                + "or @name='OH' or @label='OH' or @name='SIAS' or @label='SIAS' "
                + "or contains(translate(@name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'LAST LONGER') "
                + "or contains(translate(@name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'KEEP IT HARD') "
                + "or contains(translate(@name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'OVERALL HEALTH') "
                + "or contains(translate(@name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'SEX IS A SKILL') "
                + "or contains(translate(@name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'UNHOOKED') "
                + "or @name='Kegel Challenge' or @label='Kegel Challenge' "
                + "or contains(translate(@label, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'UNHOOKED'))]";
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
        FIRST_LESSON_TITLE = "xpath://XCUIElementTypeStaticText[@name='Starting Point' or @name='Getting started']";
        FIRST_LESSON_TYPE = "xpath://XCUIElementTypeStaticText[@name='Lesson 1']";
        LESSON_SCREEN_TITLE = "xpath://XCUIElementTypeStaticText[@name='Starting Point' or @name='Getting started']";
        LESSON_SCREEN_CONTENT = "xpath://XCUIElementTypeStaticText[string-length(@name) > 20]";
        LESSON_SCREEN_BACK_BUTTON = "xpath://XCUIElementTypeButton[@name='ProgramCloseButtonIcon' or @name='BackButton' or @name='Close' or @name='Back']";

        DAILY_PRACTICE_TITLE = "xpath://XCUIElementTypeStaticText[@label='DAILY PRACTICE']";
        FIRST_PRACTICE_TITLE = "xpath://XCUIElementTypeStaticText[@name='Unlock Your Pelvic Floor' or @name='Pelvic Floor Assessment' or @name='Your First Kegel Workout' or @name='Finding Pelvic Floor' or @name='Morning Kegel Workout']";
        FIRST_PRACTICE_TYPE = "xpath://XCUIElementTypeStaticText[@name='Guide' or contains(@name, 'days in total')]";
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
        KEGEL_PLAYER_EXERCISE_TITLE = "xpath://XCUIElementTypeStaticText[@name='Contract' or @name='Rest' or @name='Waves' or @name='Squeeze']";
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
        KEGEL_PLAYER_INFO_MODAL_CONTENT = "xpath://XCUIElementTypeStaticText[contains(@name, 'Contract your pelvic floor muscle') and @visible='true']";
        KEGEL_PLAYER_INFO_MODAL_CLOSE_BUTTON = "xpath://XCUIElementTypeButton[@name='ic outline close' and @visible='true']";
        KEGEL_PLAYER_PHASE_SQUEEZE_BUTTON = "xpath://XCUIElementTypeStaticText[@name='SQUEEZE']";
        KEGEL_PLAYER_PHASE_REST_BUTTON = "xpath://XCUIElementTypeStaticText[@name='REST']";
        KEGEL_PLAYER_PHASE_WAVES_BUTTON = "xpath://XCUIElementTypeStaticText[@name='WAVES']";
        KEGEL_PLAYER_EXIT_CONFIRM_TITLE = "xpath://XCUIElementTypeStaticText[(contains(@value, 'You haven') or contains(@label, 'You haven')) and (contains(@value, 'quit') or contains(@label, 'quit')) and @visible='true']";
        KEGEL_PLAYER_EXIT_CONFIRM_QUIT_BUTTON = "id:Quit";
        KEGEL_PLAYER_EXIT_CONFIRM_CONTINUE_BUTTON = "id:Don’t quit";
        PRACTICE_COMPLETION_FEEDBACK_TITLE = "xpath://XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label='YOU’RE GREAT!']";
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
