package tests;

import lib.ui.ios.IOSMyProgramsPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Replays the observed sheet/list structure without a device or account. */
public class IOSMyProgramsScrollUnitTests {
    @Test
    public void returnsToTheOriginalRowsAfterScrollingInBothDirections() {
        ScrollDriver driver = new ScrollDriver();
        new IOSMyProgramsPageObject(driver).scrollListDownAndBack("Sex Is a Skill");
        Assert.assertEquals(0, driver.contentOffset);
        Assert.assertEquals(1, driver.reverseGestures);
        Assert.assertTrue(driver.selectorOpen);
    }

    @Test
    public void reachesAnInitiallyOffscreenCtaByScrollingInsideTheList() {
        ScrollDriver driver = new ScrollDriver();
        new IOSMyProgramsPageObject(driver).scrollToAddProgram();
        Assert.assertEquals(1, driver.gestures);
        Assert.assertEquals(0, driver.sheetOffset);
    }

    @Test
    public void addProgramRevealsTheCtaAndOpensAllPrograms() {
        ScrollDriver driver = new ScrollDriver();
        new IOSMyProgramsPageObject(driver).openCatalogFromAddProgram();
        Assert.assertEquals(1, driver.gestures);
        Assert.assertTrue(driver.catalogOpen);
        Assert.assertFalse(driver.selectorOpen);
    }

    @Test
    public void aReportedVisibleButClippedCtaStillRequiresFurtherScrolling() {
        ScrollDriver driver = new ScrollDriver();
        driver.ctaY = 1000;
        new IOSMyProgramsPageObject(driver).scrollToAddProgram();
        Assert.assertEquals(2, driver.gestures);
    }

