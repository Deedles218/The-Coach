package lib;

import io.qameta.allure.Allure;
import org.junit.Assert;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.remote.RemoteWebDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.IOSStartScreenRecordingOptions;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Observes the sheet's drag handle in recorded intermediate frames. */
public final class SimulatorModalMotion implements AutoCloseable {
    private final Path folder;
    private final Process recorder;
    private final IOSDriver deviceDriver;
    private final Map<String, Object> originalRecordingSettings = new HashMap<>();
    private boolean stopped;

    public SimulatorModalMotion(String caseKey) throws Exception {
        this(caseKey, null);
    }

    public SimulatorModalMotion(String caseKey, RemoteWebDriver driver) throws Exception {
        String udid = SimulatorTestIdentity.deviceUdid();
        Assert.assertNotNull("Motion evidence needs an explicit iOS UDID", udid);
        folder = Files.createTempDirectory(Paths.get("target"), caseKey + "-motion-");
        if (SimulatorTestIdentity.isPhysicalDevice()) {
            Assert.assertTrue("Physical motion evidence requires the current iOS driver", driver instanceof IOSDriver);
            deviceDriver = (IOSDriver) driver;
            recorder = null;
            Map<String, Object> settings = deviceDriver.getSettings();
            for (String key : Arrays.asList("mjpegServerFramerate", "mjpegServerScreenshotQuality")) {
                if (settings.containsKey(key)) originalRecordingSettings.put(key, settings.get(key));
            }
            try {
                deviceDriver.startRecordingScreen(new IOSStartScreenRecordingOptions()
                        .withFps(30).withTimeLimit(java.time.Duration.ofMinutes(3)));
            } catch (Exception | Error failure) {
                terminateAfterFailure(failure);
                throw failure;
            }
            return;
        }
        deviceDriver = null;
        recorder = new ProcessBuilder("python3", "scripts/record_simulator_motion.py", "--udid", udid,
                "--output", folder.toString()).redirectError(folder.resolve("recorder.log").toFile()).start();
        ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            Future<String> ready = reader.submit(() -> new BufferedReader(new InputStreamReader(recorder.getInputStream())).readLine());
            Assert.assertEquals("Motion recorder failed to arm", "READY", ready.get(20, TimeUnit.SECONDS));
        } catch (Throwable error) {
            terminateAfterFailure(error);
            throw error;
        } finally { reader.shutdownNow(); }
    }

    private void stop() throws Exception {
        if (stopped) return;
        try {
            if (deviceDriver != null) {
                String encoded = deviceDriver.stopRecordingScreen();
                stopped = true;
                restoreRecordingSettings();
                Assert.assertTrue("Physical device returned no motion recording", encoded != null && !encoded.isEmpty());
                Path video = folder.resolve("motion.mp4");
                Files.write(video, Base64.getDecoder().decode(encoded));
                try (InputStream in = Files.newInputStream(video)) {
                    Allure.addAttachment("Selector motion on physical iPhone", "video/mp4", in, "mp4");
                }
                Process extraction = new ProcessBuilder("ffmpeg", "-v", "error", "-i", video.toString(),
                        "-vf", "fps=30,scale=390:-1", folder.resolve("frame-%05d.png").toString())
                        .redirectErrorStream(true).redirectOutput(folder.resolve("recorder.log").toFile()).start();
                try {
                    Assert.assertTrue("Physical recording frame extraction timed out", extraction.waitFor(30, TimeUnit.SECONDS));
                    Assert.assertEquals("Frame extraction failed; see " + folder, 0, extraction.exitValue());
                } finally {
                    if (extraction.isAlive()) {
                        extraction.destroyForcibly();
                        extraction.waitFor(5, TimeUnit.SECONDS);
                    }
                }
                return;
            }
            recorder.getOutputStream().close();
            Assert.assertTrue("Recorder did not finish", recorder.waitFor(50, TimeUnit.SECONDS));
            stopped = true;
            Path video = folder.resolve("motion.mp4");
            if (Files.exists(video)) try (InputStream in = Files.newInputStream(video)) {
                Allure.addAttachment("Selector motion", "video/mp4", in, "mp4");
            }
            Assert.assertEquals("Frame extraction failed; see " + folder, 0, recorder.exitValue());
        } catch (Exception | Error error) {
            terminateAfterFailure(error);
            throw error;
        }
    }

    private void terminateAfterFailure(Throwable primary) {
        if (deviceDriver != null) {
            try {
                if (!stopped) deviceDriver.stopRecordingScreen();
            } catch (Exception | Error cleanupFailure) {
                primary.addSuppressed(cleanupFailure);
            } finally {
                stopped = true;
                try { restoreRecordingSettings(); }
                catch (Exception | Error cleanupFailure) { primary.addSuppressed(cleanupFailure); }
            }
            return;
        }
        boolean[] interrupted = {Thread.interrupted()};
        try {
            // Python handles SIGTERM and reaps only its current simctl/ffmpeg group
            // within 2 + 2 seconds. Allow that cleanup before killing its parent.
            recorder.destroy();
            if (!awaitRecorderExit(6, interrupted)) {
                recorder.destroyForcibly();
                if (!awaitRecorderExit(2, interrupted)) {
                    throw new IOException("Recorder did not terminate after SIGKILL; see " + folder);
                }
            }
        } catch (Exception | Error cleanupFailure) {
            if (primary != cleanupFailure) primary.addSuppressed(cleanupFailure);
        } finally {
            stopped = true;
            if (interrupted[0] || primary instanceof InterruptedException) Thread.currentThread().interrupt();
        }
    }

    private void restoreRecordingSettings() {
        if (!originalRecordingSettings.isEmpty()) deviceDriver.setSettings(originalRecordingSettings);
    }

    private boolean awaitRecorderExit(int timeoutSeconds, boolean[] interrupted) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) return !recorder.isAlive();
            try {
                return recorder.waitFor(remaining, TimeUnit.NANOSECONDS);
            } catch (InterruptedException interruption) {
                // Finish the bounded child cleanup, then restore the caller's interrupt.
                interrupted[0] = true;
            }
        }
    }

    /** Shared recorder for observations that need transient rendered UI states. */
    public Path finishRecording() throws Exception {
        stop();
        return folder;
    }

    public void assertVerticalMotion(Rectangle card, int screenWidth, boolean opening) throws Exception {
        stop();
        File[] frames = folder.toFile().listFiles((dir, name) -> name.startsWith("frame-") && name.endsWith(".png"));
        Assert.assertNotNull("No recorded frames", frames);
        Arrays.sort(frames);
        List<Integer> positions = new ArrayList<Integer>();
        for (File frame : frames) {
            BufferedImage im = ImageIO.read(frame);
            double scale = (double) im.getWidth() / screenWidth;
            int y = findSheetHandle(im, (int)((card.y - 70) * scale));
            if (y >= 0 && (positions.isEmpty() || y != positions.get(positions.size() - 1))) positions.add(y);
        }
        Allure.addAttachment("Sheet handle positions in video (pixels)", positions.toString());
        assertMotionPositions(positions, opening);
    }

    public static void assertMotionPositions(List<Integer> positions, boolean opening) {
        int transitions = 0;
        for (int i = 1; i < positions.size(); i++) {
            int delta = positions.get(i) - positions.get(i - 1);
            if (opening ? delta < -1 : delta > 1) transitions++;
        }
        Assert.assertTrue("No observable intermediate " + (opening ? "upward" : "downward")
                + " sheet animation; handle positions=" + positions, transitions >= 2);
        int net = positions.get(positions.size() - 1) - positions.get(0);
        Assert.assertTrue("Handle did not travel in the expected net direction: " + positions,
                opening ? net < -8 : net > 8);
    }

    private static boolean orange(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return r >= 190 && g >= 55 && g <= 190 && b <= 115 && r > g * 1.2 && g > b * 1.2;
    }

    private static boolean accent(int rgb, boolean forHer) {
        if (!forHer) return orange(rgb);
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return r >= 190 && g >= 75 && g <= 170 && b >= 65 && b <= 160
                && r - g >= 65 && Math.abs(g - b) <= 35;
    }

    public static int findSheetHandle(BufferedImage im, int minimumY) {
        int center = im.getWidth() / 2;
        int half = (int)(im.getWidth() * .04);
        int outside = (int)(im.getWidth() * .10);
        for (int y = Math.max(6, minimumY); y < im.getHeight() - 6; y++) {
            int gray = 0;
            for (int x = center - half; x <= center + half; x++) {
                int rgb = im.getRGB(x, y);
                int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                if (r >= 170 && r <= 225 && Math.abs(r - g) < 10 && Math.abs(g - b) < 10) gray++;
            }
            if (gray < (2 * half + 1) * .85) continue;
            // The short gray handle has white sheet surface on all sides.
            // A dimmed backdrop or gray text cannot satisfy this geometry.
            if (white(im.getRGB(center - outside, y)) && white(im.getRGB(center + outside, y))
                    && white(im.getRGB(center, y - 6)) && white(im.getRGB(center, y + 6))) return y;
        }
        return -1;
    }

    private static boolean white(int rgb) {
        return ((rgb >> 16) & 255) > 235 && ((rgb >> 8) & 255) > 235 && (rgb & 255) > 235;
    }

    public static void assertOrangeOutline(byte[] screenshot, Rectangle card, int screenWidth) throws Exception {
        assertAccentOutline(screenshot, card, screenWidth,
                Platform.getInstance().getIOSBundleId().endsWith("-for-her"));
    }

    public static void assertAccentOutline(byte[] screenshot, Rectangle card, int screenWidth,
                                          boolean forHer) throws Exception {
        BufferedImage im = ImageIO.read(new ByteArrayInputStream(screenshot));
        double scale = (double)im.getWidth() / screenWidth;
        try (InputStream in = new ByteArrayInputStream(screenshot)) {
            Allure.addAttachment("Active program orange outline", "image/png", in, "png");
        }
        // Sample each edge, excluding rounded corners. Search only a narrow
        // strip around the accessibility bounds: the cell includes 16-point padding.
        for (int edge = 0; edge < 4; edge++) {
            int colored = 0;
            for (int i = 0; i < 50; i++) {
                double t = .15 + .7 * i / 49;
                double x = edge < 2 ? card.x + card.width * t : card.x + (edge == 2 ? 0 : card.width);
                double y = edge < 2 ? card.y + (edge == 0 ? 0 : card.height) : card.y + card.height * t;
                boolean found = false;
                for (int offset = -20; offset <= 20; offset++) {
                    int px = (int)((x + (edge >= 2 ? offset : 0)) * scale);
                    int py = (int)((y + (edge < 2 ? offset : 0)) * scale);
                    if (px >= 0 && py >= 0 && px < im.getWidth() && py < im.getHeight() && accent(im.getRGB(px, py), forHer)) found = true;
                }
                if (found) colored++;
            }
            Assert.assertTrue("Active card has no continuous " + (forHer ? "coral" : "orange")
                    + " outline on edge " + edge + ": " + colored + "/50", colored >= 40);
        }
    }

    @Override public void close() throws Exception { stop(); }
}
