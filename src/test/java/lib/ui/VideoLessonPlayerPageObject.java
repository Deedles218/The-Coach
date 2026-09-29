package lib.ui;

import lib.Platform;
import lib.ui.factories.DailyPlanPageObjectFactory;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** COA-9551 video lesson player, separate from the Kegel workout player. */
public final class VideoLessonPlayerPageObject extends MainPageObject {
    private static final String ANDROID_ID = "com.vamapps.thecoach:id/";
    private static final List<String> SPEEDS = Arrays.asList("x0.5", "x0.75", "x1", "x1.25", "x1.5", "x2");
    private final boolean android;
    private final DailyPlanPageObject today;

    public VideoLessonPlayerPageObject(RemoteWebDriver driver) {
        super(driver);
        android = Platform.getInstance().isAndroid();
        today = DailyPlanPageObjectFactory.get(driver);
    }

    public void openVideoFromToday(String lessonTitle) {
        Assert.assertNotNull("Configure a video lesson title", lessonTitle);
        Assert.assertFalse("Lesson title cannot contain a quote in this fixture", lessonTitle.contains("'"));
        if (android && isElementVisible("id:" + ANDROID_ID + "rbRating")) {
            waitForElementAndClick("id:" + ANDROID_ID + "btnClose",
                    "Cannot leave prior lesson feedback", 10);
        }
        if (android && isElementVisible("id:" + ANDROID_ID + "btnYes"))
            waitForElementAndClick("id:" + ANDROID_ID + "btnYes", "Cannot leave prior video", 10);
        else if (android && isElementVisible("id:" + ANDROID_ID + "viewPlayer"))
            closePlayer();
        if (!android && isElementVisible("xpath://XCUIElementTypeNavigationBar[@name='The_Coach.SimplePlayerView']"))
            closePlayer();
        returnToTodayFromLesson();
        today.openTodayTab();
        String card = android
                ? "xpath://*[@resource-id='" + ANDROID_ID + "tvLessonName' and @text='" + lessonTitle + "']"
                : "xpath://XCUIElementTypeOther[@name='DailyPlanItem']"
                    + "//XCUIElementTypeStaticText[@name='" + lessonTitle + "']";
        for (int n = 0; n < 8 && !isElementVisible(card); n++) today.mobileSwipeUp();
        waitForElementAndClick(card, "Configured video lesson is absent from Today: " + lessonTitle, 10);
        if (!android) {
            waitForFirstElementPresent(new String[]{
                    "xpath://XCUIElementTypeNavigationBar[@name='The_Coach.SimplePlayerView']",
                    "id:LessonVideoView"}, "Lesson does not contain a video", 20);
            if (!isElementVisible("xpath://XCUIElementTypeNavigationBar[@name='The_Coach.SimplePlayerView']")) {
                waitForElementAndClick("id:LessonVideoView", "Cannot open lesson video", 10);
            }
        } else {
            // Some Android lessons open an article first and expose the video
            // as a dedicated card; do not click an unrelated practice player.
            waitForFirstElementPresent(new String[]{"id:" + ANDROID_ID + "viewPlayer",
                            "id:" + ANDROID_ID + "vPlayer"},
                    "Configured Android lesson has no video player", 20);
            if (!isElementVisible("id:" + ANDROID_ID + "viewPlayer")) {
                waitForElementAndClick("id:" + ANDROID_ID + "vPlayer",
                        "Configured Android lesson has no video player card", 10);
            }
        }
        waitUntilReady();
    }

    public void waitUntilReady() {
        showControls();
        waitForElementVisible(android ? "id:" + ANDROID_ID + "exo_duration"
                        : "xpath://XCUIElementTypeNavigationBar[@name='The_Coach.SimplePlayerView']",
                "Video lesson player did not open", 20);
        createWait(20).withMessage("Video duration was not loaded").until(d -> durationSeconds() > 0);
    }

    public String currentSpeed() {
        showControls();
        if (android) {
            return waitForElementVisible("id:" + ANDROID_ID + "tv_playback_speed",
                    "Current speed is not displayed", 10).getText().trim().toLowerCase();
        }
        for (WebElement button : driver.findElements(By.xpath("//XCUIElementTypeButton[@visible='true' and starts-with(@name,'x')]"))) {
            String name = button.getAttribute("name");
            if (name != null && name.toLowerCase().matches("x[0-9]+(?:\\.[0-9]+)?"))
                return name.toLowerCase();
        }
        throw new AssertionError("Current iOS video speed is not accessible");
    }

