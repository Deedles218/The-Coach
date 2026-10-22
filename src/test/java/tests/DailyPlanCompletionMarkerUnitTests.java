package tests;

import lib.DailyPlanCompletionMarker;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Rectangle;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class DailyPlanCompletionMarkerUnitTests {
    private BufferedImage fixture(String name) throws Exception {
        return ImageIO.read(getClass().getResource("/bookmarks/completion/" + name + ".png"));
    }

    @Test public void recognizesBothActualBuilds() throws Exception {
        Assert.assertTrue(DailyPlanCompletionMarker.isCompleted(fixture("male-completed")));
        Assert.assertTrue(DailyPlanCompletionMarker.isCompleted(fixture("female-completed")));
    }

    @Test public void rejectsBothActualUnfinishedArrows() throws Exception {
        Assert.assertFalse(DailyPlanCompletionMarker.isCompleted(fixture("male-arrow")));
        Assert.assertFalse(DailyPlanCompletionMarker.isCompleted(fixture("female-arrow")));
    }

    @Test public void rejectsBlankSolidAndWrongGlyph() {
        BufferedImage image = new BufferedImage(90, 90, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 90, 90);
            Assert.assertFalse(DailyPlanCompletionMarker.isCompleted(image));
            graphics.setColor(Color.ORANGE); graphics.fillRect(0, 0, 90, 90);
            Assert.assertFalse(DailyPlanCompletionMarker.isCompleted(image));
            graphics.setColor(Color.WHITE); graphics.fillRect(15, 40, 60, 10);
            Assert.assertFalse(DailyPlanCompletionMarker.isCompleted(image));
        } finally { graphics.dispose(); }
    }

    @Test public void scalesAndCropsOnlyTheSelectedIndicator() throws Exception {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.drawImage(fixture("female-completed"), 100, 100, 72, 74, null);
            graphics.drawImage(fixture("female-arrow"), 300, 100, 72, 74, null);
        } finally { graphics.dispose(); }
        ByteArrayOutputStream png = new ByteArrayOutputStream(); ImageIO.write(image, "png", png);
        Assert.assertTrue(DailyPlanCompletionMarker.isCompleted(DailyPlanCompletionMarker.crop(
                png.toByteArray(), new Rectangle(50, 50, 37, 36), 400)));
        Assert.assertFalse("A completed neighboring card cannot complete the target", DailyPlanCompletionMarker.isCompleted(
                DailyPlanCompletionMarker.crop(png.toByteArray(), new Rectangle(150, 50, 37, 36), 400)));
    }

    @Test(expected = java.io.IOException.class)
    public void rejectsOffscreenIndicator() throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream(); ImageIO.write(fixture("male-completed"), "png", png);
        DailyPlanCompletionMarker.crop(png.toByteArray(), new Rectangle(-1, 0, 37, 36), 36);
    }
}
