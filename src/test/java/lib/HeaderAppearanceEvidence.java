package lib;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Locale;
import org.junit.Assert;

/** Bottom-corner and shadow oracle, independent of text, icons and progress.
 * Reference: COA-8772, image-20251105-133140.png, original 678 x 174.
 * Samples scale with card width, so a physical-device screenshot need not
 * have the same pixel density as the reference. This is not a whole-page diff. */
public final class HeaderAppearanceEvidence {
    private HeaderAppearanceEvidence() { }

    public static final class Metrics {
        public final double face, shadow, fade, leftCorner, rightCorner, upperLeft, upperRight;
        public final double maximumDarkening, maximumColorSpread;
        private Metrics(double face, double shadow, double fade, double leftCorner,
                        double rightCorner, double upperLeft, double upperRight,
                        double maximumDarkening, double maximumColorSpread) {
            this.face = face; this.shadow = shadow; this.fade = fade;
            this.leftCorner = leftCorner; this.rightCorner = rightCorner;
            this.upperLeft = upperLeft; this.upperRight = upperRight;
            this.maximumDarkening = maximumDarkening; this.maximumColorSpread = maximumColorSpread;
        }
        @Override public String toString() {
            return String.format(Locale.ROOT,
                    "face=%.2f; shadow=%.2f; fade=%.2f; leftCorner=%.2f; rightCorner=%.2f; "
                    + "upperCorners=%.2f/%.2f; maximumDarkening=%.2f; maximumColorSpread=%.2f",
                    face, shadow, fade, leftCorner, rightCorner, upperLeft, upperRight,
                    maximumDarkening, maximumColorSpread);
        }
    }

    public static Metrics measure(BufferedImage image, Rectangle card) {
        Assert.assertNotNull("Header screenshot is unreadable", image);
        Assert.assertTrue("Header bounds must be positive", card.width > 100 && card.height > 0);
        int bottom = card.y + card.height;
        Assert.assertTrue("Header/shadow region is outside the screenshot",
                card.x >= 0 && card.y >= 0 && card.x + card.width <= image.getWidth()
                        && bottom + Math.ceil(card.width * .05) + 2 < image.getHeight());
        double face = 0, contrast = 0, fade = 0, darkening = 0, spread = 0;
        for (double band : new double[]{.2, .5, .8}) {
            int x = card.x + (int) Math.round(card.width * band);
            double foreground = sample(image, x, bottom - (int) Math.round(card.width * .015))[0];
            double minimum = 255, previous = -1, tail = 0;
            for (double distance : new double[]{.003, .006, .01, .015, .02, .03, .04, .05}) {
                double[] color = sample(image, x, bottom + (int) Math.round(card.width * distance));
                minimum = Math.min(minimum, color[0]);
                if (previous >= 0) darkening = Math.max(darkening, previous - color[0]);
                previous = tail = color[0];
                spread = Math.max(spread, color[1]);
            }
            face += foreground / 3;
            contrast += (foreground - minimum) / 3;
            fade += (tail - minimum) / 3;
        }
        int outer = Math.max(2, (int) Math.round(card.width * .002));
        int inner = (int) Math.round(card.width * .04);
        int lower = bottom - Math.max(2, (int) Math.round(card.width * .006));
        int upper = bottom - (int) Math.round(card.width * .045);
        int right = card.x + card.width - 1;
        return new Metrics(face, contrast, fade,
                sample(image, card.x + inner, lower)[0] - sample(image, card.x + outer, lower)[0],
                sample(image, right - inner, lower)[0] - sample(image, right - outer, lower)[0],
                sample(image, card.x + outer, upper)[0], sample(image, right - outer, upper)[0],
                darkening, spread);
    }

    public static void assertMatches(Metrics reference, Metrics actual) {
        Assert.assertTrue("Header is obscured or not the expected white card: " + actual,
                actual.face >= 245 && actual.upperLeft >= 245 && actual.upperRight >= 245);
        Assert.assertTrue("Missing bottom shadow: " + actual,
                actual.shadow >= Math.max(3, reference.shadow * .2));
        Assert.assertTrue("Shadow does not fade into the background: " + actual,
                actual.fade >= Math.max(3, reference.fade * .2)
                        && actual.fade >= actual.shadow * .5);
        Assert.assertTrue("Bottom-left corner is square or obscured: " + actual,
                actual.leftCorner >= Math.max(3, reference.leftCorner * .2));
        Assert.assertTrue("Bottom-right corner is square or obscured: " + actual,
                actual.rightCorner >= Math.max(3, reference.rightCorner * .2));
        Assert.assertTrue("Unexpected dark/colored artifacts in shadow bands: " + actual,
                actual.maximumDarkening <= 3 && actual.maximumColorSpread <= 5);
    }

    private static double[] sample(BufferedImage image, int x, int y) {
        double[] channels = new double[3];
        for (int yy = y - 1; yy <= y + 1; yy++) for (int xx = x - 1; xx <= x + 1; xx++) {
            int rgb = image.getRGB(xx, yy);
            channels[0] += ((rgb >> 16) & 255) / 9.0;
            channels[1] += ((rgb >> 8) & 255) / 9.0;
            channels[2] += (rgb & 255) / 9.0;
        }
        double mean = (channels[0] + channels[1] + channels[2]) / 3;
        Arrays.sort(channels);
        return new double[]{mean, channels[2] - channels[0]};
    }
}
