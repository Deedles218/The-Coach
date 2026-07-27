package lib.ui.ios;

import lib.ui.ExplorePageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

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
    }

    public iOSExplorePageObject(RemoteWebDriver driver) {
        super(driver);
    }
}
