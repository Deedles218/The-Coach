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
        waitForElementAndClick("id:nav bar back round black", "Cannot exit the video player", 10);
        waitForFirstElementPresent(new String[]{QUIT, POPUP_TITLE}, "Video exit did not lead to SharingVideo", 15);
        if (isElementVisible(QUIT)) {
            waitForElementAndClick(QUIT, "Cannot confirm video exit", 10);
        }
        assertSharingPopup();
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
        return getElementAccessibleName(waitForElementVisible(SHARE_PREVIEW,
                "The system preview did not show the sharing content", 10)).replaceAll("\\s+", " ").trim();
    }

    @Step("Dismiss the native share sheet")
    public void dismissShareSheetIfPresent() {
        if (!isElementVisible(SHARE_SHEET)) return;
        waitForElementAndClick(DISMISS_SHARE_SHEET, "Cannot dismiss the native share sheet", 10);
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
        waitForFirstElementPresent(new String[]{TODAY, SUBSCRIPTION, SHARE_SHEET, POPUP_TITLE, LESSON_CLOSE},
                "The prepared sharing profile did not load", 45);
        returnFromSharingToToday();
        waitForElementVisible(TODAY, "Today did not open for the prepared sharing profile", 30);
    }
}
