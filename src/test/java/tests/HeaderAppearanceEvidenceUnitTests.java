package tests;

import lib.HeaderAppearanceEvidence;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.Assert;
import org.junit.Test;

public class HeaderAppearanceEvidenceUnitTests {
    private static final int WIDTH = 800, BOTTOM = 150;

    private BufferedImage fixture(boolean rounded, boolean shadow, boolean fade) {
        BufferedImage image = new BufferedImage(WIDTH, 250, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(247, 247, 247)); g.fillRect(0, 0, WIDTH, 250);
        for (int y = BOTTOM; y < 250; y++) {
            int value = !shadow ? 255 : fade ? Math.min(255, 244 + (y - BOTTOM) * 11 / 40) : 244;
            g.setColor(new Color(value, value, value)); g.drawLine(0, y, WIDTH - 1, y);
        }
        g.setColor(Color.WHITE);
        if (rounded) g.fillRoundRect(0, -30, WIDTH, BOTTOM + 30, 50, 50);
        else g.fillRect(0, 0, WIDTH, BOTTOM);
        g.dispose(); return image;
    }

    private void check(BufferedImage image) throws Exception {
        BufferedImage reference = ImageIO.read(getClass().getResourceAsStream("/modules/coa8772-header.png"));
        HeaderAppearanceEvidence.assertMatches(
                HeaderAppearanceEvidence.measure(reference, new Rectangle(4, 0, 666, 134)),
                HeaderAppearanceEvidence.measure(image, new Rectangle(0, 0, WIDTH, BOTTOM)));
    }

    private void reject(BufferedImage image, String expectedReason) throws Exception {
        try { check(image); } catch (AssertionError error) {
            Assert.assertTrue("Wrong failure: " + error.getMessage(), error.getMessage().contains(expectedReason));
            return;
        }
        Assert.fail("Invalid header passed: " + expectedReason);
    }

    @Test public void roundedHeaderWithFadingShadowPasses() throws Exception { check(fixture(true, true, true)); }
    @Test public void squareHeaderWithShadowFails() throws Exception { reject(fixture(false, true, true), "corner"); }
    @Test public void roundedHeaderWithoutShadowFails() throws Exception { reject(fixture(true, false, true), "Missing bottom shadow"); }
    @Test public void uniformGrayBackgroundIsNotShadow() throws Exception { reject(fixture(true, true, false), "does not fade"); }
    @Test public void oneSquareCornerFails() throws Exception {
        BufferedImage image = fixture(true, true, true);
        Graphics2D g = image.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0, BOTTOM - 30, 30, 30); g.dispose();
        reject(image, "Bottom-left corner");
    }
    @Test public void darkStripeInShadowFails() throws Exception {
        BufferedImage image = fixture(true, true, true);
        Graphics2D g = image.createGraphics(); g.setColor(Color.BLACK); g.fillRect(0, BOTTOM + 15, WIDTH, 3); g.dispose();
        reject(image, "artifacts");
    }
}
