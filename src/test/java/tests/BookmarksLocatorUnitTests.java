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
