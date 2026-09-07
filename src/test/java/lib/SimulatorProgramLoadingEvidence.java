package lib;

import io.qameta.allure.Allure;
import org.junit.Assert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Observes the Today loading skeleton used by the current iOS preprod layout.
 * The title and module placeholders were verified in recorded 390 x 848 frames.
 * Coordinates scale with the viewport; this is a rendered skeleton oracle, not a spinner detector.
 */
public final class SimulatorProgramLoadingEvidence {
    private SimulatorProgramLoadingEvidence() {
    }

    public static void assertLoaderAppearedAndDisappeared(Path recordedFrames) throws Exception {
        File[] frames = recordedFrames.toFile().listFiles((folder, name) -> name.matches("frame-[0-9]+\\.png"));
        Assert.assertNotNull("Recorded loading frames are unavailable: " + recordedFrames, frames);
        Arrays.sort(frames);
        Assert.assertTrue("Loading observation needs before, loading and after frames", frames.length >= 3);
        List<FrameState> states = new ArrayList<>();
        StringBuilder detections = new StringBuilder("Today skeleton: paired title/module placeholders; "
                + "positions scale from the verified iOS 390x848 viewport.\n");
        int firstSkeleton = -1, lastSkeleton = -1;
        for (int index = 0; index < frames.length; index++) {
            BufferedImage frame = ImageIO.read(frames[index]);
            Assert.assertNotNull("Unreadable loading frame: " + frames[index], frame);
            FrameObservation observation = inspect(frame);
            states.add(observation.state);
            detections.append(frames[index].getName()).append(' ').append(observation).append('\n');
            if (observation.state == FrameState.SKELETON) {
                if (firstSkeleton < 0) firstSkeleton = index;
                lastSkeleton = index;
            }
        }
        String report = detections.toString();
        Files.write(recordedFrames.resolve("loading-evidence.txt"), report.getBytes(StandardCharsets.UTF_8));
        Allure.addAttachment("Today skeleton detections", report);
        if (firstSkeleton >= 0) attach("Today loading skeleton", frames[firstSkeleton].toPath());
        int loadedAfterSkeleton = -1;
        for (int index = lastSkeleton + 1; lastSkeleton >= 0 && index < states.size(); index++) {
            if (states.get(index) == FrameState.CONTENT) {
                loadedAfterSkeleton = index;
                attach("Today content after loading", frames[index].toPath());
                break;
            }
        }
        Assert.assertTrue("No paired Today title/module loading skeleton was recorded; see " + recordedFrames,
                firstSkeleton >= 0);
        Assert.assertTrue("No rendered Today content before the loading skeleton; recording did not observe its appearance",
                states.subList(0, firstSkeleton).contains(FrameState.CONTENT));
        Assert.assertTrue("Today loading skeleton did not disappear into rendered content", loadedAfterSkeleton >= 0);
        Assert.assertEquals("Recording must finish with Today content, not loading or a blank transition",
                FrameState.CONTENT, states.get(states.size() - 1));
    }

    private static void attach(String name, Path frame) throws Exception {
        try (InputStream input = Files.newInputStream(frame)) {
            Allure.addAttachment(name, "image/png", input, "png");
        }
    }

    private enum FrameState { CONTENT, SKELETON, OTHER }

