package tests;

import io.qameta.allure.*;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.ExploreStateRestorer;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.ExplorePageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.ExplorePageObjectFactory;
import lib.ui.factories.OnboardingPageObjectFactory;
import org.junit.*;

/** Configured contract per platform/variant; legacy method names remain stable for suite selectors. */
@Epic("The Coach Explore")
@Feature("Configured Explore parity")
public class ConfiguredExploreTests extends CoreTestCase {
    private ExplorePageObject explore;
    private ExploreStateRestorer originalState;

    @Before public void openExplore() {
        requireMobilePlatform();
        OnboardingPageObjectFactory.get(driver).closePaywallsAndPopups();
        explore = ExplorePageObjectFactory.get(driver);
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        TestData.TestAccount account = TestData.existingProgressAccount();
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        originalState = ExploreStateRestorer.capture(driver);
        explore.openExploreTab();
    }

    @After public void closeDestinationAndRestoreProgram() throws Exception {
        if (explore == null) return;
        if (originalState == null) {
            explore.closeTransientDetailIfPresent();
            return;
        }
        originalState.restoreAfter(() -> Allure.addAttachment("Explore before restoration", "image/png",
                new java.io.ByteArrayInputStream(((org.openqa.selenium.TakesScreenshot) driver)
                        .getScreenshotAs(org.openqa.selenium.OutputType.BYTES)), "png"),
                explore::closeTransientDetailIfPresent);
    }

    @Test @Issue("COA-8178") public void testCurrentExploreNavigation() { explore.assertRetiredExploreUiIsAbsent(); }
    @Test @Issue("COA-8181") public void testProgramCatalogContainsCards() { explore.assertMainProgramsRecommendedSection(); }
    @Test @Issue("COA-8182") @Description("Verify the configured section contract: male iOS production retains Body Practices and removes Courses; other variants retain their existing expectations.")
    public void testRemovedCoursesAndPracticesStayAbsent() { explore.assertRemovedCoursesAndPracticesAreAbsent(); }
    @Test public void testSwipeToLastProgramAndOpenIt() {
        explore.swipeToAndOpenLastRecommendedProgram();
        explore.closeProgramDetails();
    }
    @Test @Issue("COA-8183") public void testProgramCardOpensDetails() {
        explore.openFirstRecommendedProgramAndVerifyDetails();
        explore.closeProgramDetails();
    }
    @Test public void testConfiguredSectionsAreDisplayed() { explore.assertConfiguredSectionsAreDisplayed(); }
    @Test public void testCoursesSectionIsRemoved() { explore.assertCoursesSectionIsRemoved(); }
    @Test public void testBrowseProgramsReplacesLegacyBlock() { explore.assertBrowseProgramsReplacesLegacyBlock(); }
    @Test public void testConfiguredCardTemplates() {
        explore.assertConfiguredCardTemplates();
        explore.assertConfiguredCardTitlesAreNonEmpty();
    }
    @Test @DisplayName("Quick Tip opens its configured content (article on male iOS production)")
    public void testQuickTipVideoCardOpensVideoPlayer() { explore.openQuickTipVideoAndVerifyPlayer(); }
    @Test public void testMasterClassCardOpensLessonOrWebView() { explore.openMasterClassAndVerifyDestination(); }
    @Test public void testPrivateCoachingCardOpensWebView() {
        explore.openPrivateCoachingAndVerifyWebView();
        explore.closePrivateCoachingWebView();
    }
}
