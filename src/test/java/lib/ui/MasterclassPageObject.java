package lib.ui;

import lib.Platform;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.Map;

/** Native accessibility contract for the embedded masterclass, on both platforms. */
public class MasterclassPageObject extends MainPageObject {
    private final boolean ios = Platform.getInstance().isIOS();

    public MasterclassPageObject(RemoteWebDriver driver) { super(driver); }

    public static String required(String name) {
        String value = System.getProperty("masterclass." + name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Configure -Dmasterclass." + name + " for this build/fixture");
        }
        return value.trim();
    }

    private String text(String value) {
        // XPath 1.0 literals, including titles containing both quote types.
        String[] parts = value.split("'", -1);
        StringBuilder literal = new StringBuilder("concat('");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) literal.append("',\"'\",'");
            literal.append(parts[i]);
        }
        literal.append("','')");
        return "xpath://*[" + (ios ? "@name=" : "@text=") + literal
                + (ios ? " and @visible='true'" : "") + "]";
    }

    public void assertText(String value) {
        waitForElementVisible(text(value), "Masterclass content missing: " + value, 30);
    }

    public void tapText(String value) {
        waitForElementAndClick(text(value), "Masterclass control missing: " + value, 15);
    }

    private String courseContent() {
        String title = text(required("course"));
        return ios ? title : title.replace("xpath://*", "xpath://android.webkit.WebView//*");
    }

    private void swipe(String direction) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("direction", direction);
        if (ios) {
            driver.executeScript("mobile: swipe", args);
        } else {
            Dimension size = driver.manage().window().getSize();
            args.put("left", size.width / 10); args.put("top", size.height / 5);
            args.put("width", size.width * 8 / 10); args.put("height", size.height / 2);
            args.put("percent", 0.7);
            driver.executeScript("mobile: swipeGesture", args);
        }
    }

    public void openFromExplore() {
        closePlayerIfPresent();
        if (!ios && isElementVisible(courseContent())) backToExplore();
        String courseBack = "xpath://XCUIElementTypeButton[@name='BackButton']";
        if (ios && isElementVisible(text("New Supplements"))) {
            waitForElementAndClick("xpath://XCUIElementTypeButton[@name='GO TO SHOP']",
                    "Cannot dismiss the optional Shop introduction", 10);
            waitForElementNotPresent(text("New Supplements"), "Shop introduction remained open", 10);
        }
        String tab = ios ? "xpath://XCUIElementTypeButton[@name='Explore']"
                : "id:" + Platform.getInstance().getAndroidAppPackage() + ":id/nav_graph_explore";
        if (ios) {
            final String careHeading = "xpath://XCUIElementTypeStaticText[contains(@name,'Get personalized care') and contains(@name,'WhatsApp') and @visible='true']";
            createWait(20).withMessage("Native navigation is not ready").until(d -> {
                if (isElementVisible(careHeading)) {
                    waitForElementAndClick("xpath://XCUIElementTypeButton[@name='close' and @visible='true']",
                            "Cannot close optional personal-care offer", 5);
                    waitForElementNotPresent(careHeading, "Personal-care offer remained open", 5);
                    return false;
                }
                for (String entry : new String[]{courseBack, tab}) {
                    for (WebElement element : driver.findElements(getLocatorByString(entry))) {
                        if (element.isDisplayed() && isElementEnabled(element)) return true;
                    }
                }
                return false;
            });
            if (isElementVisible(courseBack)) backToExplore();
        }
        waitForElementAndClick(tab, "Pre-authenticated Explore tab is required", 20);
        String card = text(required("card"));
        // Normalize scroll first: subsequent tests may inherit a scrolled catalog.
        for (int i = 0; i < 5 && !isElementVisible(card); i++) swipe("down");
        for (int i = 0; i < 10 && !isElementVisible(card); i++) swipe("up");
        waitForElementAndClick(card, "Configured masterclass card missing", 15);
    }

    public void assertPurchased() {
        assertText(required("course"));
        assertText(required("lesson"));
        Assert.assertFalse("Purchased course asks for payment again", isElementVisible(text("UNLOCK ACCESS")));
    }

    public void assertUnpaid() {
        assertText(required("offerTitle"));
        String price = required("price");
        assertText(price);
        waitForElementEnabled(text("UNLOCK ACCESS"), "Purchase CTA missing or disabled", 20);
        Assert.assertFalse("Unpaid fixture unexpectedly has lesson access",
                isElementVisible(text(required("lesson"))));
    }

    public void backToExplore() {
        String back = ios ? "xpath://XCUIElementTypeButton[@name='BackButton']"
                : System.getProperty("masterclass.backLocator");
        if (back == null) {
            driver.navigate().back();
        } else {
            waitForElementAndClick(back, "Masterclass back button missing", 10);
        }
        String tab = ios ? "xpath://XCUIElementTypeButton[@name='Explore']"
                : "id:" + Platform.getInstance().getAndroidAppPackage() + ":id/nav_graph_explore";
        waitForElementVisible(tab, "Back did not return to native navigation", 15);
        waitForElementNotVisible(courseContent(), "Course remained open after Back", 15);
    }

    public void assertNextDayLocked() {
        assertPurchased();
        tapText(required("nextDay"));
        assertText(required("lockedMessage"));
        assertText(required("lesson"));
        assertText(required("currentDay"));
    }

    public void startPlayer() {
        String locator = System.getProperty("masterclass.playerStartLocator");
        if (locator == null && ios) {
            locator = "xpath://XCUIElementTypeWebView//XCUIElementTypeImage[@visible='true']";
        }
        if (locator == null) locator = required("playerStartLocator");
        waitForElementAndClick(locator, "Lesson video start target missing", 15);
    }

    private String playerControl(String name) {
        String locator = System.getProperty("masterclass.player." + name);
        if (locator == null && ios) {
            String alternate = name;
            if ("Play".equals(name) || "Pause".equals(name)) alternate = "Play/Pause";
            if ("Go Forward 10 Seconds".equals(name)) alternate = "Skip Forward";
            if ("Close".equals(name)) alternate = "Close Button";
            locator = "xpath://XCUIElementTypeButton[@visible='true' and (@name='" + name
                    + "' or @name='" + alternate + "')]";
        }
        if (locator == null) locator = required("player." + name);
        return locator;
    }

    public void tapPlayerControl(String name) {
        String locator = playerControl(name);
        waitForElementAndClick(locator, "Player control missing: " + name, 15);
    }

    private void revealPlayerControls() {
        WebElement surface = waitForElementVisible(required("positionLocator"), "Player surface missing", 10);
        org.openqa.selenium.Rectangle rect = surface.getRect();
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x", rect.x + rect.width / 2); args.put("y", rect.y + rect.height / 2);
        driver.executeScript("mobile: tap", args);
    }

    public void pausePlayer() {
        if (ios && !isElementVisible(playerControl("Pause"))) {
            // The center tap exposes AVPlayer controls and pauses this embedded player.
            revealPlayerControls();
        } else {
            tapPlayerControl("Pause");
        }
        if (ios) waitForElementVisible(playerControl("Play"), "Player controls not exposed", 10);
    }

    public void closePlayerIfPresent() {
        String position = System.getProperty("masterclass.positionLocator");
        if (!ios || position == null) return;
        String slider = "xpath://XCUIElementTypeSlider[@name='Current position' and @visible='true']";
        if (isElementVisible(position) || isElementVisible(slider)) {
            if (!isElementVisible(playerControl("Close"))) revealPlayerControls();
            tapPlayerControl("Close");
        }
    }

    public int positionSeconds() {
        String slider = "xpath://XCUIElementTypeSlider[@name='Current position' and @visible='true']";
        String position = required("positionLocator");
        return createWait(10).withMessage("Playback position missing").until(d -> {
            // Controls can auto-hide between locating the slider and reading its value.
            for (String locator : ios ? new String[]{slider, position} : new String[]{position}) {
                for (WebElement element : driver.findElements(getLocatorByString(locator))) {
                    if (element.isDisplayed()) {
                        String value = element.getAttribute(System.getProperty(
                                "masterclass.positionAttribute", ios ? "value" : "text"));
                        if (value != null && !value.trim().isEmpty()) return timeSeconds(value);
                    }
                }
            }
            return null;
        });
    }

    private int timeSeconds(String value) {
        Assert.assertNotNull("Playback time is not exposed", value);
        java.util.regex.Matcher clock = java.util.regex.Pattern.compile("(\\d+):(\\d{2})").matcher(value);
        if (clock.find()) return Integer.parseInt(clock.group(1)) * 60 + Integer.parseInt(clock.group(2));
        java.util.regex.Matcher words = java.util.regex.Pattern.compile("(?:(\\d+) minutes?[, ]*)?(\\d+) seconds?").matcher(value);
        Assert.assertTrue("Unrecognized playback time: " + value, words.find());
        return (words.group(1) == null ? 0 : Integer.parseInt(words.group(1)) * 60) + Integer.parseInt(words.group(2));
    }

    public void assertPlaybackHasSafeRemainingTime() {
        String locator = ios ? required("positionLocator") : required("durationLocator");
        WebElement duration = waitForElementVisible(locator, "Video duration missing", 10);
        String value = duration.getAttribute(System.getProperty("masterclass.durationAttribute", ios ? "label" : "text"));
        int total = timeSeconds(value);
        int position = positionSeconds();
        Assert.assertTrue("Playback fixture is near the end; use a lesson with at least 120 seconds remaining",
                total - position >= 120);
    }

    public void waitForPositionToAdvance(final int previous) {
        createWait(30).withMessage("Video position did not advance").until(d -> positionSeconds() > previous);
    }
}
