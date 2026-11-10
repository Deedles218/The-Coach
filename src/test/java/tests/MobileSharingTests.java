package tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Allure;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.VideoSharingPageObject;
import lib.ui.android.AndroidVideoSharingPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import lib.ui.ios.IOSVideoSharingPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Epic("The Coach Daily Plan")
@Feature("SharingVideo")
public class MobileSharingTests extends CoreTestCase {
    private static final String FIRST_LESSON = "Mastering Her Orgasm";
    private static final String SECOND_LESSON = "Her Pleasure Anatomy";
    private static final String PROGRAM = "Sex Is a Skill";
    private static final String PARTNER = "SHARE WITH A PARTNER";
    private static final String FRIEND = "SHARE WITH A FRIEND";
    private static final String PARTNER_TEXT = "Hey! I’m doing the Sex Is a Skill program in The Coach app and have "
            + "already found a lot of useful advice. I decided to share one of the lessons with you. You might find it "
            + "interesting too. By the way, they have an app for women as well!";
    private static final String FRIEND_TEXT = "Hey! I started the Sex Is a Skill program in The Coach app for men. "
            + "They share a lot of useful advice about sex, like in this lesson. Check it out. "
            + "It might be interesting for you too.";
    private static final String FIRST_PARTNER_LINK = "https://vamapps.com/sharepartner_lesson1_m";
    private static final String SECOND_PARTNER_LINK = "https://vamapps.com/sharepartner_lesson2_m";
    private static final String FIRST_FRIEND_LINK = "https://vamapps.com/sharefriend_lesson1_m";
    private static final String SECOND_FRIEND_LINK = "https://vamapps.com/sharefriend_lesson2_m";
    private VideoSharingPageObject sharing;
    private DailyPlanPageObject today;
    private final boolean preparedProfile = Boolean.getBoolean("coach.sharing.preparedProfile");

    @Before @Override public void setUp() throws Exception {
        if (preparedProfile) PreparedMobileProfile.assertPreservationAllowed();
        super.setUp();
        openSharingFixture();
    }

    private void openSharingFixture() {
        requireMobilePlatform();
        Platform platform = Platform.getInstance();
        if (platform.isAndroid()) {
            Assert.assertEquals("Use the reviewed male Android sharing fixture", "com.vamapps.thecoach",
                    platform.getAndroidAppPackage());
            sharing = new AndroidVideoSharingPageObject(driver);
        } else {
            Assert.assertEquals("Use the reviewed male production sharing fixture", "com.vamapps.The-Coach",
                    platform.getIOSBundleId());
            sharing = new IOSVideoSharingPageObject(driver);
        }
        today = DailyPlanPageObjectFactory.get(driver);
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        if (preparedProfile) {
            sharing.restorePreparedToday();
        } else {
            TestData.TestAccount account = TestData.existingProgressAccount();
            sharing.returnFromSharingToToday();
            coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        }
        PreparedMobileProfile.verifyFromToday(coach);
        Assert.assertEquals("The sharing fixture must have the configured program active", PROGRAM,
                today.getActiveProgramName());
        Allure.addAttachment("Sharing fixture", PROGRAM + "; " + today.getCurrentDayLabel() + "; " + eligibleLesson());
    }

    @Test @Issue("COA-9471")
    @DisplayName("COA-9471 Share with a partner opens and dismisses the system sheet")
    public void testCoa9471PartnerOpensSystemShareSheet() {
        sharing.openByExitingVideo(eligibleLesson());
        assertSystemShareLink(PARTNER, eligiblePartnerLink());
        sharing.dismissShareSheetIfPresent();
    }

    @Test @Issue("COA-9482")
    @DisplayName("COA-9482 Each sharing action supplies its expected text and link")
    public void testCoa9482SharePayloadMatchesSelectedAction() {
        sharing.openByExitingVideo(eligibleLesson());
        Assert.assertEquals(PARTNER_TEXT + " " + eligiblePartnerLink(), sharing.readSharePreview(PARTNER));
        sharing.returnFromSharingToToday();
        sharing.openByExitingVideo(eligibleLesson());
        Assert.assertEquals(FRIEND_TEXT + " " + eligibleFriendLink(), sharing.readSharePreview(FRIEND));
        sharing.dismissShareSheetIfPresent();
    }

    @Test @Issue("COA-9481")
    @DisplayName("COA-9481 Finishing an eligible video opens SharingVideo")
    public void testCoa9481SharingPopupOnNaturalCompletion() {
        sharing.openByFinishingVideo(eligibleLesson());
    }

    @Test @Issue("COA-9483")
    @DisplayName("COA-9483 Rewatching an eligible video shows SharingVideo again")
    public void testCoa9483SharingPopupOnRepeatCompletion() {
        sharing.openByFinishingVideo(eligibleLesson());
        sharing.returnFromSharingToToday();
        sharing.openByFinishingVideo(eligibleLesson());
        sharing.returnFromSharingToToday();
        today.openTodayTab();
    }

    private void assertSystemShareLink(String action, String expectedLink) {
        Assert.assertTrue("The native share preview has the wrong lesson link",
                sharing.readSharePreview(action).endsWith(expectedLink));
    }

    // Both cases use a known eligible lesson in the first stage of the same program.
    private String eligibleLesson() {
        return Platform.getInstance().isAndroid() ? SECOND_LESSON : FIRST_LESSON;
    }

    private String eligiblePartnerLink() {
        return Platform.getInstance().isAndroid() ? SECOND_PARTNER_LINK : FIRST_PARTNER_LINK;
    }

    private String eligibleFriendLink() {
        return Platform.getInstance().isAndroid() ? SECOND_FRIEND_LINK : FIRST_FRIEND_LINK;
    }

    @Override
    protected void cleanupSessionForNextTest() {
        if (sharing != null) sharing.returnFromSharingToToday();
        if (!preparedProfile) super.cleanupSessionForNextTest();
    }
}
