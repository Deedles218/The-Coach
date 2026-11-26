package lib.ui.ios;

import io.qameta.allure.Step;
import lib.ui.MainPageObject;
import lib.ui.VideoLessonPlayerPageObject;
import lib.ui.VideoSharingPageObject;
import org.junit.Assert;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.time.Duration;

/** Native SharingVideo shown by a configured iOS video lesson. */
public final class IOSVideoSharingPageObject extends MainPageObject implements VideoSharingPageObject {
    private static final String PLAYER = "xpath://XCUIElementTypeNavigationBar[@name='The_Coach.SimplePlayerView']";
    private static final String POPUP_TITLE = "id:Want to share this video?";
    private static final String POPUP = "xpath://XCUIElementTypeOther[./XCUIElementTypeStaticText[@label='Want to share this video?']]";
    private static final String QUIT = "xpath://XCUIElementTypeButton[@label='Quit' and @visible='true']";
    private static final String CLOSE_POPUP = POPUP + "/XCUIElementTypeButton[not(./XCUIElementTypeStaticText)]";
    private static final String SHARE_SHEET = "id:ActivityListView";
    private static final String SHARE_PREVIEW = "id:LP.CaptionBar.BottomCaption";
    private static final String DISMISS_SHARE_SHEET = "id:PopoverDismissRegion";
    private static final String SHARE_SHEET_HEADER = "xpath://XCUIElementTypeNavigationBar[@name='UIActivityContentView']";
    private static final String HEADER_SHARE_PREVIEW = SHARE_SHEET_HEADER + "/XCUIElementTypeOther[normalize-space(@label) != '']";
    private static final String HEADER_SHARE_CLOSE = SHARE_SHEET_HEADER + "/XCUIElementTypeButton[@name='Close']";
    private static final String SUBSCRIPTION = "xpath://XCUIElementTypeButton[starts-with(@name,'One Year Plan,')]";
    private static final String TODAY = "xpath://XCUIElementTypeButton[@name='Today' and @visible='true']";
    private static final String LESSON_CLOSE = "xpath://XCUIElementTypeButton[@name='ic outline close' and @visible='true']";
    private final VideoLessonPlayerPageObject video;

    public IOSVideoSharingPageObject(RemoteWebDriver driver) {
        super(driver);
        video = new VideoLessonPlayerPageObject(driver);
    }

    @Step("Exit the video player before the end of {lesson}")
    public void openByExitingVideo(String lesson) {
        openVideo(lesson);
        Assert.assertTrue("The video must still be playing before its end", video.positionSeconds() < video.durationSeconds());
        exitVideo(QUIT, POPUP_TITLE);
        assertSharingPopup();
    }

    private void exitVideo(String... destinations) {
        waitForElementAndClick("id:nav bar back round black", "Cannot exit the video player", 10);
        waitForFirstElementPresent(destinations, "Video exit did not lead to the expected screen", 15);
        if (isElementVisible(QUIT)) {
            waitForElementAndClick(QUIT, "Cannot confirm video exit", 10);
        }
    }

    @Override @Step("Exit the unconfigured iOS video {lesson} without SharingVideo")
    public void assertNoSharingPopupAfterVideoExit(String lesson) {
        openVideo(lesson);
        Assert.assertTrue("Exit must occur before natural completion", video.positionSeconds() < video.durationSeconds());
        exitVideo(QUIT, LESSON_CLOSE, TODAY, POPUP_TITLE);
        waitForElementNotVisible(PLAYER, "The unconfigured video player is still open", 15);
        waitForFirstElementPresent(new String[]{LESSON_CLOSE, TODAY}, "The video exit did not return to the app", 15);
        long observeUntil = System.nanoTime() + Duration.ofSeconds(4).toNanos();
        createWait(5).until(ignored -> {
            Assert.assertFalse("SharingVideo appeared after exiting the unconfigured iOS video", isElementVisible(POPUP_TITLE));
            return System.nanoTime() >= observeUntil;
        });
    }

