package tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import lib.CoreTestCase;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.VideoLessonPlayerPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;

@Epic("Lessons")
@Feature("COA-9551 video player")
public class VideoPlayerFeatureTests extends CoreTestCase {
    private VideoLessonPlayerPageObject preparePlayer() {
        if (!Boolean.getBoolean("coach.video.useCurrentAccount")) {
            TestData.TestAccount account = TestData.testModelAccount("COA-9551");
            CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
            coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        }
        String program = System.getProperty("coach.video.program");
        if (program != null && !program.trim().isEmpty()) {
            Assert.assertTrue("Explicit video program preparation is implemented for iOS",
                    lib.Platform.getInstance().isIOS());
            lib.ui.ios.iOSProgramSelectorPageObject today =
                    new lib.ui.ios.iOSProgramSelectorPageObject(driver);
            today.openTodayTab();
            today.ensureProgramSelected(program.trim());
            today.selectObservedDay(requiredTitle("coach.video.day"));
            today.assertCurrentDayMatches(requiredTitle("coach.video.day"));
        }
        VideoLessonPlayerPageObject player = new VideoLessonPlayerPageObject(driver);
        player.openVideoFromToday(requiredTitle("coach.video.lessonTitle"));
        player.assertCurrentLesson(playerTitle());
        io.qameta.allure.Allure.addAttachment("Video fixture", requiredTitle("coach.video.lessonTitle")
                + "; durationSeconds=" + player.durationSeconds());
        return player;
    }

    @Override
    protected void cleanupSessionForNextTest() {
        if (!Boolean.getBoolean("coach.video.useCurrentAccount")) {
            if (lib.Platform.getInstance().isIOS()) {
                new VideoLessonPlayerPageObject(driver).closeLessonFlowIfOpen();
            }
            super.cleanupSessionForNextTest();
        }
    }

    private static String requiredTitle(String name) {
        String title = System.getProperty(name);
        Assert.assertTrue("Configure -D" + name + " with a video lesson in the dedicated premium fixture",
                title != null && !title.trim().isEmpty());
        return title.trim();
    }

    private static String playerTitle() {
        return requiredTitle(System.getProperty("coach.video.playerTitle") == null
                ? "coach.video.lessonTitle" : "coach.video.playerTitle");
    }

    @Test @Issue("COA-9551")
    public void testSpeedOptionsAndPauseBackground() {
        VideoLessonPlayerPageObject player = preparePlayer();
        player.ensurePaused();
        player.seekToFraction(.2);
        player.ensurePaused();
        // Other tests may have left a non-default speed on this dedicated
        // account; reset the test's own baseline without defining the app's
        // unresolved cross-lesson/relaunch speed policy.
        if (!"x1".equals(player.currentSpeed())) {
            player.openSpeedMenu();
            player.selectSpeed("x1");
        }
        int before = player.positionSeconds();
        player.openSpeedMenu();
        Assert.assertEquals("Wrong speed menu, including possible stale x3 entry",
                new LinkedHashSet<String>(Arrays.asList("x0.5", "x0.75", "x1", "x1.25", "x1.5", "x2")),
                player.speedOptions());
        player.selectSpeed("x1.5");
        Assert.assertTrue("Speed change restarted or rewound the video", player.positionSeconds() >= before - 2);
        player.resume();
        player.pause();
        Assert.assertEquals("Speed was reset on pause", "x1.5", player.currentSpeed());
        backgroundApp(2);
        Assert.assertEquals("Speed was reset after backgrounding", "x1.5", player.currentSpeed());
        player.openSpeedMenu();
        player.selectSpeed("x1");
    }

