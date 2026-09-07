package tests;

import lib.SimulatorProgramLoadingEvidence;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Offline negative controls for the visually verified Today skeleton geometry and lifecycle. */
public class ProgramLoadingEvidenceUnitTests {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void contentSkeletonContentIsRecordedInOrder() throws Exception {
        Path frames = write(content(126), skeleton(126), content(253));
        SimulatorProgramLoadingEvidence.assertLoaderAppearedAndDisappeared(frames);
        String evidence = new String(Files.readAllBytes(frames.resolve("loading-evidence.txt")), StandardCharsets.UTF_8);
        Assert.assertTrue(evidence.contains("frame-00001.png CONTENT"));
        Assert.assertTrue(evidence.contains("frame-00002.png SKELETON"));
        Assert.assertTrue(evidence.contains("frame-00003.png CONTENT"));
    }

    @Test
    public void viewportScalingPreservesDetection() throws Exception {
        SimulatorProgramLoadingEvidence.assertLoaderAppearedAndDisappeared(write(
                scaled(content(126)), scaled(skeleton(126)), scaled(content(253))));
    }

    @Test
    public void skeletonShimmerAndCompressionTolerancePreservesDetection() throws Exception {
        BufferedImage loading = skeleton(126);
        Graphics2D g = loading.createGraphics();
        g.setColor(new Color(111, 114, 113));
        g.fillRect(30, 72, 12, 16);
        g.fillRect(90, 138, 55, 77);
        g.dispose();
        SimulatorProgramLoadingEvidence.assertLoaderAppearedAndDisappeared(write(content(126), loading, content(253)));
    }

    @Test
    public void unchangedTodayDoesNotPassAsLoading() throws Exception {
        reject("No paired Today", content(126), content(126), content(126));
    }

    @Test
    public void selectorBackdropDimmingAloneDoesNotPassAsLoading() throws Exception {
        reject("No paired Today", content(253), content(126), content(253));
    }

    @Test
    public void unchangedSkeletonDoesNotProveAppearance() throws Exception {
        reject("before the loading skeleton", skeleton(126), skeleton(126), skeleton(126));
    }

    @Test
    public void recordingThatStartsLoadingDoesNotProveAppearance() throws Exception {
        reject("before the loading skeleton", skeleton(126), skeleton(126), content(253));
    }

    @Test
    public void skeletonThatNeverDisappearsFails() throws Exception {
        reject("did not disappear", content(126), skeleton(126), skeleton(126));
    }

    @Test
    public void blankAfterSkeletonDoesNotStandInForLoadedContent() throws Exception {
        reject("did not disappear", content(126), skeleton(126), background(253));
    }

    @Test
    public void contentFollowedByUnfinishedBlankTransitionFails() throws Exception {
        reject("must finish with Today content", content(126), skeleton(126), content(253), background(253));
    }

    @Test
    public void modulePlaceholderWithoutTitlePlaceholderDoesNotPass() throws Exception {
        BufferedImage moduleOnly = content(126);
        Graphics2D g = moduleOnly.createGraphics();
        g.setColor(new Color(118, 118, 118));
        g.fillRoundRect(16, 129, 358, 92, 12, 12);
        g.dispose();
        reject("No paired Today", content(126), moduleOnly, content(253));
    }

    @Test
    public void coloredContentCardsAreNotNeutralSkeletons() throws Exception {
        BufferedImage colored = skeleton(126);
        Graphics2D g = colored.createGraphics();
        g.setColor(new Color(130, 116, 108));
        g.fillRoundRect(16, 129, 358, 92, 12, 12);
        g.fillRoundRect(16, 68, 61, 23, 12, 12);
        g.dispose();
        reject("No paired Today", content(126), colored, content(253));
    }

    private void reject(String message, BufferedImage... images) throws Exception {
        try {
            SimulatorProgramLoadingEvidence.assertLoaderAppearedAndDisappeared(write(images));
        } catch (AssertionError error) {
            Assert.assertTrue(error.getMessage(), error.getMessage().contains(message));
            return;
        }
        Assert.fail("Invalid loading evidence passed");
    }

    private Path write(BufferedImage... images) throws Exception {
        Path directory = temporary.newFolder().toPath();
        for (int i = 0; i < images.length; i++) {
            ImageIO.write(images[i], "png", directory.resolve(String.format("frame-%05d.png", i + 1)).toFile());
        }
        return directory;
    }

    private BufferedImage background(int brightness) {
        BufferedImage image = new BufferedImage(390, 848, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(brightness, brightness, brightness));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
        return image;
    }

    private BufferedImage content(int brightness) {
        BufferedImage image = background(brightness);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(26, 28, 28));
        // Separated strokes model the nonuniform title and module/stage text without font dependencies.
        for (int x = 24; x < 70; x += 8) g.fillRect(x, 74, 3, 12);
        for (int x = 26; x < 210; x += 8) g.fillRect(x, 150, 4, 6);
        for (int x = 163; x < 230; x += 8) g.fillRect(x, 190, 4, 6);
        g.dispose();
        return image;
    }

    private BufferedImage skeleton(int brightness) {
        BufferedImage image = background(brightness);
        Graphics2D g = image.createGraphics();
        int fill = brightness - (brightness > 200 ? 18 : 8);
        g.setColor(new Color(fill, fill, fill));
        g.fillRoundRect(16, 68, 61, 23, 12, 12);
        g.fillRoundRect(16, 129, 358, 92, 12, 12);
        g.dispose();
        return image;
    }

    private BufferedImage scaled(BufferedImage source) {
        BufferedImage image = new BufferedImage(780, 1696, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(source, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();
        return image;
    }
}