    @Step("Play {lesson} through to its natural end")
    public void openByFinishingVideo(String lesson) {
        openVideo(lesson);
        // Play through at a supported speed without seeking to the end or closing the player.
        video.selectSpeed("x2");
        createWait(video.durationSeconds() / 2 + 45).pollingEvery(Duration.ofSeconds(2))
                .withMessage("SharingVideo did not appear after natural playback completion")
                .until(ignored -> isElementVisible(POPUP_TITLE));
        assertSharingPopup();
    }

    private void openVideo(String lesson) {
        try {
            video.openVideoFromToday(lesson);
        } catch (TimeoutException videoUnavailable) {
            if (isElementVisible(SUBSCRIPTION)) {
                throw new AssertionError("Subscription blocks the configured sharing video: " + lesson, videoUnavailable);
            }
            throw videoUnavailable;
        }
        video.assertCurrentLesson(lesson);
    }

    private void assertSharingPopup() {
        waitForElementVisible(POPUP_TITLE, "The SharingVideo title is missing", 15);
        waitForElementEnabled(button("SHARE WITH A FRIEND"), "Friend sharing action is unavailable", 10);
        waitForElementEnabled(button("SHARE WITH A PARTNER"), "Partner sharing action is unavailable", 10);
        waitForElementNotVisible(PLAYER, "Video player still covers SharingVideo", 10);
        Assert.assertFalse("The quit confirmation is still visible", isElementVisible(QUIT));
    }

    private String button(String title) {
        return POPUP + "/XCUIElementTypeButton[./XCUIElementTypeStaticText[@label='" + title + "']]";
    }

    private void openShareSheet(String action) {
        waitForElementAndClick(button(action), "Cannot select " + action, 10);
        waitForElementVisible(SHARE_SHEET, "The native share sheet did not open", 15);
    }

    @Override @Step("Read the system preview for {action}")
    public String readSharePreview(String action) {
        openShareSheet(action);
        return getElementAccessibleName(waitForFirstElementPresent(new String[]{SHARE_PREVIEW, HEADER_SHARE_PREVIEW},
                "The system preview did not show the sharing content", 10)).replaceAll("\\s+", " ").trim();
    }

    @Step("Dismiss the native share sheet")
    public void dismissShareSheetIfPresent() {
        if (!isElementVisible(SHARE_SHEET)) return;
        waitForFirstElementAndClick(new String[]{DISMISS_SHARE_SHEET, HEADER_SHARE_CLOSE},
                "Cannot dismiss the native share sheet", 10);
        waitForElementNotVisible(SHARE_SHEET, "The native share sheet remained open", 10);
    }

    @Step("Close the native SharingVideo popup")
    public void closePopupIfPresent() {
        if (!isElementVisible(POPUP_TITLE)) return;
        waitForElementAndClick(CLOSE_POPUP, "Cannot dismiss SharingVideo", 10);
        waitForElementNotVisible(POPUP_TITLE, "SharingVideo remained visible after closing", 10);
    }

    public void returnFromSharingToToday() {
        dismissShareSheetIfPresent();
        closePopupIfPresent();
        if (isElementVisible(SUBSCRIPTION)) new iOSOnboardingPageObject(driver).closePaywallsAndPopups();
        if (isElementVisible(LESSON_CLOSE)) {
            new iOSDailyPlanPageObject(driver).returnFromDailyLessonToDailyPlan();
        }
    }

    public void restorePreparedToday() {
        waitForFirstElementPresent(new String[]{TODAY, SUBSCRIPTION, SHARE_SHEET, POPUP_TITLE, LESSON_CLOSE,
                        iOSCoachFlowPageObject.PARTNER_PROMO_TITLE},
                "The prepared sharing profile did not load", 45);
        returnFromSharingToToday();
        iOSCoachFlowPageObject coach = new iOSCoachFlowPageObject(driver);
        createWait(30).withMessage("Today did not open for the prepared sharing profile").until(ignored -> {
            coach.closePartnerPromoIfPresent();
            return isElementVisible(TODAY);
        });
    }
}
