package lib.ui.ios;

import io.qameta.allure.Step;
import lib.ModuleStage;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.util.Arrays;
import java.util.List;

/** Locators observed in the saved DailyDaySwitcherView accessibility tree.
 * English locale, portrait, standard text size; no exact module-title oracle.
 */
public final class iOSModulesPageObject extends iOSDailyPlanPageObject {
    private static final String BLOCK = "id:DailyDaySwitcherView";
    private static final String MODULE = "xpath://XCUIElementTypeOther[@name='DailyDaySwitcherView']"
            + "/XCUIElementTypeStaticText[starts-with(@name,'Module ')]";
    private static final String STAGE = "xpath://XCUIElementTypeOther[@name='DailyDaySwitcherView']"
            + "/XCUIElementTypeStaticText[starts-with(@name,'Stage ')]";
    private static final String OLD_DAY = "xpath://XCUIElementTypeOther[@name='DailyDaySwitcherView']"
            + "/XCUIElementTypeStaticText[starts-with(@name,'Day ')]";

    public iOSModulesPageObject(RemoteWebDriver driver) { super(driver); }

    @Override public void openTodayTab() {
        new iOSCoachFlowPageObject(driver).closePartnerPromoIfPresent();
        super.openTodayTab();
    }

    public ModuleStage position() {
        WebElement title = waitForElementVisible(MODULE, "Module title is absent", 10);
        WebElement stage = waitForElementVisible(STAGE, "Stage is absent", 10);
        Assert.assertFalse("Old Day label is visible alongside Stage", isElementVisible(OLD_DAY));
        return ModuleStage.parse(getElementAccessibleName(title), getElementAccessibleName(stage));
    }

    @Step("Wait for exact module and stage")
    public void waitForPosition(ModuleStage expected) {
        createWait(15).ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class)
                .withMessage("Today did not reach " + expected).until(ignored -> {
                    // During a request Today temporarily removes the header.
                    // Poll within this single deadline; position() has its own
                    // nested timeout and could abort the outer wait prematurely.
                    WebElement title = driver.findElement(getLocatorByString(MODULE));
                    WebElement stage = driver.findElement(getLocatorByString(STAGE));
                    return title.isDisplayed() && stage.isDisplayed()
                            && expected.equals(ModuleStage.parse(getElementAccessibleName(title),
                            getElementAccessibleName(stage)));
                });
        Assert.assertFalse("Old Day label is visible alongside Stage", isElementVisible(OLD_DAY));
        Assert.assertFalse("Unexpected blocking popup after navigation", isElementVisible(LOCKED_MODULE_POPUP_BUTTON));
    }

    public long next() { return tap(RIGHT_SWITCHER_ARROW); }
    public long previous() { return tap(LEFT_SWITCHER_ARROW); }
    private long tap(String locator) {
        WebElement arrow = waitForElementEnabled(locator, "Navigation arrow unavailable", 10);
        long boundary = System.currentTimeMillis();
        arrow.click();
        return boundary;
    }

    @Step("Blocking popup is visible and current stage is retained")
    public void assertBlocked(ModuleStage before) {
        // A visible GOT IT control plus the observed module popup identifies
        // this dialog; no exact content, GIF or Remote Config comparison.
        waitForElementVisible(LOCKED_MODULE_POPUP_TITLE, "Module blocking popup is absent", 10);
        waitForElementEnabled(LOCKED_MODULE_POPUP_BUTTON, "GOT IT is unavailable", 10);
        // The underlying header may be inaccessible while the modal is open.
        closeLockedModulePopup();
        Assert.assertEquals("Blocked transition changed module or stage", before, position());
    }

    @Step("Restore the original viewed stage without completing activities")
    public void restore(ModuleStage original) {
        if (isElementVisible(LOCKED_MODULE_POPUP_BUTTON)) closeLockedModulePopup();
        ModuleStage current = position();
        Assert.assertEquals("Restoration cannot cross an unexpected module", original.module, current.module);
        Assert.assertEquals("Module boundary changed", original.total, current.total);
        int budget = original.total;
        while (!original.equals(current) && budget-- > 0) {
            int target = current.stage + (current.stage < original.stage ? 1 : -1);
            if (target > current.stage) next(); else previous();
            waitForPosition(current.at(target));
            current = position();
        }
        Assert.assertEquals("Original viewed stage was not restored", original, current);
    }

    /** Geometric subset only: accessibility bounds cannot prove glyph clipping,
     * safe-area insets or occlusion by arbitrary views. See coverage document. */
    @Step("Module header elements fit their container and do not overlap")
    public void assertHeaderBounds() {
        Rectangle block = waitForElementVisible(BLOCK, "Module block is absent", 10).getRect();
        Dimension screen = driver.manage().window().getSize();
        assertContained(new Rectangle(0, 0, screen.height, screen.width), block);
        List<String> locators = Arrays.asList(MODULE, STAGE, LEFT_SWITCHER_ARROW, RIGHT_SWITCHER_ARROW);
        Rectangle[] bounds = new Rectangle[locators.size()];
        for (int i = 0; i < bounds.length; i++) {
            bounds[i] = waitForElementVisible(locators.get(i), "Header element missing", 10).getRect();
            assertContained(block, bounds[i]);
        }
        for (int i = 0; i < bounds.length; i++) for (int j = i + 1; j < bounds.length; j++) {
            Assert.assertFalse("Module title, Stage or arrows overlap", overlaps(bounds[i], bounds[j]));
        }
    }

    public static void assertContained(Rectangle outer, Rectangle inner) {
        Assert.assertTrue("Header element has empty bounds or extends outside its container",
                inner.width > 0 && inner.height > 0 && inner.x >= outer.x && inner.y >= outer.y
                && inner.x + inner.width <= outer.x + outer.width
                && inner.y + inner.height <= outer.y + outer.height);
    }
    public static boolean overlaps(Rectangle a, Rectangle b) {
        return Math.max(a.x, b.x) < Math.min(a.x + a.width, b.x + b.width)
                && Math.max(a.y, b.y) < Math.min(a.y + a.height, b.y + b.height);
    }
}
