package lib.ui.android;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import lib.ui.SubscriptionPageObject;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AndroidSubscriptionPageObject extends SubscriptionPageObject {
    private static final String APP_PACKAGE = "com.vamapps.thecoach";
    private static final String ID_PREFIX = APP_PACKAGE + ":id/";

    private static final String PAYWALL = "id:" + ID_PREFIX + "clPaywallContainer";
    private static final String CONTINUE_BUTTON = "id:" + ID_PREFIX + "btnContinue";
    private static final String RESTORE_BUTTON = "id:" + ID_PREFIX + "btnRestore";
    private static final String POLICY_TEXT = "id:" + ID_PREFIX + "tvSubscriptionPolicy";
    private static final String PRIVACY_TEXT = "id:" + ID_PREFIX + "tvPrivacy";
    private static final String TODAY_TAB = "id:" + ID_PREFIX + "nav_graph_daily";
    private static final By PLAN_CARD = By.id(ID_PREFIX + "layoutSubscription");
    private static final By SELECTED_RADIO = By.id(ID_PREFIX + "rbChecked");
    private static final By TERMS_DESTINATION = By.xpath(
            "//android.webkit.WebView "
                    + "| //*[(contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'privacy') "
                    + "or contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'terms')) "
                    + "and not(ancestor-or-self::*[@resource-id='" + ID_PREFIX + "clPaywallContainer'])]"
    );
    private static final By RESTORE_RESULT = By.xpath(
            "//android.widget.Toast "
                    + "| //android.app.Dialog "
                    + "| //*[(contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'restor') "
                    + "or contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no active') "
                    + "or contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'not found') "
                    + "or contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'expired')) "
                    + "and @resource-id!='" + ID_PREFIX + "btnRestore']"
    );
    private static final String[] PLAY_PURCHASE_MARKERS = new String[]{
            "xpath://*[@text='Subscribe' or @text='SUBSCRIBE' or @text='Buy' or @text='BUY' "
                    + "or @text='Start free trial' or @text='START FREE TRIAL']"
    };
    private static final String[] PLAY_CONFIRM_BUTTONS = new String[]{
            "xpath://*[@text='Subscribe' or @text='SUBSCRIBE']",
            "xpath://*[@text='Start free trial' or @text='START FREE TRIAL']",
            "xpath://*[@text='Buy' or @text='BUY']"
    };

    public AndroidSubscriptionPageObject(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Verify Android subscription paywall is fully loaded")
    public void assertPaywallIsDisplayed() {
        waitForElementPresent(PAYWALL, "Android subscription paywall is not displayed", 30);
        createWait(45).withMessage(
                "Android paywall did not load at least two subscription plans. "
                        + "Use a Google Play system image and an approved license-tester account.\n"
        ).until(webDriver -> visiblePlanCards().size() >= 2);
        waitForElementEnabled(CONTINUE_BUTTON, "Android paywall Continue button is not enabled", 15);
    }

    @Override
    @Step("Select {position} Android subscription plan")
    public void selectPlan(final PlanPosition position) {
        List<WebElement> plans = visiblePlanCards();
        Assert.assertTrue("Android paywall must expose at least two plans", plans.size() >= 2);
        WebElement plan = planAtPosition(plans, position);
        Assert.assertTrue("Selected Android plan card must be enabled", isElementEnabled(plan));
        plan.click();
        createWait(10).withMessage(position + " Android plan did not become selected\n")
                .until(webDriver -> {
                    List<WebElement> currentPlans = visiblePlanCards();
                    return currentPlans.size() >= 2
                            && isPlanSelected(planAtPosition(currentPlans, position));
                });
    }

    @Override
    @Step("Open Google Play license-tester purchase sheet")
    public void startPurchaseAndWaitForStore() {
        waitForElementAndClick(CONTINUE_BUTTON, "Cannot tap Continue on Android paywall", 15);
        createWait(30).withMessage("Google Play purchase sheet did not open\n")
                .until(webDriver -> isGooglePlayForeground() && hasAnyVisible(PLAY_PURCHASE_MARKERS));
    }

    @Override
    @Step("Confirm Google Play license-tester purchase")
    public void confirmTestPurchase() {
        createWait(20).withMessage(
                "Google Play dialog is not visibly marked as a test purchase; confirmation is blocked to prevent a real charge\n"
        ).until(webDriver -> hasTestPurchaseMarker());
        waitForFirstElementAndClick(
                PLAY_CONFIRM_BUTTONS,
                "Cannot confirm Google Play test purchase. Verify the account is a license tester and the dialog says this is a test purchase.",
                20
        );
    }

    @Override
    @Step("Verify Android premium access after purchase")
    public void assertPurchaseCompleted() {
        createWait(45).withMessage(
                "Android purchase did not close the paywall and open premium Today content\n"
        ).until(webDriver -> {
            String source = normalizeWhitespace(driver.getPageSource()).toLowerCase(Locale.US);
            if (source.contains("billing system is currently paused")) {
                Assert.fail(
                        "Google Play blocked the license-tester transaction because billing is paused "
                                + "for the account/device region"
                );
            }
            return isElementVisible(TODAY_TAB);
        });
    }

    @Override
    @Step("Open Android paywall Terms & Privacy")
    public void openTermsAndPrivacy() {
        WebElement disclosure = waitForElementPresent(
                POLICY_TEXT,
                "Android Terms & Privacy disclosure is not available",
                15
        );
        disclosure.click();
        try {
            createWait(3).until(webDriver -> termsDestinationIsVisible());
            return;
        } catch (TimeoutException ignored) {
            // In 1.40.8 the links are ClickableSpans inside one TextView and
            // UiAutomator2 does not expose either span as a child element.
        }

        Rectangle rectangle = disclosure.getRect();
        Map<String, Object> tap = new HashMap<String, Object>();
        tap.put("x", rectangle.getX() + (rectangle.getWidth() * 45) / 100);
        tap.put("y", rectangle.getY() + (rectangle.getHeight() * 84) / 100);
        ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", tap);
    }

    @Override
    @Step("Verify Android Terms & Privacy destination")
    public void assertTermsAndPrivacyOpened() {
        createWait(20).withMessage("Android Terms & Privacy destination did not open\n")
                .until(webDriver -> termsDestinationIsVisible());
    }

    @Override
    @Step("Verify exact Android auto-renewal disclosure")
    public void assertAutoRenewalDisclosure() {
        createWait(20).withMessage("Android subscription disclosure did not load\n")
                .until(webDriver -> !combinedDisclosureText().isEmpty());
        Assert.assertEquals(
                "Unexpected Android subscription auto-renewal disclosure",
                EXPECTED_AUTO_RENEWAL_DISCLOSURE,
                combinedDisclosureText()
        );
    }

    @Override
    @Step("Verify Android Restore button")
    public void assertRestoreButtonIsAvailable() {
        WebElement restore = waitForElementEnabled(
                RESTORE_BUTTON,
                "Android Restore button is not visible and enabled",
                20
        );
        Assert.assertEquals(
                "Unexpected Android Restore label",
                "RESTORE",
                normalizedText(restore).toUpperCase(Locale.US)
        );
    }

    @Override
    @Step("Tap Android Restore and verify response")
    public void tapRestoreAndAssertResult() {
        waitForElementAndClick(RESTORE_BUTTON, "Cannot tap Android Restore", 15);
        createWait(35).withMessage("Android Restore did not produce restored access or an explicit result\n")
                .until(webDriver -> isElementVisible(TODAY_TAB)
                        || !driver.findElements(RESTORE_RESULT).isEmpty());
    }

    private List<WebElement> visiblePlanCards() {
        List<WebElement> visible = new ArrayList<WebElement>();
        for (WebElement plan : driver.findElements(PLAN_CARD)) {
            try {
                if (plan.isDisplayed()) {
                    visible.add(plan);
                }
            } catch (StaleElementReferenceException ignored) {
                // The paywall can rerender when Google Play product details arrive.
            }
        }
        Collections.sort(visible, new Comparator<WebElement>() {
            @Override
            public int compare(WebElement first, WebElement second) {
                return Integer.compare(first.getRect().getY(), second.getRect().getY());
            }
        });
        return visible;
    }

    private WebElement planAtPosition(List<WebElement> plans, PlanPosition position) {
        return position == PlanPosition.TOP ? plans.get(0) : plans.get(plans.size() - 1);
    }

    private boolean isPlanSelected(WebElement plan) {
        for (WebElement radio : plan.findElements(SELECTED_RADIO)) {
            String checked = radio.getAttribute("checked");
            String selected = radio.getAttribute("selected");
            if ("true".equalsIgnoreCase(checked)
                    || "true".equalsIgnoreCase(selected)
                    || "1".equals(checked)
                    || "1".equals(selected)) {
                return true;
            }
        }
        return false;
    }

    private String combinedDisclosureText() {
        String policy = firstVisibleText(By.id(ID_PREFIX + "tvSubscriptionPolicy"));
        String privacy = firstVisibleText(By.id(ID_PREFIX + "tvPrivacy"));
        if (policy.contains("Privacy Policy") && policy.contains("Terms of Service")) {
            return policy;
        }
        return normalizeWhitespace(policy + " " + privacy);
    }

    private String firstVisibleText(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed()) {
                    return normalizedText(element);
                }
            } catch (StaleElementReferenceException ignored) {
                // Poll again after the dynamic paywall rerenders.
            }
        }
        return "";
    }

    private boolean hasAnyVisible(String[] locators) {
        for (String locator : locators) {
            if (isElementVisible(locator)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGooglePlayForeground() {
        return "com.android.vending".equals(currentAndroidPackage());
    }

    private boolean termsDestinationIsVisible() {
        return !APP_PACKAGE.equals(currentAndroidPackage())
                || !driver.findElements(TERMS_DESTINATION).isEmpty();
    }

    private boolean hasTestPurchaseMarker() {
        String purchaseSheetSource = normalizeWhitespace(driver.getPageSource()).toLowerCase(Locale.US);
        return purchaseSheetSource.contains("test purchase")
                || purchaseSheetSource.contains("test order")
                || purchaseSheetSource.contains("test card")
                || purchaseSheetSource.contains("test instrument");
    }

    private String currentAndroidPackage() {
        if (driver instanceof AndroidDriver) {
            return ((AndroidDriver) driver).getCurrentPackage();
        }
        return "";
    }
}
