package lib.ui.ios;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.IOSStartScreenRecordingOptions;
import lib.ui.OnboardingSlidesPageObject;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** iOS locators and recording; content/media assertions are shared with Android. */
public class iOSOnboardingSlidesPageObject extends OnboardingSlidesPageObject {
    private static final By INDICATOR = AppiumBy.iOSClassChain("**/XCUIElementTypePageIndicator[`visible == 1`]");
    public iOSOnboardingSlidesPageObject(RemoteWebDriver driver) throws IOException { super(driver); }
    @Override protected String headerLocator(int page) { return "id:" + header(page); }
    @Override protected String buttonLocator(int page) { return "id:" + configured(page + ".buttonText"); }
    @Override protected String buttonText(WebElement button) { return button.getAttribute("label"); }
    @Override protected By indicatorLocator() { return INDICATOR; }
    @Override protected By imageLocator() { return AppiumBy.iOSClassChain("**/XCUIElementTypeImage[`visible == 1`]"); }
    @Override protected By closeLocator() {
        return AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND visible == 1 AND "
                + "(name IN {'SKIP', 'Skip', 'Пропустить', 'Close', 'CLOSE', 'Закрыть', 'X', "
                + "'ic_outline_close', 'ic outline close', 'CloseRoundBlack', 'navBarRoundClose'})");
    }
    @Override protected void assertIndicator(int page) {
        createWait(10).withMessage("Wrong page indicator on slide " + page).until(d ->
                ("page " + page + " of " + count()).equals(driver.findElement(INDICATOR).getAttribute("value")));
    }
    @Override protected void swipePlatform(String direction) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("direction", direction);
        ((JavascriptExecutor) driver).executeScript("mobile: swipe", arguments);
    }
    @Override protected void startPlatformRecording(Dimension size) {
        ((IOSDriver) driver).startRecordingScreen(new IOSStartScreenRecordingOptions()
                .withVideoType("libx264").withFps(10).withTimeLimit(Duration.ofMinutes(3))
                .withVideoScale(size.getWidth() + ":" + size.getHeight()));
    }
    @Override protected String stopPlatformRecording() { return ((IOSDriver) driver).stopRecordingScreen(); }
}
