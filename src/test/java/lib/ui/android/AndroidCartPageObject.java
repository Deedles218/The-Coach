package lib.ui.android;

import io.qameta.allure.Step;
import lib.Platform;
import lib.ui.CoachFlowPageObject;
import lib.ui.MainPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** The embedded Store owns cart items; the app's native tab owns the indicator. */
public final class AndroidCartPageObject extends MainPageObject {
    private static final String WEB = "//android.webkit.WebView";
    private static final String LOGO = WEB + "//android.view.View[@clickable='true' and starts-with(@content-desc,'logo-primary-')]";
    private static final String CART = "xpath:" + LOGO + "/../android.view.View[@clickable='true' and "
            + "(@content-desc='cart' or ./android.widget.TextView[string-length(@text)>0 and translate(@text,'0123456789','')=''])]";
    private static final String DRAWER_CLOSE = "xpath:" + WEB + "//android.widget.Button[@text='Close' or @content-desc='Close']";
    private static final String EMPTY = "xpath:" + WEB + "//android.widget.TextView[@text='Your cart is empty']";
    private static final String TOTAL = "xpath:" + WEB + "//android.widget.TextView[@text='Total']";
    private static final String FOR_HIM = "xpath:" + WEB + "//android.view.View[@clickable='true' and @content-desc='For Him']";
    private static final String ADD_TO_CART = "xpath:" + WEB + "//android.widget.Button[@clickable='true' and "
            + "(starts-with(@text,'Add to cart') or starts-with(@content-desc,'Add to cart'))]";
    private final CoachFlowPageObject coach;
    private final String id;

    public AndroidCartPageObject(RemoteWebDriver driver) {
        super(driver);
        id = "id:" + Platform.getInstance().getAndroidAppPackage() + ":id/";
        coach = CoachFlowPageObjectFactory.get(driver);
    }

    @Step("Confirm the QA cart is empty in Store")
    public void assertEmptyCart() {
        openCart();
        waitForElementVisible(EMPTY, "The cart must be empty before this scenario", 15);
        closeCart();
    }

    @Step("Add one {product} through the Store")
    public void addFixtureItem(String product) {
        coach.openShop();
        waitForElementVisible("xpath:" + WEB, "Store WebView did not open", 25);
        reveal("xpath:" + LOGO, "down");
        waitForElementAndClick("xpath:" + LOGO, "Cannot return to the Store home page", 10);
        waitForElementAndClick(FOR_HIM, "Store has no For Him category", 25);
        waitForElementVisible("xpath:" + WEB + "//android.widget.TextView[@text='Bundles & New Arrivals']",
                "The For Him catalog did not finish loading", 25);
        String card = "xpath:" + WEB + "//android.view.View[@clickable='true' and "
                + "contains(@content-desc," + literal(product) + ") and contains(@content-desc,'SHOP NOW')]";
        reveal(card, "up");
        waitForElementAndClick(card, "Cannot open the fixture product", 15);
        waitForElementVisible("xpath:" + WEB + "//android.widget.TextView[@text=" + literal(product) + "]",
                "A different product opened", 20);
        reveal(ADD_TO_CART, "up");
        waitForElementAndClick(ADD_TO_CART, "Cannot add the fixture product to the cart", 10);
        waitForElementVisible(TOTAL, "Adding the product did not open the cart", 20);
        waitForElementVisible(singleItemRow(product), "The cart did not contain one unit of the fixture product", 15);
        closeCart();
    }

    @Step("Remove the fixture item and confirm the Store cart is empty")
    public void removeFixtureItemIfPresent(String product) {
        openCart();
        if (!isElementVisible(EMPTY)) {
            String row = singleItemRow(product);
            waitForElementVisible(row, "Cleanup refused to remove an unknown cart item or quantity", 12);
            // The trash button has no accessible name on 356. Its wrapper
            // contains just the button; the separate quantity wrapper has
            // two buttons and a count. Keep the lookup inside this exact row.
            String remove = row + "/android.view.View[count(./*)=1]/android.widget.Button"
                    + "[@clickable='true' and string-length(@text)=0 and string-length(@content-desc)=0]";
            List<WebElement> buttons = driver.findElements(getLocatorByString(remove)).stream()
                    .filter(WebElement::isDisplayed).collect(Collectors.toList());
            Assert.assertEquals("Expected exactly one remove action in the fixture cart row", 1, buttons.size());
            buttons.get(0).click();
        }
        waitForElementVisible(EMPTY, "The Store cart did not become empty", 20);
        closeCart();
    }