    @Test
    public void rejectsAShortFixtureBeforeAnyGesture() {
        ScrollDriver driver = new ScrollDriver();
        driver.ctaY = 700;
        try {
            new IOSMyProgramsPageObject(driver).scrollListDownAndBack("Sex Is a Skill");
            Assert.fail("An already visible CTA cannot demonstrate scrolling");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("fixture is too short"));
        }
        Assert.assertEquals(0, driver.gestures);
    }

    @Test
    public void rejectsMovingTheEntireSheetInsteadOfItsList() {
        ScrollDriver driver = new ScrollDriver();
        driver.moveSheet = true;
        try {
            new IOSMyProgramsPageObject(driver).scrollToAddProgram();
            Assert.fail("Moving the sheet must not satisfy the list-scroll oracle");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("sheet moved"));
        }
    }

    @Test(expected = TimeoutException.class)
    public void movementOfTextOutsideTheSelectorCannotProveListScrolling() {
        ScrollDriver driver = new ScrollDriver();
        driver.moveOutsideOnly = true;
        new IOSMyProgramsPageObject(driver).scrollToAddProgram();
    }

    @Test
    public void neverTapsTheHeaderWhenAnotherApplicationIsInTheForeground() {
        ScrollDriver driver = new ScrollDriver();
        driver.otherApplication = true;
        try {
            new IOSMyProgramsPageObject(driver).openProgramSelector();
            Assert.fail("The selector action cannot touch another application");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("foreground application"));
        }
        Assert.assertEquals(0, driver.nativeTaps);
    }

    @Test
    public void tapsTheObservedDropdownImageWithoutHittingTheAdjacentCalendarControl() {
        ScrollDriver driver = new ScrollDriver();
        driver.nativeHeader = true;
        driver.selectorOpen = false;
        new IOSMyProgramsPageObject(driver).openProgramSelector();
        Assert.assertEquals(164, ((Number) driver.lastTap.get("x")).intValue());
        Assert.assertEquals(68, ((Number) driver.lastTap.get("y")).intValue());
        Assert.assertEquals(1, driver.nativeTaps);
        Assert.assertTrue(driver.selectorOpen);
    }

    private static final class ScrollDriver extends RemoteWebDriver {
        int gestures;
        int reverseGestures;
        int contentOffset;
        int sheetOffset;
        int outsideOffset;
        int ctaY = 860;
        boolean moveSheet;
        boolean moveOutsideOnly;
        boolean selectorOpen = true;
        boolean catalogOpen;
        boolean otherApplication;
        int nativeTaps;
        boolean nativeHeader;
        Map<?, ?> lastTap;

        @Override
        public Object executeScript(String script, Object... arguments) {
            if ("mobile: activeAppInfo".equals(script)) {
                return java.util.Collections.singletonMap("bundleId", otherApplication
                        ? "com.apple.mobilecal" : lib.Platform.getInstance().getIOSBundleId());
            }
            if ("mobile: tap".equals(script)) {
                nativeTaps++;
                lastTap = (Map<?, ?>) arguments[0];
                selectorOpen = !selectorOpen;
                if (selectorOpen) contentOffset = 0;
                return null;
            }
            Assert.assertEquals("mobile: dragFromToForDuration", script);
            Map<?, ?> drag = (Map<?, ?>) arguments[0];
            int from = ((Number) drag.get("fromY")).intValue();
            int to = ((Number) drag.get("toY")).intValue();
            Assert.assertTrue("Gesture starts outside the list", from > 300 && from < 800);
            Assert.assertTrue("Gesture ends outside the list", to > 300 && to < 800);
            gestures++;
            if (to > from) reverseGestures++;
            int delta = to - from;
            if (moveOutsideOnly) outsideOffset += delta;
            else contentOffset += delta;
            if (moveSheet) sheetOffset += delta;
            return null;
        }

        @Override
        public WebElement findElement(By by) {
            List<WebElement> elements = findElements(by);
            if (elements.isEmpty()) throw new NoSuchElementException(by.toString());
            return elements.get(0);
        }

        @Override
        public List<WebElement> findElements(By by) {
            String locator = by.toString();
            String xpath;
            if (locator.startsWith("By.id: ")) {
                xpath = "//*[@name='" + locator.substring("By.id: ".length()) + "']";
            } else if (locator.startsWith("By.xpath: ")) {
                xpath = locator.substring("By.xpath: ".length());
            } else throw new AssertionError("Unexpected locator: " + locator);
            String xml = "<App>"
                    + (catalogOpen ? node("XCUIElementTypeStaticText", "All Programs", 260, 22) : "")
                    + node("XCUIElementTypeStaticText", "Unrelated Today title", 650 + outsideOffset, 24)
                    + "<XCUIElementTypeStaticText name='active_program_name' label='Sex Is a Skill'"
                    + " visible='true' x='16' y='60' width='200' height='20'/>"
                    + "<XCUIElementTypeOther name='DailyDaySwitcherView'>"
                    + node("XCUIElementTypeStaticText", "Stage 1 of 8", 120, 20)
                    + "</XCUIElementTypeOther>"
                    + (nativeHeader ? "<XCUIElementTypeOther><XCUIElementTypeOther><XCUIElementTypeOther>"
                    + "<XCUIElementTypeStaticText name='Sex Is a Skill' visible='true' x='16' y='56' width='129' height='24'/>"
                    + "<XCUIElementTypeImage visible='true' x='152' y='56' width='25' height='24'/>"
                    + "</XCUIElementTypeOther></XCUIElementTypeOther>"
                    + "<XCUIElementTypeButton name='Calendar' visible='true' x='270' y='64' width='32' height='33'/>"
                    + "<XCUIElementTypeButton name='nav bar bookmarks' visible='true' x='318' y='64' width='32' height='33'/>"
                    + "</XCUIElementTypeOther>" : node("XCUIElementTypeButton", "daily_plan_program_selector", 100, 20))
                    + (selectorOpen ? node("XCUIElementTypeButton", "Sheet Grabber", 230 + sheetOffset, 26)
                    + node("XCUIElementTypeStaticText", "My programs", 260 + sheetOffset, 22)
                    + "<XCUIElementTypeScrollView visible='true' x='16' y='" + (300 + sheetOffset)
                    + "' width='382' height='500'>"
                    + node("XCUIElementTypeStaticText", "MAIN JOURNEY", 300 + contentOffset, 12)
                    + node("XCUIElementTypeStaticText", "Sex Is a Skill", 400 + contentOffset, 19)
                    + node("XCUIElementTypeStaticText", "✓", 406 + contentOffset, 20)
                    + node("XCUIElementTypeStaticText", "Keep It Hard", 650 + contentOffset, 19)
                    // UIKit can report an element visible even while its bounds are clipped.
                    + node("XCUIElementTypeStaticText", "Add a program",
                            ctaY + (moveOutsideOnly ? outsideOffset : contentOffset), 20)
                    + "</XCUIElementTypeScrollView>" : "") + "</App>";
            try {
                Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
                NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath()
                        .evaluate(xpath, document, XPathConstants.NODESET);
                List<WebElement> elements = new ArrayList<>();
                for (int i = 0; i < nodes.getLength(); i++) elements.add(new XmlElement((Element) nodes.item(i), this));
                return elements;
            } catch (Exception failure) {
                throw new AssertionError(failure);
            }
        }

        private static String node(String type, String name, int y, int height) {
            return "<" + type + " name='" + name + "' visible='true' enabled='true' hittable='true' x='84' y='" + y
                    + "' width='200' height='" + height + "'/>";
        }
    }

    private static final class XmlElement extends RemoteWebElement {
        private final Element node;
        private final ScrollDriver driver;

        XmlElement(Element node, ScrollDriver driver) { this.node = node; this.driver = driver; }
        @Override public void click() {
            Assert.assertEquals("Unexpected control click", "Add a program", node.getAttribute("name"));
            driver.catalogOpen = true;
            driver.selectorOpen = false;
        }
        @Override public boolean isDisplayed() { return true; }
        @Override public boolean isEnabled() { return true; }
        @Override public String getAttribute(String name) { return node.getAttribute(name); }
        @Override public Rectangle getRect() {
            return new Rectangle(Integer.parseInt(node.getAttribute("x")), Integer.parseInt(node.getAttribute("y")),
                    Integer.parseInt(node.getAttribute("height")), Integer.parseInt(node.getAttribute("width")));
        }
    }
}
