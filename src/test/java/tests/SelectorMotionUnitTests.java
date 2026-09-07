package tests;

import lib.SimulatorModalMotion;
import org.junit.Test;
import org.junit.Assert;
import java.util.Arrays;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.openqa.selenium.Rectangle;

/** Guards against treating a static frame, jitter, or opposite motion as animation. */
public class SelectorMotionUnitTests {
    @Test public void acceptsIntermediateOpeningAndClosingPositions() {
        SimulatorModalMotion.assertMotionPositions(Arrays.asList(750, 680, 550, 340), true);
        SimulatorModalMotion.assertMotionPositions(Arrays.asList(340, 550, 680, 750), false);
    }
    @Test(expected = AssertionError.class) public void rejectsSingleFinalScreenshot() {
        SimulatorModalMotion.assertMotionPositions(Arrays.asList(340), true);
    }
    @Test(expected = AssertionError.class) public void rejectsInstantJump() {
        SimulatorModalMotion.assertMotionPositions(Arrays.asList(750, 340), true);
    }
    @Test(expected = AssertionError.class) public void rejectsOppositeDirection() {
        SimulatorModalMotion.assertMotionPositions(Arrays.asList(340, 550, 680), true);
    }
    @Test(expected = AssertionError.class) public void rejectsStationaryCompressionJitter() {
        SimulatorModalMotion.assertMotionPositions(Arrays.asList(340, 343, 340, 343, 340), true);
    }

    private byte[] cardImage(Color color, boolean outline) throws Exception {
        BufferedImage image = new BufferedImage(390, 844, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        if (outline) g.drawRect(20, 340, 350, 120);
        else g.fillRect(100, 370, 100, 40);
        g.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    @Test public void acceptsOrangeOnAllFourEdges() throws Exception {
        SimulatorModalMotion.assertOrangeOutline(cardImage(new Color(255, 130, 50), true),
                new Rectangle(20, 340, 120, 350), 390);
    }
    @Test(expected = AssertionError.class) public void rejectsOrangeArtworkWithoutOutline() throws Exception {
        SimulatorModalMotion.assertOrangeOutline(cardImage(new Color(255, 130, 50), false),
                new Rectangle(20, 340, 120, 350), 390);
    }
    @Test(expected = AssertionError.class) public void rejectsOutlineOfWrongColor() throws Exception {
        SimulatorModalMotion.assertOrangeOutline(cardImage(Color.GREEN, true),
                new Rectangle(20, 340, 120, 350), 390);
    }
    @Test public void findsShortHandleOnWhiteSheet() {
        BufferedImage image = new BufferedImage(390, 844, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 320, 390, 524);
        g.setColor(new Color(210, 210, 210)); g.fillRoundRect(171, 332, 48, 4, 4, 4);
        g.dispose();
        int y = SimulatorModalMotion.findSheetHandle(image, 200);
        Assert.assertTrue("Handle not detected", y >= 332 && y <= 335);
    }
    @Test public void rejectsGrayBackdropWithoutHandle() {
        BufferedImage image = new BufferedImage(390, 844, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(210, 210, 210)); g.fillRect(0, 200, 390, 644);
        g.dispose();
        Assert.assertEquals(-1, SimulatorModalMotion.findSheetHandle(image, 200));
    }
}
