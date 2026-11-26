package tests;

import lib.ui.ios.iOSBookmarksPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BookmarksTooltipUnitTests {
    private static final String TEXT = "Remove from bookmarks";
    private static final List<String> TITLES = Arrays.asList("Second card", "First card");

    @Test
    public void usesVisualOrderAndLeavesTheTooltipOnScreen() {
        Screen screen = new Screen(new Rectangle(733, 786, 160, 268), TITLES);
        Assert.assertEquals("First card", screen.openFromTodayAndInspectTooltip(TEXT, TITLES));
        Assert.assertEquals(1, screen.entries);
    }

    @Test
    public void rejectsTooltipBesideTheSecondCard() {
        Screen screen = new Screen(new Rectangle(733, 975, 160, 268), TITLES);
        rejects(screen, "Tooltip is not adjacent to the first card's bookmark icon");
    }

    @Test
    public void tooltipTextAloneCannotProveTheAnchor() {
        Screen screen = new Screen(new Rectangle(733, 786, 160, 268), Collections.emptyList());
        rejects(screen, "The expected cards must be visible without moving the tooltip");
    }

    @Test
    public void matchesTheWrappedIPhoneTooltipAsAnExactVisibleLabel() throws Exception {
        String page = "<root>"
                + "<XCUIElementTypeStaticText name='Remove from &#10; bookmarks' visible='true'/>"
                + "<XCUIElementTypeStaticText name='Remove from bookmarks' visible='false'/>"
                + "<XCUIElementTypeStaticText name='Remove from bookmarks only here' visible='true'/>"
                + "</root>";
        NodeList hits = new iOSBookmarksPageObject(null) {
            NodeList tooltipMatches() throws Exception {
                return (NodeList) XPathFactory.newInstance().newXPath().evaluate(
                        tooltipFor(TEXT).substring("xpath:".length()),
                        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(page))),
                        XPathConstants.NODESET);
            }
        }.tooltipMatches();
        Assert.assertEquals(1, hits.getLength());
        Assert.assertEquals("Remove from \n bookmarks", hits.item(0).getAttributes().getNamedItem("name").getNodeValue());
    }

    private static void rejects(Screen screen, String expected) {
        try {
            screen.openFromTodayAndInspectTooltip(TEXT, TITLES);
            Assert.fail("Tooltip without verified placement was accepted");
        } catch (AssertionError error) {
            Assert.assertTrue(error.getMessage(), error.getMessage().contains(expected));
        }
    }

    private static class Screen extends iOSBookmarksPageObject {
        private final Rectangle tooltip;
        private final List<String> visible;
        private int entries;

        Screen(Rectangle tooltip, List<String> visible) {
            super(null);
            this.tooltip = tooltip;
            this.visible = visible;
        }

        @Override public void tapEntryFromToday() { entries++; }

        @Override protected List<String> visibleCardTitles(List<String> expected) { return visible; }

        @Override protected void swipe(String direction) { Assert.fail("Scrolled before measuring the tooltip"); }

        @Override protected void tapBlankArea(int x, int y) { Assert.fail("Dismissed the tooltip before measuring it"); }

        @Override public WebElement waitForElementPresent(String locator, String message, long timeout) {
            if (locator.equals(tooltipFor(TEXT))) return element(tooltip);
            if (locator.equals(cardFor("First card"))) return element(new Rectangle(42, 588, 168, 996));
            if (locator.equals(cardFor("Second card"))) return element(new Rectangle(42, 777, 168, 996));
            if (locator.equals(cardBookmarkFor("First card"))) return element(new Rectangle(901, 609, 126, 126));
            if (locator.equals(cardBookmarkFor("Second card"))) return element(new Rectangle(901, 798, 126, 126));
            throw new AssertionError(message);
        }

        private WebElement element(Rectangle rect) {
            return new RemoteWebElement() {
                @Override public Rectangle getRect() { return rect; }
            };
        }
    }
}
