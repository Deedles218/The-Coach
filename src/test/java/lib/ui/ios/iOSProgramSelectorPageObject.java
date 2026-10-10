package lib.ui.ios;

import io.qameta.allure.Allure;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.*;

/** Additional selector observations; navigation and collection reading stay in DailyPlan. */
public final class iOSProgramSelectorPageObject extends iOSDailyPlanPageObject {
    private static final String CARDS = "id:ProgramSelectionCardView";
    private static final String TITLES = "xpath://XCUIElementTypeOther[@name='ProgramSelectionCardView']//XCUIElementTypeStaticText[@name='TitleBlock.Title']";
    private static final String COLLECTION = "xpath://XCUIElementTypeCollectionView[.//XCUIElementTypeOther[@name='ProgramSelectionCardView']]";

    public iOSProgramSelectorPageObject(RemoteWebDriver driver) { super(driver); }

    public Rectangle firstCardBounds() {
        waitForElementPresent(CARDS, "Selector is not open", 10);
        return driver.findElements(getLocatorByString(CARDS)).stream().filter(WebElement::isDisplayed)
                .map(WebElement::getRect).min(Comparator.comparingInt(r -> r.y))
                .orElseThrow(() -> new AssertionError("No visible program cards"));
    }

    public void dismissSelectorIfOpen() {
        if (isElementVisible(CARDS)) closeProgramSelector();
    }

    /** Scroll only inside the observed selector collection to a fixture title. */
    public void selectProgram(String title) {
        Assert.assertTrue("Only the reviewed selector fixture pair may be changed",
                "Last Longer".equals(title) || "Keep It Hard".equals(title));
        selectExactProgram(title);
    }

    /** Explicit customization-account navigation; never changes days or completes cards. */
    public void selectCustomizationInspectionProgram(String title) {
        Assert.assertTrue("Unsupported customization inspection program",
                "Overall Health".equals(title) || "Kegel Challenge".equals(title) || "Last Longer: Retain".equals(title));
        selectExactProgram(title);
    }

    public void selectExactProgram(String title) {
        String target = "xpath://XCUIElementTypeOther[@name='ProgramSelectionCardView']"
                + "//XCUIElementTypeStaticText[@name='TitleBlock.Title' and @label="
                + org.openqa.selenium.support.ui.Quotes.escape(title) + "]";
        for (int scroll = 0; scroll < 15; scroll++) {
            if (isElementVisible(target)) {
                waitForElementAndClick(target, "Cannot select fixture program " + title, 10);
                waitForElementNotPresent(CARDS, "Selector did not close after selecting " + title, 15);
                return;
            }
            Rectangle collection = waitForElementPresent(COLLECTION, "Selector is not open", 10).getRect();
            Map<String,Object> args = new HashMap<String,Object>();
            args.put("duration", .5);
            args.put("fromX", collection.x + collection.width / 2);
            args.put("toX", collection.x + collection.width / 2);
            args.put("fromY", collection.y + (int)(collection.height * .8));
            args.put("toY", collection.y + (int)(collection.height * .4));
            ((JavascriptExecutor)driver).executeScript("mobile: dragFromToForDuration", args);
        }
        throw new AssertionError("Fixture program is not available in selector: " + title);
    }

    public void waitForSelectedProgram(String title) {
        new WebDriverWait(driver, Duration.ofSeconds(20)).withMessage("Today did not switch to " + title)
                .until(ignored -> getActiveProgramName().equalsIgnoreCase(title));
        assertTodayTabIsSelected();
        assertDailyPlanDaySwitcherIsDisplayed();
    }

    /** Compare rendered Today with the fresh, UID-verified response for this selection. */
    @SuppressWarnings("unchecked")
    public void assertSelectedProgramContent(Map<String,Object> content) {
        Assert.assertEquals("Unexpected destination program", "keep_it_hard", content.get("programId"));
        assertActiveProgramMatches((String)content.get("headline"));
        Map<String,Object> metadata = (Map<String,Object>)content.get("sectionMetadata");
        String module = (String)metadata.get("module_name");
        Assert.assertNotNull("Destination has no module title", module);
        new WebDriverWait(driver, Duration.ofSeconds(15)).withMessage("Today module does not match selected program: " + module)
                .until(ignored -> visibleText().contains(normalize(module)));
        String stage = "Stage " + ((Number)metadata.get("module_current_day")).intValue()
                + " of " + ((Number)metadata.get("module_total_days")).intValue();
        Assert.assertEquals("Today stage does not match destination module", stage, getCurrentDayLabel());
        Set<String> required = new LinkedHashSet<String>();
        for (Map<String,Object> question : (List<Map<String,Object>>)content.get("questions")) {
            if (((String)question.get("id")).startsWith("lesson_")) required.add(normalize((String)question.get("headline")));
            if (required.size() == 3) break;
        }
        Assert.assertEquals("Destination fixture needs three distinguishable lesson cards", 3, required.size());
        Set<String> observed = new LinkedHashSet<String>();
        int swipes = 0;
        Throwable primaryFailure = null;
        try {
            for (; swipes < 5; swipes++) {
                observed.addAll(visibleText());
                if (observed.containsAll(required)) {
                    Allure.addAttachment("Verified selected Today content", module + "; " + stage + "; lessons: " + required);
                    return;
                }
                mobileSwipeUp();
            }
            Assert.fail("Today is missing selected program lesson cards: " + required + "; observed: " + observed);
        } catch (RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            try {
                for (int i = 0; i < swipes; i++) mobileSwipeDown();
            } catch (RuntimeException | Error restorationFailure) {
                if (primaryFailure == null) throw restorationFailure;
                if (primaryFailure != restorationFailure) primaryFailure.addSuppressed(restorationFailure);
            }
        }
    }

