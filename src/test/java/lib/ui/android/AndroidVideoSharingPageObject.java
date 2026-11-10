package lib.ui.android;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import lib.Platform;
import lib.ui.MainPageObject;
import lib.ui.VideoLessonPlayerPageObject;
import lib.ui.VideoSharingPageObject;
import org.junit.Assert;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.time.Duration;

public final class AndroidVideoSharingPageObject extends MainPageObject implements VideoSharingPageObject {
    private static final String ID = Platform.getInstance().getAndroidAppPackage() + ":id/";
    private static final String PLAYER = "id:" + ID + "viewPlayer";
    private static final String BACK = "id:" + ID + "btnNavigateUp";
    private static final String CONFIRM_EXIT = "id:" + ID + "btnYes";
    private static final String LESSON = "id:" + ID + "rvLessonDetail";
    private static final String CLOSE_LESSON = "id:" + ID + "ivButtonClose";
    private static final String TODAY = "id:" + ID + "nav_graph_daily";
    private static final String TODAY_CONTENT = "id:" + ID + "rvQuestions";
    private static final String PAYWALL = "id:" + ID + "clPaywallContainer";
    private static final String FEEDBACK = "id:" + ID + "rbRating";
    private static final String PARTNER = "id:" + ID + "btnSharePartner";
    private static final String FRIEND = "id:" + ID + "btnShareFriend";
    private static final String POPUP = "xpath://*[@resource-id='" + ID + "btnSharePartner']/..";
    private static final String CLOSE_POPUP = POPUP + "/*[@resource-id='" + ID + "btnClose']";
    private static final String SHEET_CONTENT = "//*[(@package='com.android.intentresolver' or @package='android')";
    private static final String SHARE_SHEET = "xpath:" + SHEET_CONTENT
            + " and @resource-id='android:id/chooser_header']";
    private static final String SHARE_PREVIEW = "xpath:" + SHEET_CONTENT
            + " and (@resource-id='android:id/content_preview_text' or (@resource-id='android:id/text1'"
            + " and ancestor::*[@resource-id='android:id/content_preview_container']))]";
    private final VideoLessonPlayerPageObject video;

    public AndroidVideoSharingPageObject(RemoteWebDriver driver) {
        super(driver);
        // Playback keeps the accessibility tree busy while the controls auto-hide.
        ((AndroidDriver) driver).setSetting("waitForIdleTimeout", 1000);
        video = new VideoLessonPlayerPageObject(driver);
    }

    @Override @Step("Exit Android video before the end of {lesson}")
    public void openByExitingVideo(String lesson) {
        openVideo(lesson);
        Assert.assertTrue("The video must still be playing before its end", video.positionSeconds() < video.durationSeconds());
        exitVideo();
        if (isElementVisible(LESSON) && !isElementVisible(PARTNER)) exitLesson();
        assertSharingPopup();
    }

    @Override @Step("Play Android video {lesson} through to its natural end")
    public void openByFinishingVideo(String lesson) {
        openVideo(lesson);
        int duration = video.durationSeconds();
        video.openSpeedMenu();
        video.tapAndroidSpeedOption("x2");
        createWait(duration + 45).pollingEvery(Duration.ofSeconds(2))
                .withMessage("SharingVideo did not appear after natural Android playback completion")
                .until(ignored -> isElementVisible(PARTNER));
        assertSharingPopup();
    }

    private void openVideo(String lesson) {
        try {
            video.openVideoFromToday(lesson);
        } catch (TimeoutException unavailable) {
            if (isElementVisible(PAYWALL)) {
                throw new AssertionError("Subscription blocks the configured sharing video: " + lesson, unavailable);
            }
            throw unavailable;
        }
    }

    private void exitVideo() {
        if (!isElementVisible(BACK)) video.waitUntilReady();
        waitForElementAndClick(BACK, "Cannot exit the Android video player", 10);
        waitForFirstElementPresent(new String[]{CONFIRM_EXIT, PARTNER, LESSON, TODAY_CONTENT},
                "Android video exit did not finish", 15);
        if (isElementVisible(CONFIRM_EXIT)) {
            waitForElementAndClick(CONFIRM_EXIT, "Cannot confirm Android video exit", 10);
        }
    }