    private static FrameObservation inspect(BufferedImage frame) {
        Assert.assertTrue("Loading evidence needs a portrait phone viewport", frame.getHeight() > frame.getWidth());
        Region title = sample(frame, 22, 73, 70, 87);
        Region titleBackground = sample(frame, 4, 73, 12, 87);
        Region module = sample(frame, 24, 139, 367, 214);
        Region moduleLeft = sample(frame, 4, 139, 12, 214);
        Region moduleRight = sample(frame, 378, 139, 386, 214);
        double titleContrast = titleBackground.median - title.median;
        double leftContrast = moduleLeft.median - module.median;
        double rightContrast = moduleRight.median - module.median;
        // The shimmer changes brightness slightly, while both filled shapes remain neutral,
        // nearly uniform, and darker than their surrounding surface on both module edges.
        boolean skeleton = title.isNeutralFill() && module.isNeutralFill()
                && titleContrast >= 4 && titleContrast <= 35
                && leftContrast >= 4 && leftContrast <= 35
                && rightContrast >= 4 && rightContrast <= 35;
        double titleInk = title.darkFraction(titleBackground.median - 25);
        double moduleInk = module.darkFraction((moduleLeft.median + moduleRight.median) / 2 - 25);
        // Disappearance must reveal text in both the title and module/stage area.
        // A blank screen or the selector's dimmed backdrop cannot stand in for loaded content.
        boolean content = !skeleton && titleBackground.median >= 80 && moduleLeft.median >= 80
                && Math.abs(leftContrast) <= 4 && Math.abs(rightContrast) <= 4
                && titleInk >= .10 && titleInk <= .80 && moduleInk >= .012 && moduleInk <= .25;
        return new FrameObservation(skeleton ? FrameState.SKELETON : content ? FrameState.CONTENT : FrameState.OTHER,
                titleContrast, leftContrast, rightContrast, titleInk, moduleInk);
    }

    private static Region sample(BufferedImage frame, int left, int top, int right, int bottom) {
        double scaleX = frame.getWidth() / 390.0, scaleY = frame.getHeight() / 848.0;
        int x0 = (int) Math.round(left * scaleX), x1 = (int) Math.round(right * scaleX);
        int y0 = (int) Math.round(top * scaleY), y1 = (int) Math.round(bottom * scaleY);
        List<Double> brightness = new ArrayList<>();
        int neutralPixels = 0;
        int step = Math.max(1, (int) Math.round(2 * Math.min(scaleX, scaleY)));
        for (int y = y0; y < y1; y += step) {
            for (int x = x0; x < x1; x += step) {
                int rgb = frame.getRGB(x, y);
                int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                brightness.add((r + g + b) / 3.0);
                if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 8) neutralPixels++;
            }
        }
        return new Region(brightness, neutralPixels);
    }

    private static final class Region {
        private final List<Double> brightness;
        private final double median;
        private final int neutralPixels;

        private Region(List<Double> brightness, int neutralPixels) {
            Assert.assertFalse("Loading sample region has no pixels", brightness.isEmpty());
            this.brightness = brightness;
            this.neutralPixels = neutralPixels;
            Double[] sorted = brightness.toArray(new Double[0]);
            Arrays.sort(sorted);
            median = sorted[sorted.length / 2];
        }

        private boolean isNeutralFill() {
            int closeToMedian = 0;
            for (double value : brightness) if (Math.abs(value - median) <= 8) closeToMedian++;
            return median >= 80 && median <= 245
                    && neutralPixels >= brightness.size() * .95 && closeToMedian >= brightness.size() * .95;
        }

        private double darkFraction(double threshold) {
            int dark = 0;
            for (double value : brightness) if (value < threshold) dark++;
            return (double) dark / brightness.size();
        }
    }

    private static final class FrameObservation {
        private final FrameState state;
        private final double titleContrast, leftContrast, rightContrast, titleInk, moduleInk;

        private FrameObservation(FrameState state, double titleContrast, double leftContrast,
                                 double rightContrast, double titleInk, double moduleInk) {
            this.state = state;
            this.titleContrast = titleContrast;
            this.leftContrast = leftContrast;
            this.rightContrast = rightContrast;
            this.titleInk = titleInk;
            this.moduleInk = moduleInk;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%s titleContrast=%.1f moduleContrast=%.1f/%.1f titleInk=%.3f moduleInk=%.3f",
                    state, titleContrast, leftContrast, rightContrast, titleInk, moduleInk);
        }
    }
}