    @Step("Check the native Shop tab's cart indicator: {present}")
    public void assertShopBadge(boolean present) {
        createWait(30).withMessage("The native Shop cart badge did not become " + (present ? "visible" : "absent"))
                .until(ignored -> {
                    for (WebElement tab : driver.findElements(getLocatorByString(id + "nav_graph_shop"))) {
                        if (tab.isDisplayed()) {
                            String label = tab.getAttribute("content-desc");
                            if (present ? label != null && label.matches("Shop, [1-9][0-9]* new notifications?")
                                    : "Shop".equals(label)) return true;
                        }
                    }
                    return false;
                });
    }

    @Step("Confirm no abandoned-cart banner is visible on the loaded tab")
    public void assertBannerAbsent(String tab) {
        Assert.assertTrue("Observe either Today or Explore before checking the cart banner",
                "daily".equals(tab) || "explore".equals(tab));
        String selectedTab = "xpath://android.widget.FrameLayout[@resource-id='" + id.substring(3)
                + "nav_graph_" + tab + "' and @selected='true']";
        waitForElementVisible(selectedTab, "The expected tab is not selected", 15);
        waitForElementVisible(id + ("daily".equals(tab) ? "rvQuestions" : "catalogContainer"),
                "The selected tab has not loaded its content", 25);
        waitForElementNotVisible(id + "pbLoading", "The selected tab is still loading", 20);
        // Observe the loaded tab for late banner rendering, rather than accepting a single empty frame.
        long observeUntil = System.nanoTime() + java.time.Duration.ofSeconds(4).toNanos();
        createWait(5).until(ignored -> {
            Assert.assertTrue("The selected tab changed during the banner check", isElementVisible(selectedTab));
            Assert.assertFalse("Abandoned-cart banner is visible with an empty cart",
                    isElementVisible(id + "abandoned_cart_banner"));
            return System.nanoTime() >= observeUntil;
        });
    }

    private void openCart() {
        coach.openShop();
        waitForElementVisible("xpath:" + WEB, "Store WebView did not open", 25);
        if (!isElementVisible(DRAWER_CLOSE)) {
            reveal(CART, "down");
            waitForElementAndClick(CART, "Cannot open the Store cart from its header", 15);
        }
        createWait(20).withMessage("Store cart did not show its item total or empty state")
                .until(ignored -> isElementVisible(DRAWER_CLOSE) && (isElementVisible(EMPTY) || isElementVisible(TOTAL)));
    }

    private void closeCart() {
        waitForElementAndClick(DRAWER_CLOSE, "Cannot close the Store cart", 10);
        waitForElementNotVisible(DRAWER_CLOSE, "Store cart remained open", 10);
    }

    private String singleItemRow(String product) {
        return "xpath:" + WEB + "//android.view.View[./android.view.View[@clickable='true' and @content-desc="
                + literal(product) + "] and ./android.view.View[./android.widget.TextView[@text='1']"
                + " and count(./android.widget.Button)=2]]";
    }

    private void reveal(String locator, String swipeDirection) {
        WebElement container = waitForElementVisible(id + "webview_container", "Store container is absent", 20);
        Map<String, Object> swipe = new HashMap<>();
        swipe.put("elementId", ((RemoteWebElement) container).getId());
        swipe.put("direction", swipeDirection);
        swipe.put("percent", .68);
        for (int i = 0; i < 8 && !isElementVisible(locator); i++) {
            driver.executeScript("mobile: swipeGesture", swipe);
        }
        waitForElementVisible(locator, "Expected Store control did not become visible", 15);
    }

    private static String literal(String text) {
        Assert.assertFalse("The reviewed Store fixture title must not contain an apostrophe", text.contains("'"));
        return "'" + text + "'";
    }
}
