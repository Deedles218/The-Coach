package tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import lib.CoreTestCase;
import lib.ui.MasterclassPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/** Select tests matching the explicitly configured, pre-authenticated entitlement fixture. */
@Epic("The Coach") @Feature("Masterclasses — iOS / Android, Him / Her")
public class MasterclassTests extends CoreTestCase {
    private MasterclassPageObject course;

    @Before public void prepareMasterclass() {
        requireMobilePlatform();
        Assert.assertEquals("Masterclass fixtures must preserve login and purchased access",
                "logout", lib.TestData.testIsolationMode());
        course = new MasterclassPageObject(driver);
        if (lib.Platform.getInstance().isAndroid()) {
            lib.ui.factories.OnboardingPageObjectFactory.get(driver).closePaywallsAndPopups();
        }
        if (Boolean.getBoolean("masterclass.login")) {
            lib.TestData.TestAccount account = lib.TestData.existingProgressAccount();
            lib.ui.CoachFlowPageObject coach = lib.ui.factories.CoachFlowPageObjectFactory.get(driver);
            if (Boolean.getBoolean("masterclass.prepareOnboarding") && lib.Platform.getInstance().isAndroid()) {
                ((lib.ui.android.AndroidCoachFlowPageObject) coach)
                        .authenticateForFixtureSetup(account.getEmail(), account.getOtp());
            } else {
                coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
            }
        }
        if (Boolean.getBoolean("masterclass.prepareOnboarding")) {
            Assert.assertTrue("This fixture preparation is Android-only", lib.Platform.getInstance().isAndroid());
            lib.ui.android.AndroidOnboardingPageObject onboarding =
                    new lib.ui.android.AndroidOnboardingPageObject(driver);
            if (course.isElementVisible("xpath://android.widget.Button[@text='BOOST OVERALL HEALTH']")) {
                onboarding.selectGoal(lib.ui.OnboardingGoal.BOOST_OVERALL_HEALTH);
            }
            onboarding.completeQuestionnaire();
            onboarding.closePaywallsAndPopups();
            onboarding.waitForToday();
        }
        openCourse();
    }

    private void openCourse() {
        if (lib.Platform.getInstance().isAndroid()) {
            lib.ui.factories.OnboardingPageObjectFactory.get(driver).closePaywallsAndPopups();
        } else {
            lib.ui.CoachFlowPageObject coach = lib.ui.factories.CoachFlowPageObjectFactory.get(driver);
            coach.closePdfGuideUpsellIfPresent();
            coach.closeNotificationPromptIfPresent();
            coach.closeConnectEmailPromptIfPresent();
        }
        course.openFromExplore();
    }

    private void requireEntitlement(String expected) {
        Assert.assertEquals("Wrong entitlement fixture", expected, MasterclassPageObject.required("entitlement"));
    }

    @Override protected void cleanupSessionForNextTest() {
        // Preserve the user-prepared authenticated fixture; Core still quits the driver.
        if (course != null) course.closePlayerIfPresent();
    }

    @Test public void testUnpaidCourseOpensAndReturns() {
        requireEntitlement("unpaid");
        course.assertUnpaid();
        course.backToExplore();
        openCourse();
        course.assertUnpaid();
        course.backToExplore();
    }

    @Test public void testPurchasedAccessSurvivesReopenAndRestart() {
        requireEntitlement("purchased");
        course.assertPurchased();
        course.backToExplore();
        openCourse();
        course.assertPurchased();
        course.backToExplore();
        closeAndReopenCoachApplication();
        openCourse();
        course.assertPurchased();
        course.backToExplore();
    }

    @Test public void testNextDayRemainsLockedBeforeCompletion() {
        requireEntitlement("purchased");
        course.assertNextDayLocked();
        course.backToExplore();
    }

    @Test public void testPlaybackPauseResumeAndForward() throws InterruptedException {
        requireEntitlement("purchased");
        course.assertPurchased();
        course.startPlayer();
        course.waitForPositionToAdvance(0);
        course.assertPlaybackHasSafeRemainingTime();
        course.pausePlayer();
        // Allow the native player's displayed clock to settle after pausing.
        Thread.sleep(1000);
        int paused = course.positionSeconds();
        Thread.sleep(2000);
        Assert.assertEquals("Playback continued while paused", paused, course.positionSeconds());
        course.tapPlayerControl("Go Forward 10 Seconds");
        Assert.assertEquals("Forward interval differs", paused + 10, course.positionSeconds());
        course.tapPlayerControl("Play");
        course.waitForPositionToAdvance(paused + 10);
        course.pausePlayer();
        course.tapPlayerControl("Close");
        course.assertPurchased();
        course.backToExplore();
    }
}
