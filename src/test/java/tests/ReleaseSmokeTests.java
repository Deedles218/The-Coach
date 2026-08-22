package tests;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.ExplorePageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import lib.ui.factories.ExplorePageObjectFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Epic(value = "The Coach Release Smoke")
public class ReleaseSmokeTests extends CoreTestCase {
    @Test
    @Features(value = {@Feature(value = "Navigation"), @Feature(value = "Main tabs")})
    @Issue("COA-8178")
    @DisplayName("Release smoke: Today, Explore and Shop are the only main tabs")
    @Description("Verifies the authorized app exposes the current three-tab navigation and that Today, Explore and Shop open successfully. Feed is not part of the release smoke contract.")
    @Step("Start test testMainThreeTabNavigation")
    @Severity(value = SeverityLevel.CRITICAL)
    public void testMainThreeTabNavigation() {
        requireIOSPlatform();

        CoachFlowPageObject coachFlow = authorizedCoachFlow(TestData.existingProgressAccount());
        coachFlow.assertMainTabsAreDisplayed();
        coachFlow.openToday();
        coachFlow.openExplore();
        coachFlow.openShop();
    }

    @Test
    @Features(value = {@Feature(value = "Explore"), @Feature(value = "Current UI")})
    @Issue("COA-8178")
    @DisplayName("Release smoke: current Explore sections are displayed")
    @Description("Treats the current Explore layout as canonical: Recommended for you, Sexual Health, Courses, Body Practices and Mind Practices.")
    @Step("Start test testCurrentExploreSectionsAreDisplayed")
    @Severity(value = SeverityLevel.CRITICAL)
    public void testCurrentExploreSectionsAreDisplayed() {
        requireIOSPlatform();

        ExplorePageObject explore = ExplorePageObjectFactory.get(driver);
        Assert.assertNotNull("Explore page object is not available for current platform", explore);
        authorizedCoachFlow(TestData.existingProgressAccount());
        explore.openExploreTab();
        explore.assertCurrentSectionsAreDisplayed();
    }

    @Test
    @Features(value = {@Feature(value = "Explore"), @Feature(value = "Recommended"), @Feature(value = "Custom Kegel")})
    @Issue("COA-8174")
    @DisplayName("Release smoke: current Explore Recommended and Custom Kegel")
    @Description("Verifies the current Explore layout, a Recommended card collection, and the Custom Kegel entry point. The current Explore UI is the canonical expected result.")
    @Step("Start test testExploreRecommendedAndCustomKegel")
    @Severity(value = SeverityLevel.CRITICAL)
    public void testExploreRecommendedAndCustomKegel() {
        requireIOSPlatform();

        CoachFlowPageObject coachFlow = authorizedCoachFlow(TestData.existingProgressAccount());
        ExplorePageObject explore = ExplorePageObjectFactory.get(driver);
        Assert.assertNotNull("Explore page object is not available for current platform", explore);
        explore.openExploreTab();
        explore.assertCurrentSectionsAreDisplayed();
        explore.assertMainProgramsRecommendedSection();
        explore.assertBodyPracticesSection();
        explore.openCustomKegelAndVerifyStartScreen();
        explore.closeCustomKegel();
        coachFlow.openToday();
    }

    @Test
    @Ignore("Blocked until the dedicated no-PDF entitlement account and fixture are provisioned")
    @Features(value = {@Feature(value = "My Workbook"), @Feature(value = "PDF paywall")})
    @Issue("COA-8870")
    @DisplayName("Release smoke: My Workbook shows PDF paywall for a non-entitled user")
    @Description("Uses a dedicated account without PDF entitlement, opens My Workbook, verifies the paywall, and closes it without purchasing.")
    @Step("Start test testMyWorkbookPdfPaywallCanBeClosed")
    @Severity(value = SeverityLevel.CRITICAL)
    public void testMyWorkbookPdfPaywallCanBeClosed() {
        requireIOSPlatform();

        TestData.noPdfEntitlementFixtureId();
        CoachFlowPageObject coachFlow = authorizedCoachFlow(TestData.noPdfEntitlementAccount());
        coachFlow.openProfile();
        coachFlow.openMyWorkbookFromProfile();
        coachFlow.assertPdfGuideUpsellIsDisplayed();
        coachFlow.closePdfGuideUpsell();
    }

    @Test
    @Features(value = {@Feature(value = "Update"), @Feature(value = "Authorization"), @Feature(value = "Progress")})
    @Issue("COA-7914")
    @DisplayName("Release smoke: update preserves authorization and progress")
    @Description("Installs the old configured build, establishes an authorized progress state, updates the same bundle with the new build, and verifies that authorization and Daily Plan content remain available.")
    @Step("Start test testUpdatePreservesAuthorizationAndProgress")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testUpdatePreservesAuthorizationAndProgress() {
        requireIOSPlatform();

        requireUpdateConfiguration();
        Assert.assertTrue("The current driver must support app installation", driver instanceof InteractsWithApps);

        InteractsWithApps apps = (InteractsWithApps) driver;
        CoachFlowPageObject coachFlow = authorizedCoachFlow(TestData.existingProgressAccount());
        apps.installApp(Platform.getInstance().getIOSUpdateAppPath());
        coachFlow.activateAppIfPossible();
        coachFlow.assertAuthorizedDashboardIsDisplayed();
        coachFlow.openToday();
        coachFlow.assertMainTabsAreDisplayed();
        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);
        TestData.Fixture fixture = TestData.deterministicFixture();
        dailyPlan.openTodayTab();
        dailyPlan.assertDailyPlanDaySwitcherIsDisplayed();
        dailyPlan.assertCurrentDayMatches(fixture.getDailyPlanDay());
        dailyPlan.assertDailyPracticeIsDisplayed();
    }

    private CoachFlowPageObject authorizedCoachFlow(TestData.TestAccount account) {
        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);
        TestData.deterministicFixture();
        coachFlow.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        return coachFlow;
    }

    private void requireUpdateConfiguration() {
        Assert.assertTrue(
                "Update smoke requires -Dios.fullReset=false and -Dios.noReset=true",
                Platform.getInstance().isUpdateTestConfigured()
        );
        Assert.assertTrue(
                "Update smoke requires -Dios.app=/path/to/old/The-Coach.app or IOS_APP",
                Platform.getInstance().getIOSAppPath() != null && !Platform.getInstance().getIOSAppPath().trim().isEmpty()
        );
        Assert.assertTrue(
                "Update smoke requires -Dios.update.app=/path/to/new/The-Coach.app or IOS_UPDATE_APP",
                Platform.getInstance().getIOSUpdateAppPath() != null && !Platform.getInstance().getIOSUpdateAppPath().trim().isEmpty()
        );
    }
}
