package lib.ui.ios;

import lib.ui.ExplorePageObject;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

public class iOSExplorePageObject extends ExplorePageObject {
    static {
        TAB_EXPLORE = "id:Explore";
        SELECTED_EXPLORE_TAB = "xpath://XCUIElementTypeButton[@name='Explore' and @value='1']";
        EXPLORE_ENTRY_POINT = "xpath://XCUIElementTypeButton[@name='Explore' or @name='CloseRoundBlack' or @name='ProgramCloseButtonIcon' or @name='navBarRoundBack']";
        TAB_PROGRAMS = "xpath://XCUIElementTypeButton[@name='Programs']";
        TAB_TOOLS = "xpath://XCUIElementTypeButton[@name='Tools']";

        RECOMMENDED_SECTION = "id:RECOMMENDED FOR YOU";
        RECOMMENDED_COLLECTION = "xpath://XCUIElementTypeStaticText[@name='RECOMMENDED FOR YOU']/following-sibling::XCUIElementTypeCollectionView";
        RECOMMENDED_PROGRAM_CARDS = "xpath://XCUIElementTypeStaticText[@name='RECOMMENDED FOR YOU']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='CoachProgramDetailedManCard']";
        SEXUAL_HEALTH_SECTION = "id:SEXUAL HEALTH";
        COURSES_SECTION = "id:COURSES";
        COURSES_COLLECTION = "xpath://XCUIElementTypeStaticText[@name='COURSES']/following-sibling::XCUIElementTypeCollectionView";
        COURSE_CARDS = "xpath://XCUIElementTypeStaticText[@name='COURSES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='CardCourseView']";
        BODY_PRACTICES_SECTION = "id:BODY PRACTICES";
        BODY_PRACTICES_COLLECTION = "xpath://XCUIElementTypeStaticText[@name='BODY PRACTICES']/following-sibling::XCUIElementTypeCollectionView";
        BODY_PRACTICE_CARDS = "xpath://XCUIElementTypeStaticText[@name='BODY PRACTICES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='ExploreExerciseManCellView']";
        FIRST_BODY_PRACTICE_CARD = "xpath:(//XCUIElementTypeStaticText[@name='BODY PRACTICES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='ExploreExerciseManCellView'])[1]";
        MIND_PRACTICES_SECTION = "id:MIND PRACTICES";
        MIND_PRACTICES_COLLECTION = "xpath://XCUIElementTypeStaticText[@name='MIND PRACTICES']/following-sibling::XCUIElementTypeCollectionView";
        MIND_PRACTICE_CARDS = "xpath://XCUIElementTypeStaticText[@name='MIND PRACTICES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='ExploreExerciseManCellView']";
        FIRST_MIND_PRACTICE_CARD = "xpath:(//XCUIElementTypeStaticText[@name='MIND PRACTICES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='ExploreExerciseManCellView'])[1]";
        RETIRED_MAIN_PROGRAMS_SECTION = "id:MAIN PROGRAMS";
        RETIRED_MORE_TOOLS_SECTION = "id:MORE TOOLS";
        COMPLETED_PROGRAMS_SECTION = "id:COMPLETED PROGRAMS";
        COMING_SOON_SECTION = "id:COMING SOON";

        FIRST_PROGRAM_CARD = "xpath:(//XCUIElementTypeOther[@name='CoachProgramDetailedManCard'])[1]";
        FIRST_PROGRAM_CARD_IMAGE = "xpath:(//XCUIElementTypeOther[@name='CoachProgramDetailedManCard'])[1]//XCUIElementTypeImage";
        FIRST_PROGRAM_TITLE = "id:Keep It Hard";
        // Program-card names are dynamic. Prefer the dedicated identifier and
        // retain the existing card hierarchy only as a migration fallback.
        EXPLORE_PROGRAM_ITEMS = "xpath://XCUIElementTypeOther[@name='CoachProgramDetailedManCard']//XCUIElementTypeStaticText[1]";
        LAST_RECOMMENDED_PROGRAM_CARD = "xpath:(//XCUIElementTypeStaticText[@name='RECOMMENDED FOR YOU']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='CoachProgramDetailedManCard'])[last()]";
        LAST_RECOMMENDED_PROGRAM_TITLE = "id:Kegel Challenge";
        PROGRAM_DETAIL_TITLE = "id:NewCoachProgramHeaderView";
        PROGRAM_DETAIL_CONTENT = "id:DailyDaySwitcherView";
        PROGRAM_DETAIL_CLOSE_BUTTON = "id:CloseRoundBlack";

        FIRST_COURSE_CARD = "xpath:(//XCUIElementTypeOther[@name='CardCourseView'])[1]";
        FIRST_COURSE_CARD_IMAGE = "xpath:(//XCUIElementTypeOther[@name='CardCourseView'])[1]//XCUIElementTypeImage[1]";
        FIRST_COURSE_TITLE = "id:Perform Better In Bed";
        LAST_COURSE_CARD = "xpath:(//XCUIElementTypeStaticText[@name='COURSES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='CardCourseView'])[last()]";
        LAST_COURSE_TITLE = "id:Intermittent Fasting";
        COURSE_DETAIL_TITLE = "id:The_Coach.VideoCourseDetailView";
        COURSE_DETAIL_TYPE = "id:COURSE";
        COURSE_DETAIL_CONTENT = "id:BEFORE YOUR START";
        COURSE_DETAIL_BACK_BUTTON = "id:navBarRoundBack";

        CUSTOM_KEGEL_CARD = "id:Custom Kegel";
        CUSTOM_KEGEL_TITLE = "id:Custom Kegel Workout";
        CUSTOM_KEGEL_START_BUTTON = "id:START WORKOUT";
        CUSTOM_KEGEL_CLOSE_BUTTON = "id:ProgramCloseButtonIcon";
        LAST_BODY_PRACTICE_CARD = "xpath:(//XCUIElementTypeStaticText[@name='BODY PRACTICES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='ExploreExerciseManCellView'])[last()]";
        LAST_BODY_PRACTICE_TITLE = "id:Jacobson Relaxation";
        LAST_BODY_PRACTICE_DETAIL_TITLE = "id:Jacobson Relaxation Exercise";
        LAST_MIND_PRACTICE_CARD = "xpath:(//XCUIElementTypeStaticText[@name='MIND PRACTICES']/following-sibling::XCUIElementTypeCollectionView//XCUIElementTypeOther[@name='ExploreExerciseManCellView'])[last()]";
        LAST_MIND_PRACTICE_TITLE = "id:Triple Column";
        LAST_MIND_PRACTICE_DETAIL_TITLE = "id:Triple Column Practice";
        PRACTICE_DETAIL_HEADER = "id:DailyStartHeaderView";
        PRACTICE_DETAIL_GOAL = "id:GOAL";
        PRACTICE_DETAIL_START_BUTTON = "id:START EXERCISE";
        CONCEPT_POPUP_CONFIRM_BUTTON = "id:Got it";

        OVERALL_HEALTH_PROGRAM = "xpath://XCUIElementTypeOther[@name='overall_health_program'] | //XCUIElementTypeStaticText[@name='Overall Health']";
        PROGRAM_SETTINGS_BUTTON = "id:CoachProgramSettingsIcon";
        PROGRAM_SETTINGS_SCREEN = "id:Removed exercises";
        REMOVED_EXERCISES_BUTTON = "xpath://XCUIElementTypeButton[@name='Removed exercises'] | //XCUIElementTypeStaticText[@name='Removed exercises']";
        REMOVED_EXERCISES_SCREEN = "id:Restore";
        REMOVED_EXERCISE_CARD = "id:DailyPlanItem";
        REMOVED_EXERCISE_CHECKBOX = "id:SettingsCheckBoxInactive";
        RECOVER_REMOVED_EXERCISE_BUTTON = "id:Restore";
        PROGRAM_DAY_CONTAINERS = "id:program_day_container";
        RECOVERED_EXERCISE_CARDS = "id:recovered_exercise_card";
    }

