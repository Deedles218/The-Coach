package lib;

import java.awt.image.BufferedImage;

/** Compares app-owned regions, excluding system bars, clocks and animated content. */
public final class AppearanceSnapshot {
    private AppearanceSnapshot() { }

    public static boolean hasVisibleContrast(BufferedImage image) {
        if (image == null || image.getWidth() < 8 || image.getHeight() < 8) return false;
        int low = 255, high = 0;
        int[] histogram = new int[256];
        int count = image.getWidth() * image.getHeight();
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int rgb = image.getRGB(x, y);
            int luminance = (int) (.2126 * ((rgb >> 16) & 255) + .7152 * ((rgb >> 8) & 255) + .0722 * (rgb & 255));
            histogram[luminance]++;
        }
        int accumulated = 0;
        for (int n = 0; n < 256; n++) {
            accumulated += histogram[n];
            if (accumulated >= Math.max(1, count / 100)) { low = n; break; }
        }
        accumulated = 0;
        for (int n = 255; n >= 0; n--) {
            accumulated += histogram[n];
            if (accumulated >= Math.max(1, count / 100)) { high = n; break; }
        }
        return high - low >= 40;
    }

    public static boolean sameRendering(BufferedImage before, BufferedImage after) {
        if (!hasVisibleContrast(before) || !hasVisibleContrast(after)
                || before.getWidth() != after.getWidth() || before.getHeight() != after.getHeight()) return false;
        int changed = 0;
        for (int y = 0; y < before.getHeight(); y++) for (int x = 0; x < before.getWidth(); x++) {
            int first = before.getRGB(x, y), second = after.getRGB(x, y);
            for (int shift : new int[]{0, 8, 16}) {
                if (Math.abs(((first >> shift) & 255) - ((second >> shift) & 255)) > 24) { changed++; break; }
            }
        }
        return changed <= before.getWidth() * before.getHeight() * .01;
    }
}
