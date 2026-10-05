package tests;

import io.qameta.allure.*;
import lib.CoreTestCase;
import lib.Platform;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.ExplorePageObject;
import lib.ui.android.AndroidProgramSelectorPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.ExplorePageObjectFactory;
import lib.ui.factories.OnboardingPageObjectFactory;
import org.junit.*;

/** Current product contract on both platforms; legacy Explore tests remain separate. */
@Epic("The Coach Explore")
@Feature("Configured Explore parity")
public class ConfiguredExploreTests extends CoreTestCase {
    private ExplorePageObject explore;
    private AndroidProgramSelectorPageObject today;
    private String originalProgram, originalStage, originalProgress;

    @Before public void openExplore() {
        requireMobilePlatform();
        OnboardingPageObjectFactory.get(driver).closePaywallsAndPopups();
        explore = ExplorePageObjectFactory.get(driver);
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        TestData.TestAccount account = TestData.existingProgressAccount();
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        if (Platform.getInstance().isAndroid()) {
            today = new AndroidProgramSelectorPageObject(driver);
            today.openTodayTab();
            originalProgram = today.getActiveProgramName();
            originalStage = today.getCurrentDayLabel();
            originalProgress = today.getProgramProgressValue();
        }
        explore.openExploreTab();
    }

    @After public void closeDestinationAndRestoreProgram() {
        if (explore == null) return;
        Allure.addAttachment("Explore before restoration", "image/png",
                new java.io.ByteArrayInputStream(((org.openqa.selenium.TakesScreenshot) driver)
                        .getScreenshotAs(org.openqa.selenium.OutputType.BYTES)), "png");
        try {
            explore.closeTransientDetailIfPresent();
        } finally {
            if (today != null && originalProgram != null && originalStage != null && originalProgress != null) {
                today.openTodayTab();
                if (!originalProgram.equals(today.getActiveProgramName())) {
                    today.openProgramSelector();
                    today.selectProgram(originalProgram);
                }
                Assert.assertEquals("Explore changed the original stage", originalStage, today.getCurrentDayLabel());
                Assert.assertEquals("Explore changed the original progress", originalProgress, today.getProgramProgressValue());
            }
        }
    }

    @Test @Issue("COA-8178") public void testCurrentExploreNavigation() { explore.assertRetiredExploreUiIsAbsent(); }
    @Test @Issue("COA-8181") public void testProgramCatalogContainsCards() { explore.assertMainProgramsRecommendedSection(); }
    @Test @Issue("COA-8182") public void testRemovedCoursesAndPracticesStayAbsent() { explore.assertRemovedCoursesAndPracticesAreAbsent(); }
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
    @Test public void testQuickTipVideoCardOpensVideoPlayer() { explore.openQuickTipVideoAndVerifyPlayer(); }
    @Test public void testMasterClassCardOpensLessonOrWebView() { explore.openMasterClassAndVerifyDestination(); }
    @Test public void testPrivateCoachingCardOpensWebView() {
        explore.openPrivateCoachingAndVerifyWebView();
        explore.closePrivateCoachingWebView();
    }
}