    public iOSExplorePageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    public void openOverallHealthProgramSettings() {
        openFixtureProgramSettings("Overall Health");
    }

    public void openKegelProgramSettings() { openFixtureProgramSettings("Kegel Challenge"); }

    private void openFixtureProgramSettings(String program) {
        String collection = "xpath://XCUIElementTypeOther[@name='CoachProgramDetailedSectionViewCoachProgramDetailedManCard' "
                + "and .//XCUIElementTypeStaticText[@name='ALL PROGRAMS']]/XCUIElementTypeCollectionView";
        String title = collection + "//XCUIElementTypeOther[@name='CoachProgramDetailedManCard']"
                + "/XCUIElementTypeImage/XCUIElementTypeStaticText[@label='" + program + "' and @visible='true']";
        for (int i = 0; i < 12 && !isElementVisible(title); i++) {
            Rectangle bounds = waitForElementPresent(collection, "ALL PROGRAMS carousel is not visible", 15).getRect();
            Map<String, Object> gesture = new HashMap<String, Object>();
            gesture.put("duration", .65);
            gesture.put("fromX", bounds.getX() + bounds.getWidth() * .8);
            gesture.put("toX", bounds.getX() + bounds.getWidth() * .2);
            gesture.put("fromY", bounds.getY() + bounds.getHeight() * .5);
            gesture.put("toY", bounds.getY() + bounds.getHeight() * .5);
            ((JavascriptExecutor) driver).executeScript("mobile: dragFromToForDuration", gesture);
        }
        waitForElementAndClick(title, program + " is absent from ALL PROGRAMS", 10);
        // Catalog selection returns before the fetched detail screen appears.
        // Wait for that screen transition before looking for its Settings action.
        waitForElementPresent(PROGRAM_DETAIL_TITLE, program + " detail did not load", 30);
        waitForElementAndClick(PROGRAM_SETTINGS_BUTTON, "Cannot open " + program + " Settings", 15);
        waitForElementPresent(PROGRAM_SETTINGS_SCREEN, program + " Settings did not open", 10);
    }