    private void exitLesson() {
        waitForElementAndClick(CLOSE_LESSON, "Cannot close the Android video lesson", 10);
        waitForFirstElementPresent(new String[]{CONFIRM_EXIT, PARTNER, TODAY_CONTENT},
                "Android lesson exit did not finish", 15);
        if (isElementVisible(CONFIRM_EXIT)) {
            waitForElementAndClick(CONFIRM_EXIT, "Cannot confirm exiting the lesson", 10);
        }
        waitForElementNotVisible(LESSON, "Android video lesson remained open after exit", 10);
    }

    private void assertSharingPopup() {
        waitForElementVisible(POPUP + "/*[@resource-id='" + ID + "tvTitle' and @text='Want to share this video?']",
                "The Android SharingVideo title is missing", 15);
        Assert.assertEquals("Friend sharing action has the wrong title", "SHARE WITH A FRIEND",
                waitForElementEnabled(FRIEND, "Friend sharing action is unavailable", 10).getText());
        Assert.assertEquals("Partner sharing action has the wrong title", "SHARE WITH A PARTNER",
                waitForElementEnabled(PARTNER, "Partner sharing action is unavailable", 10).getText());
        waitForElementEnabled(CLOSE_POPUP, "SharingVideo close action is unavailable", 10);
        waitForElementNotVisible(PLAYER, "Video player still covers SharingVideo", 10);
        Assert.assertFalse("The exit confirmation is still visible", isElementVisible(CONFIRM_EXIT));
    }

    private void openShareSheet(String action) {
        Assert.assertTrue("Unsupported share action", "SHARE WITH A PARTNER".equals(action) || "SHARE WITH A FRIEND".equals(action));
        waitForElementAndClick("SHARE WITH A PARTNER".equals(action) ? PARTNER : FRIEND, "Cannot select " + action, 10);
        waitForElementVisible(SHARE_SHEET, "The Android system share sheet did not open", 15);
    }

    @Override @Step("Read the Android system preview for {action}")
    public String readSharePreview(String action) {
        openShareSheet(action);
        WebElement preview = waitForElementVisible(SHARE_PREVIEW, "The Android share sheet did not expose its text preview", 10);
        return getElementAccessibleName(preview).replaceAll("\\s+", " ").trim();
    }

    @Override @Step("Dismiss the Android system share sheet without selecting a target")
    public void dismissShareSheetIfPresent() {
        if (!isElementVisible(SHARE_SHEET)) return;
        driver.navigate().back();
        waitForElementNotVisible(SHARE_SHEET, "The Android share sheet remained open", 10);
    }

    @Override @Step("Close the Android SharingVideo popup")
    public void closePopupIfPresent() {
        if (!isElementVisible(PARTNER)) return;
        waitForElementAndClick(CLOSE_POPUP, "Cannot dismiss Android SharingVideo", 10);
        waitForElementNotVisible(PARTNER, "Android SharingVideo remained visible after closing", 10);
    }

    @Override public void returnFromSharingToToday() {
        dismissShareSheetIfPresent();
        if (isElementVisible(CONFIRM_EXIT) && isElementVisible(PLAYER)) {
            waitForElementAndClick(CONFIRM_EXIT, "Cannot confirm exit from the interrupted video", 10);
        } else if (isElementVisible(PLAYER) && !isElementVisible(PARTNER)) {
            exitVideo();
        }
        closePopupIfPresent();
        if (isElementVisible(FEEDBACK)) {
            waitForElementAndClick("id:" + ID + "btnClose", "Cannot close lesson feedback", 10);
        }
        if (isElementVisible(LESSON)) {
            exitLesson();
            closePopupIfPresent();
        }
    }

    @Override public void restorePreparedToday() {
        waitForFirstElementPresent(new String[]{TODAY, SHARE_SHEET, PARTNER, PLAYER, LESSON, FEEDBACK, PAYWALL},
                "The prepared Android sharing profile did not load", 45);
        returnFromSharingToToday();
        Assert.assertFalse("The prepared Android account is blocked by a subscription screen", isElementVisible(PAYWALL));
        waitForElementVisible(TODAY, "Today did not open for the prepared Android sharing profile", 30);
    }
}