    public void openSpeedMenu() {
        showControls();
        if (android) {
            waitForElementAndClick("id:" + ANDROID_ID + "tv_playback_speed", "Cannot open speed menu", 10);
        }
    }

    public Set<String> speedOptions() {
        Set<String> options = new LinkedHashSet<String>();
        if (!android) {
            String initial = currentSpeed();
            options.add(initial);
            for (int n = 0; n < 8; n++) {
                advanceIosSpeed();
                String next = currentSpeed();
                if (initial.equals(next)) return options;
                options.add(next);
            }
            throw new AssertionError("iOS speed control did not complete a cycle within eight taps");
        }
        By locator = By.id(ANDROID_ID + "title");
        createWait(10).withMessage("Speed menu did not expose choices").until(d -> {
            for (WebElement item : driver.findElements(locator)) {
                if (!item.isDisplayed()) continue;
                String value = getElementAccessibleName(item).trim().toLowerCase();
                if (value.matches("x[0-9]+(?:\\.[0-9]+)?")) options.add(value);
            }
            return options.size() >= SPEEDS.size();
        });
        return options;
    }

    public void selectSpeed(String speed) {
        Assert.assertTrue("Unsupported speed in fixture", SPEEDS.contains(speed));
        if (!android) {
            for (int n = 0; n < 8 && !speed.equals(currentSpeed()); n++) advanceIosSpeed();
            Assert.assertEquals("Selected speed was not applied", speed, currentSpeed());
            return;
        }
        String locator = "xpath://android.widget.TextView[@resource-id='" + ANDROID_ID
                + "title' and @text='" + speed + "']";
        waitForElementAndClick(locator, "Speed choice is absent: " + speed, 10);
        createWait(10).withMessage("Selected speed was not applied: " + speed)
                .until(d -> speed.equals(currentSpeed()));
    }

    private void advanceIosSpeed() {
        String before = currentSpeed();
        waitForElementAndClick("xpath://XCUIElementTypeButton[@name='" + before + "' and @visible='true']",
                "Cannot cycle iOS video speed", 10);
        createWait(10).withMessage("iOS speed did not change from " + before)
                .until(d -> !before.equals(currentSpeed()));
    }

    public static int parseClock(String value) {
        String[] parts = value.trim().split(":");
        if (parts.length < 2 || parts.length > 3) throw new AssertionError("Invalid player clock: " + value);
        int seconds = 0;
        for (String part : parts) seconds = seconds * 60 + Integer.parseInt(part);
        return seconds;
    }

    public int durationSeconds() { return clockValues()[1]; }
    public int positionSeconds() { return clockValues()[0]; }

    private int[] clockValues() {
        if (android) {
            return createWait(15).withMessage("Android player clocks are not accessible").until(d -> {
                showControls();
                try {
                    String current = driver.findElement(By.id(ANDROID_ID + "exo_position")).getText();
                    String total = driver.findElement(By.id(ANDROID_ID + "exo_duration")).getText();
                    return new int[]{parseClock(current), parseClock(total)};
                } catch (StaleElementReferenceException staleControls) {
                    return null;
                }
            });
        }
        return createWait(15).withMessage("iOS player clocks changed while reading").until(d -> {
            int[] clocks = iosClockSnapshot();
            if (clocks != null) return clocks;
            org.openqa.selenium.Dimension screen = driver.manage().window().getSize();
            Map<String,Object> args = new HashMap<String,Object>();
            args.put("x", screen.width / 2);
            args.put("y", screen.height / 2);
            ((JavascriptExecutor)driver).executeScript("mobile: tap", args);
            return iosClockSnapshot();
        });
    }

    private int[] iosClockSnapshot() {
        List<int[]> clocks = iosClockLabels();
        if (clocks.size() < 2) return null;
        return new int[]{clocks.get(0)[4], clocks.get(clocks.size() - 1)[4]};
    }

