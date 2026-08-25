package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.android.AndroidExplorePageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.ExplorePageObjectFactory;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

@Epic(value = "The Coach Android Explore")
public class AndroidExploreTests extends CoreTestCase {
    private AndroidExplorePageObject explore;

    @Before
    public void openAndroidExploreForTest() {
        Assume.assumeTrue(
                "Android Explore tests require -Dplatform=android",
                Platform.getInstance().isAndroid()
        );

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        explore = (AndroidExplorePageObject) ExplorePageObjectFactory.get(driver);

        explore.closeTransientDetailIfPresent();
        if (!explore.isExploreContextAvailable()) {
            TestData.TestAccount account = TestData.existingProgressAccount();
            coachFlow.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        }
        explore.openExploreTab();
    }

    @Test
    @Features({@Feature("Explore sections"), @Feature("Remote configuration")})
    @DisplayName("Android Explore renders the configured sections")
    @Description("Checks the Explore title and the configured Quick Tips, Master Classes and Private coaching sections.")
    @Severity(SeverityLevel.CRITICAL)
    public void testConfiguredSectionsAreDisplayed() {
        explore.assertConfiguredSectionsAreDisplayed();
    }

    @Test
    @Features({@Feature("Explore sections"), @Feature("Remote configuration")})
    @DisplayName("Courses section is removed from Android Explore")
    @Description("The configurable Explore contract must not render the legacy Courses section.")
    @Severity(SeverityLevel.CRITICAL)
    public void testCoursesSectionIsRemoved() {
        explore.assertCoursesSectionIsRemoved();
    }

    @Test
    @Features({@Feature("Explore programs"), @Feature("Remote configuration")})
    @DisplayName("Browse Programs replaces the legacy active-program block")
    @Description("The v7 Explore contract requires Browse Programs and no legacy ALL PROGRAMS/active-program RecyclerView.")
    @Severity(SeverityLevel.CRITICAL)
    public void testBrowseProgramsReplacesLegacyBlock() {
        explore.assertBrowseProgramsReplacesLegacyBlock();
    }

    @Test
    @Features({@Feature("Explore cards"), @Feature("Remote configuration")})
    @DisplayName("Configured card templates expose images and titles")
    @Description("Checks square Quick Tips cards, challenge Master Classes cards and private-coaching cards.")
    @Severity(SeverityLevel.NORMAL)
    public void testConfiguredCardTemplates() {
        explore.assertConfiguredCardTemplates();
        explore.assertConfiguredCardTitlesAreNonEmpty();
    }

    @Test
    @Features({@Feature("Explore navigation"), @Feature("Lessons and practices")})
    @DisplayName("Quick Tips card opens a lesson or practice")
    @Description("A square configured card must leave Explore and open a native lesson/practice or WebView destination after one tap.")
    @Severity(SeverityLevel.CRITICAL)
    public void testQuickTipCardOpensLessonOrPractice() {
        explore.openQuickTipAndVerifyDestination();
    }

    @Test
    @Features({@Feature("Explore navigation"), @Feature("Master Classes")})
    @DisplayName("Master Class card opens a lesson or WebView")
    @Description("A challenge configured card must leave Explore and open its destination after one tap.")
    @Severity(SeverityLevel.CRITICAL)
    public void testMasterClassCardOpensLessonOrWebView() {
        explore.openMasterClassAndVerifyDestination();
    }

    @Test
    @Features({@Feature("Explore navigation"), @Feature("Private coaching")})
    @DisplayName("Private coaching card opens its WebView")
    @Description("The coaching card must expose the Learn More action, the private-session WebView and its booking action.")
    @Severity(SeverityLevel.NORMAL)
    public void testPrivateCoachingCardOpensWebView() {
        explore.openPrivateCoachingAndVerifyWebView();
        explore.closePrivateCoachingWebView();
    }
}
