package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.ui.CoachFlowPageObject;
import lib.ui.ExplorePageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.ExplorePageObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Epic(value = "The Coach Explore")
public class ExploreTests extends CoreTestCase {
    private static final String EXISTING_PROGRESS_EMAIL = "ds@vamapps.com";
    private static final String OTP_CODE = "8654";

    private ExplorePageObject explore;

    @Before
    public void openExploreForTest() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        explore = ExplorePageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);
        Assert.assertNotNull("Explore page object is not available for current platform", explore);
        if (!explore.isExploreContextAvailable()) {
            coachFlow.ensureExistingProgressUserIsLoggedIn(EXISTING_PROGRESS_EMAIL, OTP_CODE);
        }
        explore.openExploreTab();
    }

    @Test
    @Features({@Feature("Explore navigation"), @Feature("Explore regression")})
    @DisplayName("COA-8178 COA-8179 COA-8180 Explore uses the current navigation")
    @Description("Verifies the current Explore tab plus absence of retired Programs/Tools navigation and sections.")
    @Step("Start Explore navigation regression test")
    @Severity(SeverityLevel.CRITICAL)
    public void testExploreUsesCurrentNavigation() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }
        explore.assertRetiredExploreUiIsAbsent();
    }

    @Test
    @Features({@Feature("Explore sections"), @Feature("Main programs")})
    @DisplayName("COA-8181 Main programs displays Recommended for you")
    @Description("Verifies the current Main programs area: the legacy group heading is no longer rendered, while Recommended for you contains program cards.")
    @Step("Start Main programs section test")
    @Severity(SeverityLevel.CRITICAL)
    public void testMainProgramsDisplaysRecommendedForYou() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }
        explore.assertMainProgramsRecommendedSection();
    }

    @Test
    @Features({@Feature("Explore sections"), @Feature("Courses")})
    @DisplayName("COA-8182 Courses section displays course cards")
    @Description("Verifies the Courses heading, at least one course card, and a non-empty course image area.")
    @Step("Start Courses section test")
    @Severity(SeverityLevel.NORMAL)
    public void testCoursesSectionDisplaysCards() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }
        explore.assertCoursesSection();
    }

    @Test
    @Features({@Feature("Explore sections"), @Feature("Body Practices")})
    @DisplayName("Body Practices section displays practice cards")
    @Description("Verifies the Body Practices heading, at least one scoped practice card, and the current Custom Kegel entry.")
    @Step("Start Body Practices section test")
    @Severity(SeverityLevel.NORMAL)
    public void testBodyPracticesSectionDisplaysCards() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }
        explore.assertBodyPracticesSection();
    }

    @Test
    @Features({@Feature("Explore sections"), @Feature("Mind Practices")})
    @DisplayName("Mind Practices section displays practice cards")
    @Description("Scrolls using an element-based condition and verifies the lazily loaded Mind Practices heading and scoped cards.")
    @Step("Start Mind Practices section test")
    @Severity(SeverityLevel.NORMAL)
    public void testMindPracticesSectionDisplaysCards() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }
        explore.assertMindPracticesSection();
    }

    @Test
    @Features({@Feature("Explore swipe navigation"), @Feature("Main programs")})
    @DisplayName("Swipe Recommended for you and open the last program")
    @Description("Horizontally swipes the Recommended for you carousel until its last card is fully visible, opens it, verifies program details, and returns.")
    @Step("Start last Recommended program swipe test")
    @Severity(SeverityLevel.CRITICAL)
    public void testSwipeToLastRecommendedProgramAndOpenIt() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }
        explore.swipeToAndOpenLastRecommendedProgram();
        explore.closeProgramDetails();
    }

    @Test
    @Features({@Feature("Explore swipe navigation"), @Feature("Courses")})
    @DisplayName("Swipe Courses and open the last course")
    @Description("Horizontally swipes the Courses carousel until its last card is fully visible, opens it, verifies course details, and returns.")
    @Step("Start last Course swipe test")
    @Severity(SeverityLevel.CRITICAL)
    public void testSwipeToLastCourseAndOpenIt() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }
        explore.swipeToAndOpenLastCourse();
        explore.closeCourseDetails();
    }

    @Test
    @Features({@Feature("Explore swipe navigation"), @Feature("Body Practices")})
    @DisplayName("Swipe Body Practices and open the last practice")
    @Description("Horizontally swipes Body Practices until its last card is fully visible, opens it, verifies the exercise start screen, and returns.")
    @Step("Start last Body Practice swipe test")
    @Severity(SeverityLevel.CRITICAL)
    public void testSwipeToLastBodyPracticeAndOpenIt() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }
        explore.swipeToAndOpenLastBodyPractice();
        explore.closePracticeDetails();
    }

    @Test
    @Features({@Feature("Explore swipe navigation"), @Feature("Mind Practices")})
    @DisplayName("Swipe Mind Practices and open the last practice")
    @Description("Finds Mind Practices, horizontally swipes until its last card is fully visible, opens it, verifies the exercise start screen, and returns.")
    @Step("Start last Mind Practice swipe test")
    @Severity(SeverityLevel.CRITICAL)
    public void testSwipeToLastMindPracticeAndOpenIt() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }
        explore.swipeToAndOpenLastMindPractice();
        explore.closePracticeDetails();
    }

    @Test
    @Features({@Feature("Explore programs"), @Feature("Program details")})
    @DisplayName("COA-8183 COA-8184 COA-8224 Recommended program card opens program details")
    @Description("Verifies a current image-backed recommended program card is tappable, opens its detail screen, and does not show the concept-program popup.")
    @Step("Start recommended program navigation test")
    @Severity(SeverityLevel.CRITICAL)
    public void testRecommendedProgramOpensDetails() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }
        explore.openFirstRecommendedProgramAndVerifyDetails();
        explore.closeProgramDetails();
    }

    @Test
    @Features({@Feature("Explore courses"), @Feature("Course details")})
    @DisplayName("COA-8185 Explore course cards are compact and open course details")
    @Description("Compares current course-card dimensions with program cards and verifies the first video course opens its detail content.")
    @Step("Start Explore course card test")
    @Severity(SeverityLevel.NORMAL)
    public void testCourseCardsAreCompactAndOpenDetails() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }
        explore.assertCourseCardsAreCompactAndOpenFirstCourse();
        explore.closeCourseDetails();
    }

    @Test
    @Features({@Feature("Explore body practices"), @Feature("Custom Kegel")})
    @DisplayName("COA-8174 Custom Kegel is available from Explore")
    @Description("Opens Custom Kegel from the current Body Practices section and verifies its start screen without beginning a workout.")
    @Step("Start Custom Kegel from Explore test")
    @Severity(SeverityLevel.CRITICAL)
    public void testCustomKegelIsAvailableFromExplore() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }
        explore.openCustomKegelAndVerifyStartScreen();
        explore.closeCustomKegel();
    }
}
