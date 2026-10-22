package lib;

import org.openqa.selenium.Rectangle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Recognizes the rendered white tick when the live iOS indicator has no accessibility name. */
public final class DailyPlanCompletionMarker {
    private static final int GRID = 32;
    private static final boolean[] EXPECTED = mask(readReference());

    private DailyPlanCompletionMarker() { }

    public static BufferedImage crop(byte[] screenshot, Rectangle bounds, int screenWidth) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshot));
        if (image == null || screenWidth <= 0) throw new IOException("Unreadable completion screenshot");
        double scale = image.getWidth() / (double) screenWidth;
        int x = (int) Math.round(bounds.x * scale), y = (int) Math.round(bounds.y * scale);
        int width = (int) Math.round(bounds.width * scale), height = (int) Math.round(bounds.height * scale);
        if (width < 12 || height < 12 || x < 0 || y < 0
                || x + width > image.getWidth() || y + height > image.getHeight()) {
            throw new IOException("Completion indicator is outside the screenshot");
        }
        return image.getSubimage(x, y, width, height);
    }

    public static boolean isCompleted(BufferedImage indicator) {
        if (indicator == null || indicator.getWidth() < 12 || indicator.getHeight() < 12
                || Math.abs(1 - indicator.getWidth() / (double) indicator.getHeight()) > .15) return false;
        boolean[] actual = mask(indicator);
        int intersection = 0, union = 0, different = 0;
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] && EXPECTED[i]) intersection++;
            if (actual[i] || EXPECTED[i]) union++;
            if (actual[i] != EXPECTED[i]) different++;
        }
        // Compare the tick's shape, not the male/female circle color. A blank region,
        // an arrow, or a solid circle cannot satisfy both shape and overlap checks.
        return union > 0 && intersection / (double) union >= .80 && different <= GRID * GRID * .05;
    }

    private static boolean[] mask(BufferedImage image) {
        boolean[] result = new boolean[GRID * GRID];
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int rgb = image.getRGB((int) (image.getWidth() * (.1 + .8 * (x + .5) / GRID)),
                        (int) (image.getHeight() * (.1 + .8 * (y + .5) / GRID)));
                int red = (rgb >> 16) & 255, green = (rgb >> 8) & 255, blue = rgb & 255;
                result[y * GRID + x] = Math.min(red, Math.min(green, blue)) >= 225
                        && Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue)) <= 25;
            }
        }
        return result;
    }

    private static BufferedImage readReference() {
        try (InputStream input = DailyPlanCompletionMarker.class.getResourceAsStream(
                "/bookmarks/completion/male-completed.png")) {
            if (input == null) throw new IOException("Completion tick reference is missing");
            BufferedImage image = ImageIO.read(input);
            if (image == null) throw new IOException("Completion tick reference is unreadable");
            return image;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot load completion tick reference", error);
        }
    }
}
