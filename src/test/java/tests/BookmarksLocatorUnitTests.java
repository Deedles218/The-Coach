package tests;

import lib.ui.ios.iOSBookmarksPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookmarksLocatorUnitTests {
    private static final class Screen extends iOSBookmarksPageObject {
        Screen() { super(null); }
        String slide(String text) { return slideText(text); }
        String card(String title) { return cardFor(title); }
        String indicator(String title) { return dailyPlanCompletionIndicatorFor(title); }
        String category(String title, String category) { return cardCategoryFor(title, category); }
        String group(String title, String group) { return cardGroupFor(title, group); }
        String listTitles(String anchor) { return cardListTitleLocator(anchor); }
    }

    private int count(String xml, String text) throws Exception {
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        String locator = new Screen().slide(text);
        return ((NodeList) XPathFactory.newInstance().newXPath().evaluate(
                locator.substring("xpath:".length()), document, XPathConstants.NODESET)).getLength();
    }

    @Test
    public void persistentLessonTitleCannotSatisfySavedSlideCheck() throws Exception {
        String xml = "<root><XCUIElementTypeStaticText name='The Basics' visible='true'/>"
                + "<XCUIElementTypeCollectionView><XCUIElementTypeStaticText name='A later slide' visible='true'/>"
                + "</XCUIElementTypeCollectionView></root>";
        Assert.assertEquals(0, count(xml, "The Basics"));
        Assert.assertEquals(1, count(xml, "A later slide"));
    }

    @Test
    public void hiddenSlideCannotSatisfyNavigation() throws Exception {
        String xml = "<XCUIElementTypeCollectionView><XCUIElementTypeStaticText name='Saved slide' visible='false'/>"
                + "</XCUIElementTypeCollectionView>";
        Assert.assertEquals(0, count(xml, "Saved slide"));
    }

    @Test
    public void verifiesEachConfiguredSlideWithoutPressingFinish() {
        for (boolean saved : new boolean[]{true, false}) {
            Reader reader = new Reader(saved);
            reader.assertBookmarkOnEverySlide(Reader.SLIDES, saved);
            Assert.assertEquals(Arrays.asList(0, 1, 2, 3), reader.checked);
        }
    }

    @Test
    public void rejectsIncorrectBookmarkStateOnAnIntermediateSlide() {
        Reader reader = new Reader(false);
        reader.incorrect = 1;
        try {
            reader.assertBookmarkOnEverySlide(Reader.SLIDES, false);
            Assert.fail("An intermediate slide with the wrong state was accepted");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage().contains("Wrong bookmark state on slide 2"));
        }
        Assert.assertEquals(Arrays.asList(0, 1), reader.checked);
    }

    @Test
    public void rejectsSkippedSlidesAndATruncatedFixture() {
        Reader skipped = new Reader(true);
        skipped.step = 2;
        try {
            skipped.assertBookmarkOnEverySlide(Reader.SLIDES, true);
            Assert.fail("The reader skipped a slide");
        } catch (TimeoutException expected) {
            Assert.assertTrue(expected.getMessage().contains("configured slide 2"));
        }
        Assert.assertEquals(Arrays.asList(0), skipped.checked);

        Reader truncated = new Reader(true);
        try {
            truncated.assertBookmarkOnEverySlide(Reader.SLIDES.subList(0, 2), true);
            Assert.fail("A fixture ending before FINISH was accepted");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage().contains("final configured slide must display FINISH"));
        }
    }

    private static final class Reader extends iOSBookmarksPageObject {
        static final List<String> SLIDES = Arrays.asList("First", "Second", "Third", "Last");
        final List<Integer> checked = new ArrayList<>();
        final boolean initialSaved;
        int page;
        int step = 1;
        int incorrect = -1;

        Reader(boolean saved) { super(new RemoteWebDriver() { }); initialSaved = saved; }
        @Override protected String slideText(String text) { return text; }
        @Override public boolean isElementVisible(String locator) {
            if (FINISH.equals(locator)) return page == SLIDES.size() - 1;
            if (CONTENT_NEXT.equals(locator)) return page < SLIDES.size() - 1;
            return SLIDES.get(page).equals(locator);
        }
        @Override public WebDriverWait createWait(long timeout) { return new WebDriverWait(driver, Duration.ZERO); }
        @Override public WebElement waitForElementVisible(String locator, String message, long timeout) {
            Assert.assertTrue(message, isElementVisible(locator));
            return null;
        }
        @Override public WebElement waitForElementAndClick(String locator, String message, long timeout) {
            Assert.assertEquals("Only the next-slide control should be clicked", CONTENT_NEXT, locator);
            page += step;
            return null;
        }
        @Override public void assertOpenedContentSaved() { checkState(true); }
        @Override public void assertOpenedContentNotSaved() { checkState(false); }
        private void checkState(boolean expected) {
            checked.add(page);
            Assert.assertEquals("Wrong bookmark state on slide " + (page + 1), expected,
                    page == incorrect ? !initialSaved : initialSaved);
        }
    }

    @Test
    public void bookmarkListTitleCannotSatisfyReaderCheck() throws Exception {
        String xml = "<root><XCUIElementTypeStaticText name='Saved lesson' visible='true'/>"
                + "<XCUIElementTypeButton name='ic_outline_chevron_left' visible='true'/></root>";
        org.w3c.dom.Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        javax.xml.xpath.XPath xpath = XPathFactory.newInstance().newXPath();
        iOSBookmarksPageObject screen = new iOSBookmarksPageObject(null) {
            @Override public org.openqa.selenium.WebElement waitForElementPresent(String locator, String message, long timeout) {
                try {
                    if (xpath.evaluate(locator.substring("xpath:".length()), document, XPathConstants.NODE) == null) {
                        throw new org.openqa.selenium.TimeoutException(message);
                    }
                    return null;
                } catch (javax.xml.xpath.XPathExpressionException error) {
                    throw new AssertionError(error);
                }
            }
        };
        try {
            screen.assertOpenedContentTitle("Saved lesson");
            Assert.fail("A title on the list was accepted as an opened lesson");
        } catch (org.openqa.selenium.TimeoutException expected) {
            Assert.assertTrue(expected.getMessage().startsWith("Bookmark did not open the content reader: Saved lesson"));
        }
        org.w3c.dom.Element close = document.createElement("XCUIElementTypeButton");
        close.setAttribute("name", "ic outline close");
        close.setAttribute("visible", "true");
        document.getDocumentElement().appendChild(close);
        screen.assertOpenedContentTitle("Saved lesson");
    }

    @Test
    public void staleSavedStateFailsWithoutRemovingItDuringTheAssertion() {
        iOSBookmarksPageObject screen = new iOSBookmarksPageObject(null) {
            @Override public org.openqa.selenium.WebElement waitForElementPresent(String locator, String message, long timeout) {
                Assert.assertEquals(INACTIVE_BOOKMARK, locator);
                return null;
            }
            @Override public boolean isElementPresent(String locator) {
                Assert.assertEquals(ACTIVE_BOOKMARK, locator);
                return true;
            }
            @Override public void removeOpenedContent() {
                Assert.fail("The assertion repaired the stale saved state");
            }
        };
        try {
            screen.assertOpenedContentNotSaved();
            Assert.fail("Stale active bookmark was accepted");
        } catch (AssertionError expected) {
            Assert.assertEquals("Removed content still exposes an active bookmark", expected.getMessage());
        }
    }

    private static class ScrolledList extends iOSBookmarksPageObject {
        int viewport = 1;
        int target = -1;
        boolean movesForever;
        ScrolledList() { super(null); }
        @Override public void assertScreenVisible() { }
        @Override public boolean isElementPresent(String locator) { return viewport == target; }
        @Override protected String bookmarkListSignature() { return "viewport:" + viewport; }
        @Override protected void swipe(String direction) {
            if (movesForever) viewport++;
            else viewport = Math.max(0, Math.min(3, viewport + ("up".equals(direction) ? 1 : -1)));
        }
    }

    @Test
    public void absentCardCheckRejectsAStaleRowOutsideTheInitialViewport() {
        for (int target : new int[]{0, 3}) {
            ScrolledList screen = new ScrolledList();
            screen.target = target;
            try {
                screen.assertCardAbsent("Removed lesson");
                Assert.fail("Offscreen stale bookmark was accepted as absent");
            } catch (AssertionError expected) {
                Assert.assertTrue(expected.getMessage().contains("Bookmark card is still displayed"));
            }
        }
    }

    @Test
    public void absentCardCheckReachesBothBoundariesAndFailsWhenTheListCannotBeExhausted() {
        ScrolledList screen = new ScrolledList();
        screen.assertCardAbsent("Removed lesson");
        Assert.assertEquals(3, screen.viewport);
        screen.movesForever = true;
        try {
            screen.assertCardAbsent("Removed lesson");
            Assert.fail("An unexhausted list was accepted as absent");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage().contains("list boundary was not reached"));
        }
    }

    @Test
    public void exactListCheckRejectsExtraOrRepeatedCardsBeyondTheFirstViewport() {
        List<String> expected = Arrays.asList("A", "B", "C");
        assertViewportTitles(expected, Arrays.asList("B", "C"));
        for (List<String> wrong : Arrays.asList(Arrays.asList("B", "C", "Extra"),
                Arrays.asList("B", "A", "C"), Arrays.asList("C"))) {
            try {
                assertViewportTitles(expected, wrong);
                Assert.fail("The full list accepted the invalid second viewport " + wrong);
            } catch (AssertionError failure) {
                String message = failure.getMessage();
                Assert.assertTrue(message, message.contains("Unexpected bookmark cards")
                        || message.contains("appears again") || message.contains("skipped rows"));
            }
        }
    }

    private void assertViewportTitles(List<String> expected, List<String> second) {
        iOSBookmarksPageObject screen = new iOSBookmarksPageObject(null) {
            private boolean first;
            @Override public void assertScreenVisible() { }
            @Override protected String bookmarkListSignature() { return Boolean.toString(first); }
            @Override protected void swipe(String direction) { first = "down".equals(direction); }
            @Override protected List<String> visibleCardTitles(List<String> expected) {
                return first ? Arrays.asList("A", "B") : second;
            }
        };
        screen.assertExactCardTitles(expected, null);
    }

    @Test
    public void moduleHeadingCannotComeFromThePreviousProgram() throws Exception {
        String xml = "<root><XCUIElementTypeStaticText name='First' visible='true'/>"
                + "<XCUIElementTypeStaticText name='MODULE 1' visible='true'/>"
                + "<XCUIElementTypeStaticText name='Second' visible='true'/>"
                + "<XCUIElementTypeStaticText name='Practices' visible='true'/>"
                + "<XCUIElementTypeButton visible='true'><XCUIElementTypeStaticText name='Target' visible='true'/>"
                + "</XCUIElementTypeButton></root>";
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        String locator = new Screen().group("Target", "MODULE 1").substring("xpath:".length());
        NodeList matches = (NodeList) XPathFactory.newInstance().newXPath().evaluate(locator, document, XPathConstants.NODESET);
        Assert.assertEquals(0, matches.getLength());
    }

    @Test
    public void cardListLocatorReadsSiblingTitlesEvenWhenAnUnexpectedCardHasNoActiveIcon() throws Exception {
        String xml = "<root><XCUIElementTypeButton visible='true'><XCUIElementTypeStaticText name='Outside' visible='true'/>"
                + "</XCUIElementTypeButton><XCUIElementTypeOther>"
                + "<XCUIElementTypeStaticText name='Lessons' visible='true'/>"
                + "<XCUIElementTypeButton visible='true'><XCUIElementTypeImage name='filled_bookmark' visible='true'/>"
                + "<XCUIElementTypeStaticText name='Expected' visible='true'/></XCUIElementTypeButton>"
                + "<XCUIElementTypeButton visible='true'><XCUIElementTypeStaticText name='Extra' visible='true'/>"
                + "</XCUIElementTypeButton></XCUIElementTypeOther></root>";
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        String locator = new Screen().listTitles("Expected").substring("xpath:".length());
        NodeList matches = (NodeList) XPathFactory.newInstance().newXPath().evaluate(locator, document, XPathConstants.NODESET);
        Assert.assertEquals(2, matches.getLength());
        Assert.assertEquals("Expected", matches.item(0).getAttributes().getNamedItem("name").getNodeValue());
        Assert.assertEquals("Extra", matches.item(1).getAttributes().getNamedItem("name").getNodeValue());
    }

    @Test
    public void slideMarkersCanContainBothQuoteTypes() throws Exception {
        String xml = "<XCUIElementTypeCollectionView><XCUIElementTypeStaticText "
                + "name='Your &quot;partner&quot;&apos;s desire' visible='true'/></XCUIElementTypeCollectionView>";
        Assert.assertEquals(1, count(xml, "Your \"partner\"'s desire"));
    }

    @Test
    public void removalControlBelongsToTheRequestedCardRatherThanItsGroup() throws Exception {
        String xml = "<XCUIElementTypeOther><XCUIElementTypeButton name='First'>"
                + "<XCUIElementTypeStaticText name='First' visible='true'/>"
                + "<XCUIElementTypeButton name='filled_bookmark' fixture='first'/></XCUIElementTypeButton>"
                + "<XCUIElementTypeButton name='Second'><XCUIElementTypeStaticText name='Second' visible='true'/>"
                + "<XCUIElementTypeButton name='filled_bookmark' fixture='second'/></XCUIElementTypeButton>"
                + "</XCUIElementTypeOther>";
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        String locator = new Screen().card("Second").substring("xpath:".length());
        NodeList controls = (NodeList) XPathFactory.newInstance().newXPath().evaluate(
                locator + "//*[@name='filled_bookmark']", document, XPathConstants.NODESET);
        Assert.assertEquals(1, controls.getLength());
        Assert.assertEquals("second", controls.item(0).getAttributes().getNamedItem("fixture").getNodeValue());
    }

    @Test
    public void neighboringProgramOrCategoryCannotSatisfyCardGrouping() throws Exception {
        String xml = "<root><XCUIElementTypeOther>"
                + "<XCUIElementTypeStaticText name='First Program' visible='true'/>"
                + "<XCUIElementTypeStaticText name='MODULE 1' visible='true'/>"
                + "<XCUIElementTypeStaticText name='Lessons' visible='true'/>"
                + "<XCUIElementTypeButton><XCUIElementTypeStaticText name='First' visible='true'/></XCUIElementTypeButton>"
                + "<XCUIElementTypeStaticText name='Second Program' visible='true'/>"
                + "<XCUIElementTypeStaticText name='MODULE 2' visible='true'/>"
                + "<XCUIElementTypeStaticText name='Practices' visible='true'/>"
                + "<XCUIElementTypeButton><XCUIElementTypeStaticText name='Second' visible='true'/></XCUIElementTypeButton>"
                + "</XCUIElementTypeOther></root>";
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        javax.xml.xpath.XPath xpath = XPathFactory.newInstance().newXPath();
        Screen screen = new Screen();
        for (String locator : new String[]{screen.group("Second", "Second Program"),
                screen.group("Second", "MODULE 2"), screen.category("Second", "Practices")}) {
            Assert.assertEquals(1, ((NodeList) xpath.evaluate(locator.substring(6), document, XPathConstants.NODESET)).getLength());
        }
        for (String locator : new String[]{screen.group("Second", "First Program"),
                screen.group("Second", "MODULE 1"), screen.category("Second", "Lessons")}) {
            Assert.assertEquals(0, ((NodeList) xpath.evaluate(locator.substring(6), document, XPathConstants.NODESET)).getLength());
        }
    }

    @Test
    public void restoredProgramsAreReadFromTodayHeaderInsteadOfLessonTitles() throws Exception {
        for (String title : new String[]{"A Man&apos;s Guide to Sexting", "Woman’s Guide to Sexting"}) {
            String xml = "<root><XCUIElementTypeOther><XCUIElementTypeOther><XCUIElementTypeOther>"
                    + "<XCUIElementTypeStaticText name='" + title + "' visible='true'/>"
                    + "</XCUIElementTypeOther><XCUIElementTypeOther>"
                    + "<XCUIElementTypeStaticText name='28%' visible='true'/></XCUIElementTypeOther>"
                    + "</XCUIElementTypeOther><XCUIElementTypeButton name='nav bar bookmarks' visible='true'/>"
                    + "</XCUIElementTypeOther><XCUIElementTypeButton name='Today' value='1' visible='true'/>"
                    + "<XCUIElementTypeOther name='DailyPlanItem'>"
                    + "<XCUIElementTypeStaticText name='Sex Is a Skill' visible='true'/></XCUIElementTypeOther></root>";
            Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            String locator = lib.ui.ios.iOSDailyPlanPageObject.activeProgramTitleLocator().substring(6);
            javax.xml.xpath.XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList found = (NodeList) xpath.evaluate(locator, document, XPathConstants.NODESET);
            Assert.assertEquals(1, found.getLength());
            Assert.assertEquals(title.replace("&apos;", "'"), found.item(0).getAttributes().getNamedItem("name").getNodeValue());
            Object explore = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new ByteArrayInputStream(xml.replace("value='1'", "value='0'").getBytes(StandardCharsets.UTF_8)));
            Assert.assertEquals(0, ((NodeList) xpath.evaluate(locator, explore, XPathConstants.NODESET)).getLength());
        }
    }

    @Test
    public void legacyProgramLabelsAreAcceptedOnlyOnSelectedToday() throws Exception {
        String locator = lib.ui.ios.iOSDailyPlanPageObject.activeProgramTitleLocator().substring(6);
        for (boolean today : new boolean[]{true, false}) {
            String xml = "<root><XCUIElementTypeButton name='Today' visible='true' value='" + (today ? "1" : "0")
                    + "'/><XCUIElementTypeStaticText name='Last Longer' visible='true'/></root>";
            Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList found = (NodeList) XPathFactory.newInstance().newXPath().evaluate(locator, document, XPathConstants.NODESET);
            Assert.assertEquals(today ? 1 : 0, found.getLength());
        }
    }

    @Test
    public void legacyProgramAliasesKeepTheirExactNameAndLabelContracts() throws Exception {
        String locator = lib.ui.ios.iOSDailyPlanPageObject.activeProgramTitleLocator().substring(6);
        for (String attribute : new String[]{"name", "label"}) {
            for (String value : new String[]{"LL", "KIH", "OH", "SIAS", "Kegel Challenge", "Unhooked", "LL-extra"}) {
                String xml = "<root><XCUIElementTypeButton name='Today' visible='true' value='1'/>"
                        + "<XCUIElementTypeStaticText " + attribute + "='" + value + "' visible='true'/></root>";
                Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
                int expected = "LL-extra".equals(value) ? 0 : 1;
                Assert.assertEquals(attribute + ": " + value, expected, ((NodeList) XPathFactory.newInstance()
                        .newXPath().evaluate(locator, document, XPathConstants.NODESET)).getLength());
            }
        }
    }

    @Test
    public void completionIndicatorBelongsToTargetCardAndExcludesMetadataAndHiddenStatus() throws Exception {
        String xml = "<root><XCUIElementTypeOther name='DailyPlanItem'>"
                + "<XCUIElementTypeStaticText name='Neighbor'/><XCUIElementTypeOther visible='true' fixture='neighbor'/>"
                + "</XCUIElementTypeOther><XCUIElementTypeOther name='DailyPlanItem'>"
                + "<XCUIElementTypeStaticText name='Target'/><XCUIElementTypeOther visible='true' fixture='target'/>"
                + "<XCUIElementTypeOther visible='true' fixture='metadata'/></XCUIElementTypeOther>"
                + "<XCUIElementTypeOther name='DailyPlanItem'><XCUIElementTypeStaticText name='Hidden'/>"
                + "<XCUIElementTypeOther visible='false'/></XCUIElementTypeOther></root>";
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        javax.xml.xpath.XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList result = (NodeList) xpath.evaluate(new Screen().indicator("Target").substring(6), document, XPathConstants.NODESET);
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals("target", result.item(0).getAttributes().getNamedItem("fixture").getNodeValue());
        Assert.assertEquals(0, ((NodeList) xpath.evaluate(new Screen().indicator("Hidden").substring(6), document, XPathConstants.NODESET)).getLength());
    }
}
