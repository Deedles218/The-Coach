package lib.ui.ios;

import lib.ui.ModulesPageObject;
import lib.HeaderAppearanceEvidence;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Locators observed in DailyDaySwitcherView; English, portrait, default text size. */
public final class iOSModulesPageObject extends ModulesPageObject {
    private final iOSDailyPlanPageObject today;
    private static final String HEADER = "//XCUIElementTypeOther[@name='DailyDaySwitcherView']";

    public iOSModulesPageObject(RemoteWebDriver driver) {
        super(driver, "id:DailyDaySwitcherView",
                "xpath:" + HEADER + "/XCUIElementTypeStaticText[starts-with(@name,'Module ')]",
                "xpath:" + HEADER + "/XCUIElementTypeStaticText[starts-with(@name,'Stage ')]",
                "xpath:" + HEADER + "/XCUIElementTypeStaticText[starts-with(@name,'Day ')]");
        today = new iOSDailyPlanPageObject(driver);
    }

    @Override public void openTodayTab() {
        new iOSCoachFlowPageObject(driver).closePartnerPromoIfPresent();
        today.openTodayTab();
    }

    @Override public void assertHeaderBounds() {
        super.assertHeaderBounds();
        Rectangle card = waitForElementVisible(
                "xpath://XCUIElementTypeButton[@name='nav bar bookmarks' and @visible='true']/parent::XCUIElementTypeOther",
                "Today navigation header is absent", 10).getRect();
        byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
        try (InputStream source = getClass().getResourceAsStream("/modules/coa8772-header.png")) {
            if (source == null) throw new IllegalStateException("COA-8772 independent header reference is missing");
            BufferedImage reference = ImageIO.read(source);
            BufferedImage actual = ImageIO.read(new ByteArrayInputStream(screenshot));
            double scale = actual.getWidth() / (double) driver.manage().window().getSize().width;
            java.awt.Rectangle pixels = new java.awt.Rectangle((int) Math.round(card.x * scale),
                    (int) Math.round(card.y * scale), (int) Math.round(card.width * scale),
                    (int) Math.round(card.height * scale));
            HeaderAppearanceEvidence.Metrics expected = HeaderAppearanceEvidence.measure(reference,
                    new java.awt.Rectangle(4, 0, 666, 134));
            HeaderAppearanceEvidence.Metrics observed = HeaderAppearanceEvidence.measure(actual, pixels);
            Allure.addAttachment("COA-8772 header appearance metrics",
                    "Independent Jira attachment image-20251105-133140.png; bottom corners and shadow bands.\n"
                            + "reference: " + expected + "\nactual: " + observed);
            Allure.addAttachment("Today header appearance screenshot", "image/png",
                    new ByteArrayInputStream(screenshot), "png");
            HeaderAppearanceEvidence.assertMatches(expected, observed);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot inspect header appearance evidence", failure);
        }
    }
}