    @Test @Issue("COA-9551")
    public void testSeekBoundsAndResumeFromSavedPosition() {
        VideoLessonPlayerPageObject player = preparePlayer();
        player.pause();
        int duration = player.durationSeconds();
        Assert.assertTrue("Fixture video must be longer than 30 seconds", duration > 30);
        player.seekToFraction(.5);
        player.createWait(20).withMessage("Seek did not move to the middle of the video")
                .until(d -> player.positionSeconds() > duration * .3 && player.positionSeconds() < duration * .7);
        int saved = player.positionSeconds();
        long savedAt = System.nanoTime();
        player.closePlayer();
        player.openVideoFromToday(requiredTitle("coach.video.lessonTitle"));
        int resumed = player.positionSeconds();
        long elapsed = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - savedAt);
        Assert.assertTrue("Lesson did not resume near its saved position: " + saved + " -> " + resumed,
                resumed >= saved - 12 && resumed <= saved + elapsed + 12);
        player.pause();
        player.dragSeekToFraction(0);
        player.createWait(10).withMessage("Dragging the timeline did not reach its start")
                .until(d -> player.positionSeconds() <= 10);
        player.rewind15();
        player.createWait(10).withMessage("−15 seconds did not stop at the start of the video")
                .until(d -> player.positionSeconds() <= 3);
        // Keep enough time for real-device accessibility round trips before
        // pressing +15, while still exercising the final 15-second boundary.
        player.ensurePaused();
        player.seekToAtLeastFraction((duration - 14.0) / duration);
        player.ensurePaused();
        player.assertCurrentLesson(playerTitle());
        Assert.assertTrue("Final-boundary fixture is not within 15 seconds of video end",
                duration - player.positionSeconds() <= 15);
        player.forward15();
        player.assertAtEndOrCompletion(duration, requiredTitle("coach.video.lessonTitle"));
    }

    @Test @Issue("COA-9551")
    public void testPositionsBelongToIndividualLessons() {
        VideoLessonPlayerPageObject player = preparePlayer();
        String first = requiredTitle("coach.video.lessonTitle");
        String second = requiredTitle("coach.video.secondLessonTitle");
        Assert.assertNotEquals("Fixture needs two different video lessons", first, second);
        player.closePlayer();
        player.openVideoFromToday(second);
        player.seekToFraction(0);
        player.closePlayer();
        player.openVideoFromToday(first);
        player.seekToFraction(.3);
        int firstPosition = player.positionSeconds();
        player.closePlayer();
        long secondOpenedAt = System.nanoTime();
        player.openVideoFromToday(second);
        int secondInitialPosition = player.positionSeconds();
        long secondOpenSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - secondOpenedAt);
        Assert.assertTrue("Second lesson reused the first lesson's position: " + secondInitialPosition,
                secondInitialPosition <= secondOpenSeconds + 12);
        player.seekToFraction(.6);
        int secondPosition = player.positionSeconds();
        player.closePlayer();
        long firstReopenedAt = System.nanoTime();
        player.openVideoFromToday(first);
        int firstResumed = player.positionSeconds();
        long firstOpenSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - firstReopenedAt);
        Assert.assertTrue("First lesson position was overwritten by second lesson",
                firstResumed >= firstPosition - 12 && firstResumed <= firstPosition + firstOpenSeconds + 12);
        player.closePlayer();
        long secondReopenedAt = System.nanoTime();
        player.openVideoFromToday(second);
        int secondResumed = player.positionSeconds();
        long secondReopenSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - secondReopenedAt);
        Assert.assertTrue("Second lesson position was overwritten by first lesson",
                secondResumed >= secondPosition - 12
                        && secondResumed <= secondPosition + secondReopenSeconds + 12);
    }

    @Test @Issue("COA-9551")
    public void testFrom95PercentReopensAtStart() {
        VideoLessonPlayerPageObject player = preparePlayer();
        int duration = player.durationSeconds();
        Assert.assertTrue("95% boundary fixture needs a video of at least three minutes", duration >= 180);
        player.ensurePaused();
        if (!"x1".equals(player.currentSpeed())) {
            player.openSpeedMenu();
            player.selectSpeed("x1");
        }
        int reached = player.seekToAtLeastFraction(.955);
        Assert.assertTrue("Seek did not reach the 95% boundary", reached >= Math.ceil(duration * .95));
        player.ensurePaused();
        int closingPosition = player.positionSeconds();
        Assert.assertTrue("95% fixture must remain paused before full video completion",
                closingPosition >= Math.ceil(duration * .95) && closingPosition < duration);
        io.qameta.allure.Allure.addAttachment("Verified 95 percent position before completion",
                requiredTitle("coach.video.lessonTitle") + "; position=" + reached
                        + "; pausedClosingPosition=" + closingPosition + "; duration=" + duration);
        if (lib.Platform.getInstance().isIOS()) player.closeLessonFlowIfOpen();
        else player.closePlayer();
        long reopenedAt = System.nanoTime();
        player.openVideoFromToday(requiredTitle("coach.video.lessonTitle"));
        player.ensurePaused();
        int resumed = player.positionSeconds();
        long reopeningSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - reopenedAt);
        player.assertCurrentLesson(playerTitle());
        io.qameta.allure.Allure.addAttachment("Position after reopening the 95 percent lesson",
                "before=" + reached + "; after=" + resumed + "; duration=" + duration
                        + "; reopeningSeconds=" + reopeningSeconds + "; playbackSpeed=x1");
        Assert.assertTrue("Reopening took too long to distinguish a reset from saved progress",
                reopeningSeconds + 3 < duration * .5);
        Assert.assertTrue("Lesson at or after 95% did not restart: " + reached + " -> " + resumed
                        + "; reopeningSeconds=" + reopeningSeconds,
                resumed <= reopeningSeconds + 3);
    }
}
