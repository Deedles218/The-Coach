package tests;

import io.qameta.allure.*;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.ExploreStateRestorer;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.ExploreBannerPageObject;
import lib.ui.ExplorePageObject;
import lib.ui.android.AndroidExploreBannerPageObject;
import lib.ui.android.AndroidExplorePageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.ExplorePageObjectFactory;
import lib.ui.factories.OnboardingPageObjectFactory;
import lib.ui.ios.IOSExploreBannerPageObject;
import lib.ui.ios.iOSExplorePageObject;
import org.junit.*;

/** Configured contract per platform/variant; legacy method names remain stable for suite selectors. */
@Epic("The Coach Explore")
@Feature("Configured Explore parity")
public class ConfiguredExploreTests extends CoreTestCase {
    private ExplorePageObject explore;
    private ExploreStateRestorer originalState;
    private final boolean preparedProfile = Boolean.getBoolean("coach.explore.preparedProfile");

    @Before @Override public void setUp() throws Exception {
        if (preparedProfile) PreparedMobileProfile.assertPreservationAllowed();
        super.setUp();
        openExplore();
    }

    private void openExplore() {
        requireMobilePlatform();
        OnboardingPageObjectFactory.get(driver).closePaywallsAndPopups();
        explore = ExplorePageObjectFactory.get(driver);
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        if (preparedProfile) {
            explore.closeTransientDetailIfPresent();
            PreparedMobileProfile.verifyFromToday(coach);
        } else {
            TestData.TestAccount account = TestData.existingProgressAccount();
            coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        }
        originalState = ExploreStateRestorer.capture(driver);
        explore.openExploreTab();
    }

    @After public void closeDestinationAndRestoreProgram() throws Exception {
        if (explore == null) return;
        if (originalState == null) {
            explore.closeTransientDetailIfPresent();
            return;
        }
        originalState.restoreAfter(() -> {
            Allure.addAttachment("Explore before restoration", "image/png",
                    new java.io.ByteArrayInputStream(((org.openqa.selenium.TakesScreenshot) driver)
                            .getScreenshotAs(org.openqa.selenium.OutputType.BYTES)), "png");
            Allure.addAttachment("Explore source before restoration", "application/xml",
                    TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
        },
                explore::closeTransientDetailIfPresent);
    }

    @Override protected void cleanupSessionForNextTest() {
        if (!preparedProfile) super.cleanupSessionForNextTest();
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
    @Test @Issue("COA-9331") public void testCoa9331ActiveProgramBlockIsAbsent() { explore.assertActiveProgramBlockIsAbsent(); }
    @Test @Issue("COA-9332") public void testCoa9332BrowseProgramsCardsAreReadable() {
        if (Platform.getInstance().isAndroid()) ((AndroidExplorePageObject) explore).assertBrowseProgramsCardsAreReadable();
        else ((iOSExplorePageObject) explore).assertBrowseProgramsCardsAreReadable();
    }
    @Test @Issue("COA-9503")
    @DisplayName("COA-9503 Explore banner loads its external landing page within The Coach")
    public void testCoa9503ExploreBannerOpensExternalPage() {
        exploreBanner().openExternalCoachingPage();
    }
    @Test @Issue("COA-9504")
    @DisplayName("COA-9504 Explore banner renders fully without covering the surrounding interface")
    public void testCoa9504ExploreBannerRendersIntact() throws Exception {
        exploreBanner().assertImageIsVisibleWithoutOverlap();
    }

    private ExploreBannerPageObject exploreBanner() {
        Platform platform = Platform.getInstance();
        if (platform.isIOS()) {
            Assert.assertEquals("Use the reviewed male production Explore fixture", "com.vamapps.The-Coach",
                    platform.getIOSBundleId());
            return new IOSExploreBannerPageObject(driver);
        }
        Assert.assertEquals("Use the reviewed male Android Explore fixture", "com.vamapps.thecoach",
                platform.getAndroidAppPackage());
        return new AndroidExploreBannerPageObject(driver);
    }
    @Test public void testBrowseProgramsReplacesLegacyBlock() { explore.assertBrowseProgramsReplacesLegacyBlock(); }
    @Test public void testConfiguredCardTemplates() {
        explore.assertConfiguredCardTemplates();
        explore.assertConfiguredCardTitlesAreNonEmpty();
    }
    @Test @DisplayName("Quick Tip opens its configured content (article on male iOS production)")
    public void testQuickTipVideoCardOpensVideoPlayer() { explore.openQuickTipVideoAndVerifyPlayer(); }
    @Test public void testMasterClassCardOpensLessonOrWebView() { explore.openMasterClassAndVerifyDestination(); }
    @Test @Issue("COA-9329") public void testPrivateCoachingCardOpensWebView() {
        explore.openPrivateCoachingAndVerifyWebView();
        explore.closePrivateCoachingWebView();
    }
}
