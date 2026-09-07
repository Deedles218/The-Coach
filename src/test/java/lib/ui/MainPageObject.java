package lib.ui;

import io.appium.java_client.PerformsTouchActions;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import io.qameta.allure.Attachment;
import lib.Platform;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MainPageObject {
    protected RemoteWebDriver driver;

    public MainPageObject(RemoteWebDriver driver) {
        this.driver = driver;
    }

    public WebElement waitForElementPresent(String locator, String error_message, long timeoutInSeconds) {
        return waitForElementVisible(locator, error_message, timeoutInSeconds);
    }

    public WebElement waitForElementVisible(String locator, String error_message, long timeoutInSeconds) {
        By by = this.getLocatorByString(locator);
        WebDriverWait wait = createWait(timeoutInSeconds);
        wait.withMessage(error_message + "\n");
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    public WebElement waitForElementEnabled(String locator, String error_message, long timeoutInSeconds) {
        By by = this.getLocatorByString(locator);
        WebDriverWait wait = createWait(timeoutInSeconds);
        wait.withMessage(error_message + "\n");
        return wait.until(webDriver -> {
            try {
                WebElement element = ExpectedConditions.visibilityOfElementLocated(by).apply(webDriver);
                if (element != null && isElementEnabled(element)) {
                    return element;
                }
            } catch (NoSuchElementException | StaleElementReferenceException ignored) {
                // The explicit wait polls until the control is visible and enabled.
            }
            return null;
        });
    }

    public void assertElementVisible(String locator, String error_message, long timeoutInSeconds) {
        waitForElementVisible(locator, error_message, timeoutInSeconds);
    }

    public void assertElementEnabled(String locator, String error_message, long timeoutInSeconds) {
        waitForElementEnabled(locator, error_message, timeoutInSeconds);
    }

    public void assertElementDisabled(String locator, String error_message, long timeoutInSeconds) {
        WebElement element = waitForElementVisible(locator, error_message, timeoutInSeconds);
        Assert.assertFalse(error_message, isElementEnabled(element));
    }

    public boolean isElementEnabled(WebElement element) {
        if (element == null) {
            return false;
        }

        String enabledAttribute = element.getAttribute("enabled");
        if (enabledAttribute != null
                && ("false".equalsIgnoreCase(enabledAttribute) || "0".equals(enabledAttribute))) {
            return false;
        }
        return element.isEnabled();
    }

    public boolean isElementVisible(String locator) {
        By by = this.getLocatorByString(locator);
        for (WebElement element : driver.findElements(by)) {
            try {
                if (element.isDisplayed()) {
                    return true;
                }
            } catch (StaleElementReferenceException ignored) {
                // A re-rendered element will be evaluated on the next call.
            }
        }
        return false;
    }

    public void waitForElementNotVisible(String locator, String error_message, long timeoutInSeconds) {
        waitForElementNotPresent(locator, error_message, timeoutInSeconds);
    }

    public void waitForLoadingToDisappearIfPresent(
            String loadingLocator,
            String error_message,
            long timeoutInSeconds
    ) {
        if (loadingLocator == null || loadingLocator.trim().isEmpty()) {
            return;
        }
        if (isElementPresent(loadingLocator)) {
            waitForElementNotPresent(loadingLocator, error_message, timeoutInSeconds);
        }
    }

    public void clickOnceAndWaitForTransition(
            String buttonLocator,
            String sourceScreenLocator,
            String destinationScreenLocator,
            String error_message,
            long timeoutInSeconds
    ) {
        waitForElementAndClick(buttonLocator, error_message, timeoutInSeconds);
        if (sourceScreenLocator != null && !sourceScreenLocator.trim().isEmpty()) {
            waitForElementNotPresent(
                    sourceScreenLocator,
                    error_message + ": source screen did not disappear after the single tap",
                    timeoutInSeconds
            );
        }
        waitForElementVisible(
                destinationScreenLocator,
                error_message + ": destination screen did not open after the single tap",
                timeoutInSeconds
        );
    }

    public void clickOnceAndWaitForTransition(
            String[] buttonLocators,
            String[] sourceScreenLocators,
            String[] destinationScreenLocators,
            String error_message,
            long timeoutInSeconds
    ) {
        waitForFirstElementAndClick(buttonLocators, error_message, timeoutInSeconds);
        if (sourceScreenLocators != null && sourceScreenLocators.length > 0) {
            waitForFirstElementNotPresent(
                    sourceScreenLocators,
                    error_message + ": source screen did not disappear after the single tap",
                    timeoutInSeconds
            );
        }
        waitForFirstElementPresent(
                destinationScreenLocators,
                error_message + ": destination screen did not open after the single tap",
                timeoutInSeconds
        );
    }

    public void tapDisabledAndAssertNoTransition(
            String disabledButtonLocator,
            String protectedScreenLocator,
            String transitionScreenLocator,
            String error_message,
            long timeoutInSeconds
    ) {
        WebElement disabledButton = waitForElementVisible(
                disabledButtonLocator,
                error_message + ": disabled control is not visible",
                timeoutInSeconds
        );
        Assert.assertFalse(
                error_message + ": control must be disabled before the no-op tap",
                isElementEnabled(disabledButton)
        );

        try {
            disabledButton.click();
        } catch (WebDriverException ignored) {
            // A native driver may reject a click on a disabled control; that is a valid no-op.
        }

        waitForElementVisible(
                protectedScreenLocator,
                error_message + ": protected screen disappeared after tapping a disabled control",
                timeoutInSeconds
        );
        if (transitionScreenLocator != null && !transitionScreenLocator.trim().isEmpty()) {
            Assert.assertFalse(
                    error_message + ": disabled control triggered an unexpected transition",
                    isElementVisible(transitionScreenLocator)
            );
        }
    }

    public void tapDisabledAndAssertNoTransition(
            String[] disabledButtonLocators,
            String protectedScreenLocator,
            String transitionScreenLocator,
            String error_message,
            long timeoutInSeconds
    ) {
        WebElement disabledButton = waitForFirstElementPresent(
                disabledButtonLocators,
                error_message + ": disabled control is not visible",
                timeoutInSeconds
        );
        Assert.assertFalse(
                error_message + ": control must be disabled before the no-op tap",
                isElementEnabled(disabledButton)
        );

        try {
            disabledButton.click();
        } catch (WebDriverException ignored) {
            // A native driver may reject a click on a disabled control; that is a valid no-op.
        }

        waitForElementVisible(
                protectedScreenLocator,
                error_message + ": protected screen disappeared after tapping a disabled control",
                timeoutInSeconds
        );
        if (transitionScreenLocator != null && !transitionScreenLocator.trim().isEmpty()) {
            Assert.assertFalse(
                    error_message + ": disabled control triggered an unexpected transition",
                    isElementVisible(transitionScreenLocator)
            );
        }
    }

    public void tapDisabledAndAssertNoTransition(
            String[] disabledButtonLocators,
            String[] protectedScreenLocators,
            String[] transitionScreenLocators,
            String error_message,
            long timeoutInSeconds
    ) {
        WebElement disabledButton = waitForFirstElementPresent(
                disabledButtonLocators,
                error_message + ": disabled control is not visible",
                timeoutInSeconds
        );
        Assert.assertFalse(
                error_message + ": control must be disabled before the no-op tap",
                isElementEnabled(disabledButton)
        );

        try {
            disabledButton.click();
        } catch (WebDriverException ignored) {
            // A native driver may reject a click on a disabled control; that is a valid no-op.
        }

        waitForFirstElementPresent(
                protectedScreenLocators,
                error_message + ": protected screen disappeared after tapping a disabled control",
                timeoutInSeconds
        );
        if (transitionScreenLocators != null) {
            for (String transitionScreenLocator : transitionScreenLocators) {
                if (transitionScreenLocator != null
                        && !transitionScreenLocator.trim().isEmpty()
                        && isElementVisible(transitionScreenLocator)) {
                    Assert.fail(error_message + ": disabled control triggered an unexpected transition");
                }
            }
        }
    }

    public WebDriverWait createWait(long timeoutInSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
        wait.pollingEvery(Duration.ofMillis(250));
        return wait;
    }

    public WebElement waitForElementPresent(String locator, String error_message) {
        return waitForElementPresent(locator, error_message, 5);
    }

    public WebElement waitForFirstElementPresent(
            String[] locators,
            String error_message,
            long timeoutInSeconds
    ) {
        if (locators == null || locators.length == 0) {
            throw new IllegalArgumentException("At least one locator is required");
        }

        WebDriverWait wait = createWait(timeoutInSeconds);
        wait.withMessage(error_message + "\n");
        return wait.until(webDriver -> {
            for (String locator : locators) {
                if (locator == null || locator.trim().isEmpty()) {
                    continue;
                }

                try {
                    WebElement element = ExpectedConditions.visibilityOfElementLocated(
                            getLocatorByString(locator)
                    ).apply(webDriver);
                    if (element != null) {
                        return element;
                    }
                } catch (NoSuchElementException | StaleElementReferenceException ignored) {
                    // Try the next locator during the same polling cycle.
                }
            }
            return null;
        });
    }

    public WebElement waitForElementAndClick(String locator, String error_message, long timeoutInSeconds) {
        WebElement element = waitForElementEnabled(locator, error_message, timeoutInSeconds);
        element.click();
        return element;
    }

    public WebElement waitForFirstElementEnabled(
            String[] locators,
            String error_message,
            long timeoutInSeconds
    ) {
        if (locators == null || locators.length == 0) {
            throw new IllegalArgumentException("At least one locator is required");
        }

        WebDriverWait wait = createWait(timeoutInSeconds);
        wait.withMessage(error_message + "\n");
        return wait.until(webDriver -> {
            for (String locator : locators) {
                if (locator == null || locator.trim().isEmpty()) {
                    continue;
                }

                try {
                    WebElement element = ExpectedConditions.visibilityOfElementLocated(
                            getLocatorByString(locator)
                    ).apply(webDriver);
                    if (element != null && isElementEnabled(element)) {
                        return element;
                    }
                } catch (NoSuchElementException | StaleElementReferenceException ignored) {
                    // Try the next locator during the same polling cycle.
                }
            }
            return null;
        });
    }

    /**
     * Click the first available locator in priority order. This is intended for
     * accessibility-id migrations: the new test id is tried first, while an
     * existing stable accessibility name can remain as a temporary fallback.
     */
    public WebElement waitForFirstElementAndClick(
            String[] locators,
            String error_message,
            long timeoutInSeconds
    ) {
        if (locators == null || locators.length == 0) {
            throw new IllegalArgumentException("At least one locator is required");
        }

        WebElement element = waitForFirstElementEnabled(locators, error_message, timeoutInSeconds);
        element.click();
        return element;
    }

    public boolean waitForFirstElementNotPresent(
            String[] locators,
            String error_message,
            long timeoutInSeconds
    ) {
        if (locators == null || locators.length == 0) {
            throw new IllegalArgumentException("At least one locator is required");
        }

        WebDriverWait wait = createWait(timeoutInSeconds);
        wait.withMessage(error_message + "\n");
        return wait.until(webDriver -> {
            boolean hasUsableLocator = false;
            for (String locator : locators) {
                if (locator == null || locator.trim().isEmpty()) {
                    continue;
                }
                hasUsableLocator = true;
                try {
                    if (!ExpectedConditions.invisibilityOfElementLocated(
                            getLocatorByString(locator)
                    ).apply(webDriver)) {
                        return false;
                    }
                } catch (NoSuchElementException | StaleElementReferenceException ignored) {
                    // An absent/stale locator is already not present.
                }
            }
            return hasUsableLocator;
        });
    }

    /**
     * Kept as a source-compatible alias for legacy page objects. The old
     * coordinate tap was removed because it could hit a neighboring control;
     * all callers now use the element's semantic click target.
     */
    @Deprecated
    public WebElement waitForElementAndTapNearLeftEdge(String locator, String error_message, long timeoutInSeconds) {
        return waitForElementAndClick(locator, error_message, timeoutInSeconds);
    }

    public WebElement waitForElementAndSendKeys(String locator, String value, String error_message, long timeoutInSeconds) {
        WebElement element = waitForElementPresent(locator, error_message, timeoutInSeconds);
        element.sendKeys(value);
        return element;
    }

    public boolean waitForElementNotPresent(String locator, String error_message, long timeoutInSeconds) {
        By by = this.getLocatorByString(locator);
        WebDriverWait wait = createWait(timeoutInSeconds);
        wait.withMessage(error_message + "\n");
        return wait.until(
                ExpectedConditions.invisibilityOfElementLocated(by)
        );
    }

    public WebElement waitForElementAndClear(String locator, String error_message, long timeoutInSeconds) {
        WebElement element = waitForElementPresent(locator, error_message, timeoutInSeconds);
        element.clear();
        return (element);
    }

    public void swipeUp(int timeOfSwipe) {
        if (driver instanceof PerformsTouchActions) {
            try {
                Map<String, Object> args = new HashMap<String, Object>();
                args.put("direction", "up");
                ((JavascriptExecutor) driver).executeScript("mobile: swipe", args);
            } catch (WebDriverException e) {
                TouchAction action = new TouchAction((PerformsTouchActions) driver);
                Dimension size = driver.manage().window().getSize();
                int x = size.width / 2;
                int start_y = (int) (size.height * 0.8);
                int end_y = (int) (size.height * 0.2);
                action
                        .press(PointOption.point(x, start_y))
                        .waitAction(WaitOptions.waitOptions(Duration.ofMillis(timeOfSwipe)))
                        .moveTo(PointOption.point(x, end_y))
                        .release()
                        .perform();
            }
        } else {
            System.out.println("Method swipeUp does nothing for platform " + Platform.getInstance().getPlatformVar());
        }
    }

    public void swipeUpQuick() {
        swipeUp(200);
    }

    public void scrollWebPAgeUp() {
        if (Platform.getInstance().isMw()) {
            JavascriptExecutor JSExecutor = (JavascriptExecutor) driver;
            JSExecutor.executeScript("window.scrollBy(0,250)");
        } else {
            System.out.println("Method scrollWebPAgeUp() does nothing for platform " + Platform.getInstance().getPlatformVar());
        }
    }

    public void scrollWebPageTillElementNotVisible(String locator, String error_message, int max_swipes) {
        int already_swiped = 0;
        WebElement element = this.waitForElementPresent(locator, error_message);
        while (!this.isElementLocatedOnTheScreen(locator)) {
            scrollWebPAgeUp();
            ++already_swiped;
            if (already_swiped > max_swipes)
                Assert.assertTrue(error_message, element.isDisplayed());
        }
    }

    public void swipeUpToFindElement(String locator, String error_message, int max_swipes) {
        By by = this.getLocatorByString(locator);
        int already_swiped = 0;
        while (driver.findElements(by).size() == 0) {
            if (already_swiped > max_swipes) {
                waitForElementPresent(locator, "Cannot find element by swiping up.\n" + error_message, 0);
                return;
            }
            swipeUpQuick();
            ++already_swiped;
        }
    }

    public void swipeUpToFindFirstElement(
            String[] locators,
            String error_message,
            int max_swipes
    ) {
        if (locators == null || locators.length == 0) {
            throw new IllegalArgumentException("At least one locator is required");
        }

        int alreadySwiped = 0;
        while (!hasAnyElement(locators) && alreadySwiped <= max_swipes) {
            swipeUpQuick();
            alreadySwiped++;
        }
        waitForFirstElementPresent(
                locators,
                "Cannot find element by swiping up.\n" + error_message,
                5
        );
    }

    /**
     * Scroll until one of the supplied locators is actually visible. Native
     * accessibility trees may contain off-screen collection cells, so a
     * presence-only search is not sufficient before a tap.
     */
    public void swipeUpToFindFirstVisibleElement(
            String[] locators,
            String error_message,
            int max_swipes
    ) {
        if (locators == null || locators.length == 0) {
            throw new IllegalArgumentException("At least one locator is required");
        }

        int alreadySwiped = 0;
        while (!hasAnyVisibleElement(locators) && alreadySwiped <= max_swipes) {
            swipeUpQuick();
            alreadySwiped++;
        }
        waitForFirstElementPresent(
                locators,
                "Cannot find a visible element by swiping up.\n" + error_message,
                5
        );
    }

    private boolean hasAnyElement(String[] locators) {
        for (String locator : locators) {
            if (locator != null && !locator.trim().isEmpty() && isElementPresent(locator)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyVisibleElement(String[] locators) {
        for (String locator : locators) {
            if (locator != null && !locator.trim().isEmpty() && isElementVisible(locator)) {
                return true;
            }
        }
        return false;
    }

    public void swipeUpTillElementAppear(String locator, String error_message, int max_swipes) {
        int already_swiped = 0;
        while (this.isElementLocatedOnTheScreen(locator)) {
            if (already_swiped > max_swipes) {
                Assert.assertTrue(error_message, this.isElementLocatedOnTheScreen(locator));
            }
        }
        swipeUpQuick();
        ++already_swiped;
    }

    public boolean isElementLocatedOnTheScreen(String locator) {
        int element_location_by_y = this.waitForElementPresent(locator, "Cannot find element by locator", 1).getLocation().getY();
        if (Platform.getInstance().isMw()) {
            JavascriptExecutor JSExecutor = (JavascriptExecutor) driver;
            Object js_result = JSExecutor.executeScript("return window.pageYOffset");
            element_location_by_y -= Integer.parseInt(js_result.toString());
        }
        int screen_size_by_y = driver.manage().window().getSize().getHeight(); //длина всего экрана
        return element_location_by_y < screen_size_by_y;
    }

    public void clickElementToTheRightUpperCorner(String locator, String error_message) {
        if (driver instanceof PerformsTouchActions) {
            WebElement element = this.waitForElementPresent(locator + "/..", error_message);
            int right_x = element.getLocation().getX();
            int upper_y = element.getLocation().getY();
            int lower_y = upper_y + element.getSize().getHeight();
            int middle_y = (upper_y + lower_y) / 2;
            int width = element.getSize().getWidth();

            int point_to_click_x = (right_x + width) - 3;
            int point_to_click_y = middle_y;

            TouchAction action = new TouchAction((PerformsTouchActions) driver);
            action.tap(PointOption.point(point_to_click_x, point_to_click_y)).perform();
        } else {
            System.out.println("Method clickElementToTheRightUpperCorner does nothing for platform " + Platform.getInstance().getPlatformVar());
        }
    }

    public void swipeElementToLeft(String locator, String error_message) {
        if (driver instanceof PerformsTouchActions) {
            WebElement element = waitForElementPresent(
                    locator,
                    error_message,
                    10);

            int left_x = element.getLocation().getX();
            int right_x = left_x + element.getSize().getWidth();
            int upper_y = element.getLocation().getY();
            int lower_y = upper_y + element.getSize().getHeight();
            int middle_y = (upper_y + lower_y) / 2;

            TouchAction action = new TouchAction((PerformsTouchActions) driver);
            action.press(PointOption.point(right_x, middle_y));
            action.waitAction(WaitOptions.waitOptions(Duration.ofMillis(300)));

            if (Platform.getInstance().isAndroid()) {
                action.moveTo(PointOption.point(left_x, middle_y));
            } else {
                int offset_x = (-1 * element.getSize().getWidth());
                action.moveTo(PointOption.point(offset_x, 0));
            }
            action.release();
            action.perform();
        } else {
            System.out.println("Method swipeElementToLeft does nothing for platform " + Platform.getInstance().getPlatformVar());
        }
    }

    public int getAmountElements(String locator) {
        By by = this.getLocatorByString(locator);
        List elements = driver.findElements(by);
        return elements.size();
    }

    /**
     * Read accessibility names from all matching elements in tree order. This
     * is intentionally separate from getAmountElements: program catalog tests
     * need the complete collection, including items that are outside the
     * viewport but already present in the native accessibility tree.
     */
    public List<String> getElementAccessibleNames(String locator) {
        if (locator == null || locator.trim().isEmpty()) {
            throw new IllegalArgumentException("A non-empty locator is required");
        }

        List<String> names = new ArrayList<String>();
        By by = this.getLocatorByString(locator);
        for (WebElement element : driver.findElements(by)) {
            String name = getElementAccessibleName(element);
            if (name != null && !name.trim().isEmpty()) {
                names.add(name.trim());
            }
        }
        return names;
    }

    /**
     * Return the most useful native accessibility value for an element across
     * XCUITest and UiAutomator2. The fallback order avoids using an
     * implementation-specific text representation when a semantic name exists.
     */
    public String getElementAccessibleName(WebElement element) {
        if (element == null) {
            return "";
        }

        // A collection item can expose one shared accessibility identifier in
        // `name` and its actual program title in `label`. Prefer user-facing
        // values first so generic item ids do not collapse the whole list to a
        // single string.
        String[] attributes = new String[]{"label", "text", "name", "value", "content-desc"};
        for (String attribute : attributes) {
            try {
                String value = element.getAttribute(attribute);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            } catch (Exception ignored) {
                // Attribute availability differs between native drivers.
            }
        }

        try {
            String text = element.getText();
            return text == null ? "" : text.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    public boolean isElementPresent(String locator) {
        return getAmountElements(locator) > 0;
    }

    /**
     * Kept for legacy non-Coach flows. The action is deliberately executed
     * once; retrying a tap can submit a form or navigate twice.
     */
    @Deprecated
    public void tryClickElementWithFewAttempts(String locator, String error_message, int among_of_attempts) {
        this.waitForElementAndClick(locator, error_message, Math.max(1, among_of_attempts));
    }

    public void assertElementNotPresent(String locator, String error_message) {
        int amount_of_elements = getAmountElements(locator);
        if (amount_of_elements > 0) {
            String default_message = "An element '" + locator + "' supposed to be not present";
            throw new AssertionError(default_message + " " + error_message);
        }
    }

    public String waitForElementAndGetAttribute(String locator, String attribute, String error_message, long timeOutInSeconds) {
        WebElement element = waitForElementPresent(locator, error_message, timeOutInSeconds);
        return element.getAttribute(attribute);
    }

    protected By getLocatorByString(String locator_with_type) {
        String[] exploded_locator = locator_with_type.split(Pattern.quote(":"), 2); //записывает в переменную exploded
        String by_type = exploded_locator[0];
        String locator = exploded_locator[1];

        if (by_type.equals("xpath")) {
            return By.xpath(locator);
        } else if (by_type.equals("id")) {
            return By.id(locator);
        } else if (by_type.equals("css")) {
            return By.cssSelector(locator);
        } else {
            throw new IllegalArgumentException("Cannot get type of locator. Locator " + locator_with_type);
        }
    }

    //метод создания скриншота
    public String takeScreenshot(String name) {
        TakesScreenshot ts = (TakesScreenshot) this.driver;
        File source = ts.getScreenshotAs(OutputType.FILE);
        String path = System.getProperty("user.dir") + "/" + name + "_screenshot.png";
        try {
            FileUtils.copyFile(source, new File(path));
            System.out.println("The screenshot was taken: " + path);
        } catch (Exception e) {
            System.out.println("Cannot take screenshot. Error: " + e.getMessage());
        }
        return path;
    }

    //метод для добавления скриншота в отчет
    @Attachment
    public static byte[] screenshot(String path) {
        byte[] bytes = new byte[0];
        try{
            bytes = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            System.out.println("Cannot get Bytes from screenshot.Error: " + e.getMessage());
        }
        return bytes;
    }
}