    private List<int[]> iosClockLabels() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            NodeList labels = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(driver.getPageSource())))
                    .getElementsByTagName("XCUIElementTypeStaticText");
            List<int[]> clocks = new ArrayList<int[]>();
            int screenHeight = driver.manage().window().getSize().height;
            for (int i = 0; i < labels.getLength(); i++) {
                Node label = labels.item(i);
                String name = label.getAttributes().getNamedItem("name") == null ? ""
                        : label.getAttributes().getNamedItem("name").getNodeValue();
                Node visible = label.getAttributes().getNamedItem("visible");
                Node x = label.getAttributes().getNamedItem("x");
                Node y = label.getAttributes().getNamedItem("y");
                Node width = label.getAttributes().getNamedItem("width");
                Node height = label.getAttributes().getNamedItem("height");
                if (visible != null && "true".equals(visible.getNodeValue()) && x != null
                        && y != null && width != null && height != null
                        && name.matches("[0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?")) {
                    int top = Integer.parseInt(y.getNodeValue());
                    int labelHeight = Integer.parseInt(height.getNodeValue());
                    if (top + labelHeight >= screenHeight - 30) continue;
                    clocks.add(new int[]{Integer.parseInt(x.getNodeValue()), top,
                            Integer.parseInt(width.getNodeValue()), labelHeight, parseClock(name)});
                }
            }
            clocks.sort((a, b) -> Integer.compare(a[0], b[0]));
            return clocks;
        } catch (Exception invalidSource) {
            throw new IllegalStateException("Cannot parse iOS player accessibility snapshot", invalidSource);
        }
    }

    public void pause() {
        showControls();
        if (android) {
            waitForElementAndClick("id:" + ANDROID_ID + "exo_play_pause", "Cannot pause video", 10);
            createWait(10).until(d -> "Play".equalsIgnoreCase(driver.findElement(
                    By.id(ANDROID_ID + "exo_play_pause")).getAttribute("content-desc")));
        } else {
            waitForElementAndClick("id:PauseVideoButtonIcon", "Cannot pause video", 10);
            waitForElementVisible("id:PlayVideoButtonIcon", "Video did not enter paused state", 10);
        }
    }

    public void ensurePaused() {
        showControls();
        if (android) {
            if (!"Play".equalsIgnoreCase(driver.findElement(
                    By.id(ANDROID_ID + "exo_play_pause")).getAttribute("content-desc"))) pause();
        } else if (!isElementVisible("id:PlayVideoButtonIcon")) {
            pause();
        }
    }

    public void assertCurrentLesson(String lessonTitle) {
        if (android) return;
        Assert.assertTrue("Expected video lesson is no longer open: " + lessonTitle,
                isElementVisible("xpath://XCUIElementTypeStaticText[@name='TitleBlock.Title'"
                        + " and @label='" + lessonTitle + "' and @visible='true']"));
    }

    public void seekToFraction(double fraction) {
        Assert.assertTrue("Seek fraction must be inside video", fraction >= 0 && fraction <= 1);
        Rectangle track = seekTrack();
        Map<String,Object> args = new HashMap<String,Object>();
        args.put("x", seekX(track, fraction));
        args.put("y", track.y + track.height / 2);
        if (android) {
            ((JavascriptExecutor)driver).executeScript("mobile: clickGesture", args);
            return;
        }
        int duration = durationSeconds();
        for (int attempt = 0; attempt < 3; attempt++) {
            // A tap on a hidden iOS player reveals its controls; the next tap
            // on the same coordinate seeks. Read the result before retrying.
            ((JavascriptExecutor)driver).executeScript("mobile: tap", args);
            try {
                createWait(5).until(d -> atSeekTarget(positionSeconds(), duration, fraction));
                return;
            } catch (TimeoutException seekNotApplied) {
                // The tap may only have revealed a hidden control panel.
            }
        }
        Assert.fail("iOS tap seek stayed at " + positionSeconds() + "/" + duration
                + " instead of fraction " + fraction + " at " + args.get("x") + "," + args.get("y"));
    }

    public void dragSeekToFraction(double fraction) {
        Assert.assertTrue("Seek fraction must be inside video", fraction >= 0 && fraction <= 1);
        Rectangle track = seekTrack();
        int toX = seekX(track, fraction);
        int y = track.y + track.height / 2;
        Map<String,Object> args = new HashMap<String,Object>();
        if (android) {
            double current = Math.min(1, Math.max(0, (double)positionSeconds() / durationSeconds()));
            int fromX = seekX(track, current);
            args.put("startX", fromX); args.put("endX", toX);
            args.put("startY", y); args.put("endY", y); args.put("speed", 600);
            ((JavascriptExecutor)driver).executeScript("mobile: dragGesture", args);
        } else {
            int duration = durationSeconds();
            for (int attempt = 0; attempt < 3; attempt++) {
                double current = Math.min(1, Math.max(0, (double)positionSeconds() / duration));
                args.put("duration", .7);
                args.put("fromX", seekX(track, current)); args.put("toX", toX);
                args.put("fromY", y); args.put("toY", y);
                ((JavascriptExecutor)driver).executeScript("mobile: dragFromToForDuration", args);
                try {
                    createWait(5).until(d -> atSeekTarget(positionSeconds(), duration, fraction));
                    return;
                } catch (TimeoutException seekNotApplied) {
                    // Retry only when this drag merely revealed the controls.
                }
            }
            Assert.fail("iOS drag seek stayed at " + positionSeconds() + "/" + duration
                    + " instead of fraction " + fraction);
        }
    }

    private boolean atSeekTarget(int position, int duration, double fraction) {
        if (fraction == 0) return position <= 10;
        if (fraction >= .95) return position >= Math.ceil(duration * fraction
                - Math.max(6, duration * .01)) && position <= duration;
        return Math.abs(position - duration * fraction) <= Math.max(10, duration * .05);
    }

    private int seekX(Rectangle track, double fraction) {
        if (!android && fraction == 0) return track.x;
        int inset = Math.min(6, track.width / 20);
        return track.x + inset + (int)Math.round((track.width - 2 * inset) * fraction);
    }

    private Rectangle seekTrack() {
        if (android) {
            showControls();
            return waitForElementVisible("id:" + ANDROID_ID + "exo_progress",
                    "Video seek bar is absent", 10).getRect();
        }
        return createWait(15).withMessage("Visible iOS seek track is not accessible").until(d -> {
            List<int[]> times = iosClockLabels();
            if (times.size() < 2) {
                org.openqa.selenium.Dimension screen = driver.manage().window().getSize();
                Map<String,Object> reveal = new HashMap<String,Object>();
                reveal.put("x", screen.width / 2);
                reveal.put("y", screen.height / 2);
                ((JavascriptExecutor)driver).executeScript("mobile: tap", reveal);
                times = iosClockLabels();
            }
            if (times.size() < 2) return null;
            int[] left = times.get(0);
            int[] right = times.get(times.size() - 1);
            // The timeline has no accessibility element. Use both labels from
            // one UI snapshot so auto-hide cannot move one rect off-screen.
            return new Rectangle(left[0], left[1] - Math.max(8, left[3]) - 1,
                    Math.max(8, left[3]), right[0] + right[2] - left[0]);
        });
    }

    public void rewind15() {
        showControls();
        if (android) waitForElementAndClick("id:" + ANDROID_ID + "exo_rew", "Rewind 15 is absent", 10);
        else waitForElementAndClick("xpath:(//XCUIElementTypeButton[@name='RewindButtonMain' and @visible='true'])[1]",
                "Rewind 15 is absent", 10);
    }

    public void forward15() {
        showControls();
        if (android) waitForElementAndClick("id:" + ANDROID_ID + "exo_ffwd", "Forward 15 is absent", 10);
        else waitForElementAndClick("xpath:(//XCUIElementTypeButton[@name='RewindButtonMain' and @visible='true'])[2]",
                "Forward 15 is absent", 10);
    }

    public void closePlayer() {
        showControls();
        waitForElementAndClick(android ? "id:" + ANDROID_ID + "btnNavigateUp"
                        : "id:nav bar back round black", "Cannot close video player", 10);
        waitForElementNotPresent(android ? "id:" + ANDROID_ID + "exo_duration"
                        : "xpath://XCUIElementTypeNavigationBar[@name='The_Coach.SimplePlayerView']",
                "Video player remained after Back", 10);
        if (android) {
            try {
                waitForElementVisible("id:" + ANDROID_ID + "btnYes", "Video exit confirmation did not appear", 5);
            } catch (TimeoutException noConfirmation) {
                if (isElementVisible("id:" + ANDROID_ID + "rbRating")) {
                    waitForElementAndClick("id:" + ANDROID_ID + "btnClose",
                            "Cannot leave video lesson feedback", 10);
                }
                waitForElementVisible("id:" + ANDROID_ID + "nav_graph_daily",
                        "Today did not return after closing video", 10);
                return;
            }
            waitForElementAndClick("id:" + ANDROID_ID + "btnYes", "Cannot confirm leaving video", 10);
            waitForElementVisible("id:" + ANDROID_ID + "nav_graph_daily",
                    "Today did not return after confirming video exit", 10);
            return;
        }
        returnToTodayFromLesson();
    }

    private void returnToTodayFromLesson() {
        if (android) return;
        for (int step = 0; step < 6; step++) {
            if (isElementVisible("id:Quit")) {
                waitForElementAndClick("id:Quit", "Cannot confirm leaving video lesson", 10);
                waitForElementNotPresent("id:Quit", "Quit confirmation remained open", 10);
                continue;
            }
            if (isElementVisible("id:ic outline close")) {
                waitForElementAndClick("id:ic outline close", "Cannot leave video lesson", 10);
                try {
                    waitForElementVisible("id:Quit", "Quit confirmation did not appear", 5);
                } catch (TimeoutException noConfirmation) {
                    if (isElementVisible("id:Today") && !isElementVisible("id:ic outline close")) return;
                }
                continue;
            }
            if (isElementVisible("id:Today")) return;
        }
        Assert.fail("Today did not return after closing the video and lesson");
    }

    public void assertAtEndOrCompletion(int duration, String lessonTitle) {
        if (android && isElementVisible("id:" + ANDROID_ID + "rvFeedBackDetails")) return;
        if (!android && isElementVisible("id:LessonVideoView")) return;
        if (android) {
            int position = positionSeconds();
            Assert.assertTrue("Forward 15 did not reach the end of the video: " + position + "/" + duration,
                    position >= duration - Math.max(3, (int)Math.ceil(duration * .02)));
            Assert.assertTrue("Forward 15 moved beyond video duration", position <= duration);
            return;
        }
        String nextTitle = "xpath://XCUIElementTypeStaticText[@name='TitleBlock.Title' and @visible='true'"
                + " and @label!='" + lessonTitle + "']";
        createWait(10).withMessage("Forward 15 neither reached the end of " + lessonTitle
                + " nor opened the next lesson").until(d -> {
            int[] clocks = clockValues();
            if (clocks[1] == duration)
                return clocks[0] >= duration - Math.max(3, (int)Math.ceil(duration * .02))
                        && clocks[0] <= duration;
            return clocks[0] >= 0 && clocks[0] <= clocks[1] && isElementVisible(nextTitle);
        });
    }

    private void showControls() {
        if (android && !isElementVisible("id:" + ANDROID_ID + "exo_duration")) {
            for (int attempt = 0; attempt < 3 && !isElementVisible("id:" + ANDROID_ID + "exo_duration"); attempt++) {
                waitForElementAndClick("id:" + ANDROID_ID + "viewPlayer",
                        "Cannot reveal Android video controls", 10);
                try {
                    waitForElementVisible("id:" + ANDROID_ID + "exo_duration",
                            "Android video controls did not appear", 3);
                } catch (TimeoutException controlsHidden) {
                    if (attempt == 2) throw controlsHidden;
                }
            }
            waitForElementVisible("id:" + ANDROID_ID + "exo_duration",
                    "Android video controls did not appear", 10);
        } else if (!android && !isElementVisible("xpath://XCUIElementTypeButton[@visible='true' and starts-with(@name,'x')]")) {
            org.openqa.selenium.Dimension screen = driver.manage().window().getSize();
            Map<String,Object> args = new HashMap<String,Object>();
            args.put("x", screen.width / 2);
            args.put("y", screen.height / 2);
            ((JavascriptExecutor)driver).executeScript("mobile: tap", args);
            waitForElementVisible("xpath://XCUIElementTypeButton[@visible='true' and starts-with(@name,'x')]",
                    "iOS video controls did not appear", 10);
        }
    }
}
