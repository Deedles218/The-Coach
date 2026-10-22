package tests;

import lib.ui.ios.iOSBookmarksPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class BookmarksLocatorUnitTests {
    private static final class Screen extends iOSBookmarksPageObject {
        Screen() { super(null); }
        String slide(String text) { return slideText(text); }
        String card(String title) { return cardFor(title); }
        String indicator(String title) { return dailyPlanCompletionIndicatorFor(title); }
        String category(String title, String category) { return cardCategoryFor(title, category); }
        String group(String title, String group) { return cardGroupFor(title, group); }
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
