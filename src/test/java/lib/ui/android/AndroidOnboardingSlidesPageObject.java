package lib.ui.android;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidStartScreenRecordingOptions;
import lib.Platform;
import lib.ui.OnboardingSlidesPageObject;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.junit.Assert;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Resource IDs verified in fragment_onboarding/item_onboarding_page of Android 1.41.10. */
public class AndroidOnboardingSlidesPageObject extends OnboardingSlidesPageObject {
    private final String prefix = Platform.getInstance().getAndroidAppPackage() + ":id/";
    public AndroidOnboardingSlidesPageObject(RemoteWebDriver driver) throws IOException { super(driver); }
    @Override protected String headerLocator(int page) {
        return "xpath://*[@resource-id='" + prefix + "viewPager']//*[@resource-id='"
                + prefix + "tvHeader' and @text=" + literal(header(page)) + "]";
    }
    @Override protected String buttonLocator(int page) { return "id:" + prefix + "btnGotIt"; }
    @Override protected By indicatorLocator() { return By.id(prefix + "tabLayoutIndicator"); }
    @Override protected By imageLocator() {
        return By.xpath("//*[@resource-id='" + prefix + "viewPager']//*[@resource-id='"
                + prefix + "ivPhoneMockup' and @displayed='true']");
    }
    @Override protected By closeLocator() {
        return By.xpath("//*[@enabled='true' and (@resource-id='" + prefix + "btnSkip' "
                + "or @resource-id='" + prefix + "btnClose' or @resource-id='" + prefix + "ivClose' "
                + "or @content-desc='Close' or @text='SKIP' or @text='Skip')]");
    }
    @Override protected void assertIndicator(int page) {
        createWait(10).withMessage("Wrong selected dot or total on Android slide " + page).until(d -> {
            List<WebElement> dots = driver.findElements(By.xpath("//*[@resource-id='" + prefix
                    + "tabLayoutIndicator']//*[@content-desc and starts-with(@content-desc,'Tab ')]"));
            if (dots.size() != count()) return false;
            int selected = 0;
            for (int i = 0; i < dots.size(); i++) {
                if ("true".equals(dots.get(i).getAttribute("selected"))) {
                    selected++;
                    if (i + 1 != page) return false;
                }
            }
            return selected == 1;
        });
    }
    @Override protected void swipePlatform(String direction) {
        Assert.assertTrue("Unsupported slide swipe direction", "left".equals(direction) || "right".equals(direction));
        WebElement pager = waitForElementVisible("id:" + prefix + "viewPager", "Slide pager absent", 10);
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("elementId", ((RemoteWebElement) pager).getId());
        arguments.put("direction", direction);
        arguments.put("percent", .8);
        ((JavascriptExecutor) driver).executeScript("mobile: swipeGesture", arguments);
    }
    @Override protected void startPlatformRecording(Dimension size) {
        ((AndroidDriver) driver).startRecordingScreen(new AndroidStartScreenRecordingOptions()
                .withTimeLimit(Duration.ofMinutes(3)));
    }
    @Override protected String stopPlatformRecording() { return ((AndroidDriver) driver).stopRecordingScreen(); }
    private static String literal(String value) {
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        return "concat('" + value.replace("'", "',\"'\",'") + "')";
    }
}
