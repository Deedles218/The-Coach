package lib.ui.android;

import io.qameta.allure.Step;
import lib.Platform;
import lib.ui.CoachFlowPageObject;
import lib.ui.MainPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AndroidSpanishWorkbookPageObject extends MainPageObject {
    private final CoachFlowPageObject coach;
    private final String id;

    public AndroidSpanishWorkbookPageObject(RemoteWebDriver driver) {
        super(driver);
        coach = CoachFlowPageObjectFactory.get(driver);
        id = "id:" + Platform.getInstance().getAndroidAppPackage() + ":id/";
    }

    @Step("Open the localized workbook offer from Profile")
    public void openOffer(String workbookLabel) {
        coach.openProfile();
        String label = "xpath://android.widget.TextView[@resource-id='" + id.substring(3) + "tvTitle'"
                + " and @text=" + literal(workbookLabel) + "]";
        WebElement settings = waitForElementVisible(id + "rvSettings", "Profile settings list is absent", 15);
        for (int i = 0; i < 8 && !isElementVisible(label); i++) {
            scroll(settings, "down");
        }
        waitForElementAndClick(label, "Spanish workbook entry did not appear in Profile", 15);
        waitForElementVisible(id + "tvEworkbook", "The account did not open the workbook offer", 20);
        waitForElementNotVisible(id + "pbLoading", "Workbook offer is still loading", 20);
    }

    @Step("Verify Spanish offer copy is visible: {expected}")
    public void assertCopy(String expected) {
        String copy = "xpath://*[@displayed='true' and normalize-space(translate(@text,' ',' '))=" + literal(expected) + "]";
        reveal(copy);
    }

    @Step("Verify the purchase control and a resolved price without purchasing")
    public void assertPurchaseDetails(String label) {
        reveal(id + "tvNewPrice");
        String price = waitForElementVisible(id + "tvNewPrice", "Workbook price is missing", 12).getText().trim();
        Assert.assertTrue("Expected a resolved currency price instead of a placeholder",
                price.matches(".*\\p{Sc}.*") && price.matches(".*[0-9].*") && !price.matches(".*[%{}<>].*"));
        reveal(id + "btnContinue");
        WebElement cta = waitForElementEnabled(id + "btnContinue", "Purchase action is unavailable", 12);
        Assert.assertEquals("Wrong Spanish purchase label", label, getElementAccessibleName(cta).trim());
    }

    @Step("Verify the workbook cover is rendered")
    public void assertRenderedCover(String coverResource) throws IOException {
        Assert.assertTrue("Use an observed short Android resource ID for the workbook cover",
                coverResource.matches("[A-Za-z_][A-Za-z0-9_]*"));
        String cover = id + coverResource;
        reveal(cover);
        Rectangle bounds = waitForElementVisible(cover, "Workbook cover view did not appear", 12).getRect();
        BufferedImage screenshot = ImageIO.read(new ByteArrayInputStream(driver.getScreenshotAs(OutputType.BYTES)));
        Assert.assertNotNull("Workbook screenshot cannot be decoded", screenshot);
        Assert.assertTrue("The workbook cover has no visible area",
                bounds.width > 16 && bounds.height > 16 && bounds.x >= 0 && bounds.y >= 0
                        && bounds.x + bounds.width <= screenshot.getWidth() && bounds.y + bounds.height <= screenshot.getHeight());
        Set<Integer> colors = new HashSet<>();
        for (int x = 1; x <= 8; x++) {
            for (int y = 1; y <= 8; y++) {
                int rgb = screenshot.getRGB(bounds.x + x * bounds.width / 9, bounds.y + y * bounds.height / 9);
                colors.add(rgb & 0xf0f0f0);
            }
        }
        Assert.assertTrue("Workbook cover area is blank or a solid placeholder", colors.size() >= 6);
    }

    public void closeOfferIfVisible() {
        if (isElementVisible(id + "tvEworkbook") || isElementVisible(id + "tvNewPrice")) {
            coach.closePdfGuideUpsell();
        }
        if (isElementVisible(id + "rvSettings")) {
            coach.returnToMainScreenFromProfile();
        }
    }

    private void reveal(String locator) {
        if (isElementVisible(locator)) return;
        WebElement offer = waitForElementVisible(id + "scroll", "Workbook offer scroll area is absent", 15);
        // Each expected control may sit above or below the previous assertion.
        for (String direction : new String[]{"up", "down"}) {
            for (int i = 0; i < 8 && !isElementVisible(locator); i++) {
                if (Boolean.FALSE.equals(scroll(offer, direction))) break;
            }
        }
        waitForElementVisible(locator, "Expected workbook content is missing or clipped", 12);
    }

    private Object scroll(WebElement container, String direction) {
        Map<String, Object> gesture = new HashMap<>();
        gesture.put("elementId", ((RemoteWebElement) container).getId());
        gesture.put("direction", direction);
        gesture.put("percent", .64);
        return driver.executeScript("mobile: scrollGesture", gesture);
    }

    private static String literal(String text) {
        Assert.assertFalse("The reviewed Spanish label must not contain an apostrophe", text.contains("'"));
        return "'" + text + "'";
    }
}
