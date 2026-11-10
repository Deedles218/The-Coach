package lib.ui;

import lib.AppearanceSnapshot;
import lib.DailyPlanCompletionMarker;
import lib.MobileCaseFixture;
import org.junit.Assert;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SystemAppearancePageObject extends MainPageObject {
    private final MobileCaseFixture fixture;

    public SystemAppearancePageObject(RemoteWebDriver driver, MobileCaseFixture fixture) {
        super(driver);
        this.fixture = fixture;
    }

    public List<Region> snapshot() {
        return snapshot(true);
    }

    private List<Region> snapshot(boolean requireReadable) {
        int count = fixture.positiveInt("appearance.regions");
        Assert.assertTrue("Capture the Today heading, body text and a primary control", count >= 3);
        List<Region> result = new ArrayList<>();
        byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
        for (int n = 1; n <= count; n++) {
            WebElement element = waitForElementVisible(fixture.selector("appearance.region." + n),
                    "A reviewed Today element is not readable/visible", 10);
            if (requireReadable) Assert.assertFalse("Reviewed appearance region has no text", element.getText().trim().isEmpty());
            try {
                BufferedImage crop = DailyPlanCompletionMarker.crop(screenshot, element.getRect(), driver.manage().window().getSize().width);
                if (requireReadable) Assert.assertTrue("Today region has no visible text/background contrast", AppearanceSnapshot.hasVisibleContrast(crop));
                result.add(new Region(element.getRect(), element.getText(), crop));
            } catch (IOException error) {
                throw new AssertionError("Cannot inspect the rendered Today region", error);
            }
        }
        return result;
    }

    public void assertUnchanged(List<Region> before) {
        final int[] stable = {0};
        createWait(15).withMessage("System dark mode changed the reviewed Today layout, text or rendering")
                .until(ignored -> {
                    List<Region> after = snapshot(false);
                    boolean same = before.size() == after.size();
                    for (int n = 0; same && n < before.size(); n++) {
                        Region first = before.get(n), second = after.get(n);
                        same = first.bounds.equals(second.bounds) && first.text.equals(second.text)
                                && AppearanceSnapshot.sameRendering(first.image, second.image);
                    }
                    stable[0] = same ? stable[0] + 1 : 0;
                    return stable[0] >= 3;
                });
    }

    public static final class Region {
        private final Rectangle bounds;
        private final String text;
        private final BufferedImage image;
        private Region(Rectangle bounds, String text, BufferedImage image) {
            this.bounds = bounds; this.text = text; this.image = image;
        }
    }
}
