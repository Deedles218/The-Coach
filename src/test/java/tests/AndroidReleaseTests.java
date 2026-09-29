package tests;
import lib.*;
import lib.ui.android.*;
import io.qameta.allure.*;
import org.junit.*;

public class AndroidReleaseTests extends AndroidTestCase {
    private AndroidCoachFlowPageObject authorized(TestData.TestAccount account) {
        AndroidCoachFlowPageObject coach=new AndroidCoachFlowPageObject(driver);
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(),account.getOtp());return coach;
    }
    @Test @Issue("COA-8178") public void testOnlyCurrentThreeTabsNavigate() {
        AndroidCoachFlowPageObject coach=authorized(TestData.existingProgressAccount());
        coach.assertMainTabsAreDisplayed();coach.openToday();coach.openExplore();coach.openShop();
    }
    @Test @Issue("COA-8178") public void testConfiguredExploreSections() {
        authorized(TestData.existingProgressAccount());AndroidExplorePageObject page=new AndroidExplorePageObject(driver);
        page.openExploreTab();page.assertConfiguredSectionsAreDisplayed();
    }
    @Test @Issue("COA-8174") public void testExploreProgramCatalogAndRetiredSections() {
        authorized(TestData.existingProgressAccount());AndroidExplorePageObject page=new AndroidExplorePageObject(driver);
        page.openExploreTab();page.assertMainProgramsRecommendedSection();page.assertRemovedCoursesAndPracticesAreAbsent();
    }
    @Test @Issue("COA-8870") public void testWorkbookPaywallClosesWithoutPurchase() {
        TestData.noPdfEntitlementFixtureId();
        AndroidCoachFlowPageObject coach=authorized(TestData.noPdfEntitlementAccount());
        coach.openProfile();coach.openMyWorkbookFromProfile();coach.assertPdfGuideUpsellIsDisplayed();coach.closePdfGuideUpsell();
        coach.assertProfileScreenIsDisplayed();
    }
}