    @Override
    public void selectRemovedExerciseAndRecover() {
        String card = "xpath://XCUIElementTypeOther[@name='DailyPlanItem' "
                + "and .//XCUIElementTypeStaticText[@name='Check Your Meal Plan']]";
        waitForElementPresent(card, "The prepared removed meal-plan card is absent", 15);
        Assert.assertEquals("The removed fixture card must be unambiguous", 1, getAmountElements(card));
        waitForElementAndClick(card + "//XCUIElementTypeImage[@name='SettingsCheckBoxInactive']",
                "The fixture checkbox must initially be unchecked", 10);
        waitForElementPresent(card + "//XCUIElementTypeImage[@name='SettingsCheckBoxActive']",
                "The fixture checkbox did not become checked", 10);
        // In 1.13.29 the action is labelled Restore (RECOVER in the original TC).
        waitForElementEnabled(RECOVER_REMOVED_EXERCISE_BUTTON, "Restore did not become enabled", 10);
        waitForElementAndClick(RECOVER_REMOVED_EXERCISE_BUTTON, "Cannot restore the selected fixture card", 10);
        waitForElementNotPresent(REMOVED_EXERCISES_SCREEN, "Removed exercises modal did not close", 15);
        waitForElementPresent(PROGRAM_SETTINGS_SCREEN, "Recovery did not return to program Settings", 10);
    }

    @Override
    @io.qameta.allure.Step("Verify the restored fixture card is rendered on Overall Health day 2")
    public void assertRecoveredFixtureCardIsRendered() {
        waitForElementAndClick("id:navBarRoundBack", "Cannot return to the restored program", 10);
        waitForElementPresent("id:CoachProgramSettingsIcon", "Overall Health detail did not reappear", 10);
        waitForElementPresent("id:Day 1", "Expected a fresh Overall Health day-1 view", 10);
        waitForElementAndClick("id:rightSwitcherArrow", "Cannot open the prepared second day", 10);
        waitForElementPresent("id:Day 2", "Overall Health day 2 did not open", 15);
        String card = "xpath://XCUIElementTypeOther[@name='DailyPlanItem' and @visible='true' "
                + "and .//XCUIElementTypeStaticText[@name='Check Your Meal Plan' and @visible='true']]";
        for (int i = 0; i < 6 && !isElementVisible(card); i++) swipeUpQuick();
        waitForElementPresent(card, "The restored card was not rendered on day 2", 10);
        Assert.assertEquals("The restored card is duplicated in the day-2 UI", 1, getAmountElements(card));
        io.qameta.allure.Allure.addAttachment("Restored card rendered on day 2", "image/png",
                new java.io.ByteArrayInputStream(((org.openqa.selenium.TakesScreenshot) driver)
                        .getScreenshotAs(org.openqa.selenium.OutputType.BYTES)), "png");
    }

    @Override
    public List<String> getProgramNamesFromExplore() {
        // The current build groups every program in this horizontally recycled
        // ALL PROGRAMS collection. Require that marker so a different layout is
        // never silently compared using only the first visible cards.
        String collection = "xpath://XCUIElementTypeOther[@name='CoachProgramDetailedSectionViewCoachProgramDetailedManCard' "
                + "and .//XCUIElementTypeStaticText[@name='ALL PROGRAMS']]/XCUIElementTypeCollectionView";
        waitForElementPresent(collection, "The full ALL PROGRAMS collection is not exposed in this Explore variant", 15);
        String titles = "xpath://XCUIElementTypeOther[@name='CoachProgramDetailedSectionViewCoachProgramDetailedManCard' "
                + "and .//XCUIElementTypeStaticText[@name='ALL PROGRAMS']]//XCUIElementTypeOther[@name='CoachProgramDetailedManCard']/XCUIElementTypeImage/XCUIElementTypeStaticText";
        iOSProgramListReader reader = new iOSProgramListReader(driver);
        reader.rewind(titles, collection);
        return reader.collect(titles, collection, true);
    }
}