    private Set<String> visibleText() {
        Set<String> labels = new LinkedHashSet<String>();
        for (WebElement element : driver.findElements(By.xpath("//XCUIElementTypeStaticText[@visible='true']"))) {
            labels.add(normalize(element.getAttribute("label")));
        }
        return labels;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    public void assertActiveProgramIsFirst(String activeBeforeOpening) {
        String first = viewport().entrySet().stream().min(Comparator.comparingInt(e -> e.getValue().y))
                .orElseThrow(() -> new AssertionError("No visible selector titles")).getKey();
        String code = first.toUpperCase(Locale.ROOT).trim();
        if (code.equals("LAST LONGER")) code = "LL";
        else if (code.equals("KEEP IT HARD")) code = "KIH";
        else if (code.equals("OVERALL HEALTH")) code = "OH";
        else if (code.equals("SEX IS A SKILL")) code = "SIAS";
        String active = activeBeforeOpening.toUpperCase(Locale.ROOT).trim();
        Assert.assertTrue("First selector program differs from Today before opening: " + first + " / " + active,
                active.equals(code) || active.equals(first.toUpperCase(Locale.ROOT).trim()));
        Assert.assertTrue("Selector must leave Today visible above the bottom sheet", firstCardBounds().y > 0);
    }

    public void swipeSelectorDown() {
        Rectangle card = firstCardBounds();
        // Start above the first card, in the sheet header, so this dismisses the
        // sheet instead of scrolling its collection back to the beginning.
        Map<String,Object> args = new HashMap<String,Object>();
        int x = card.x + card.width / 2;
        int height = driver.manage().window().getSize().height;
        args.put("fromX", x); args.put("toX", x);
        args.put("fromY", Math.max(1, card.y - 20));
        args.put("toY", height - 30); args.put("duration", .5);
        ((JavascriptExecutor)driver).executeScript("mobile: dragFromToForDuration", args);
        waitForElementNotPresent(CARDS, "Selector did not close after swipe down", 10);
        assertDailyPlanDaySwitcherIsDisplayed();
    }

    private Map<String,Rectangle> viewport() {
        Map<String,Rectangle> result = new LinkedHashMap<String,Rectangle>();
        int height = driver.manage().window().getSize().height;
        Rectangle collection = waitForElementPresent(COLLECTION, "Selector collection is missing", 10).getRect();
        for (WebElement title : driver.findElements(getLocatorByString(TITLES))) {
            Rectangle r = title.getRect();
            if (title.isDisplayed() && r.y >= collection.y && r.y + r.height < Math.min(height, collection.y + collection.height))
                result.put(title.getAttribute("label"), r);
        }
        Assert.assertFalse("No visible program titles", result.isEmpty());
        return result;
    }

    public void assertProgramsScrollVertically() {
        Map<String,Rectangle> before = viewport();
        Rectangle collectionBefore = waitForElementPresent(COLLECTION, "Selector collection is missing", 10).getRect();
        Rectangle first = firstCardBounds();
        int height = driver.manage().window().getSize().height;
        Map<String,Object> args = new HashMap<String,Object>();
        args.put("duration", .5);
        args.put("fromX", first.x + first.width / 2);
        args.put("toX", first.x + first.width / 2);
        args.put("fromY", height - 110);
        args.put("toY", Math.max(first.y + 40, height - 310));
        ((JavascriptExecutor)driver).executeScript("mobile: dragFromToForDuration", args);
        new WebDriverWait(driver, Duration.ofSeconds(10)).withMessage(
                "No common card moved upward within the open selector").until(ignored -> {
            Map<String,Rectangle> after = viewport();
            for (String name : before.keySet()) {
                Rectangle a = after.get(name), b = before.get(name);
                if (a != null && a.y < b.y - 3 && Math.abs(a.x - b.x) <= 3) {
                    Allure.addAttachment("Selector vertical movement", name + ": y=" + b.y + " → " + a.y);
                    return true;
                }
            }
            return false;
        });
        Assert.assertTrue("Selector disappeared during scrolling", isElementPresent(CARDS));
        Assert.assertEquals("The sheet moved instead of its content", collectionBefore,
                waitForElementPresent(COLLECTION, "Selector collection disappeared", 10).getRect());
    }
}
