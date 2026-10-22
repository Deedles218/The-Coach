package tests;

import lib.ui.ios.iOSExplorePageObject;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/** Captures the two observed layouts and protects against reading badge text as a title. */
public class IOSExploreLocatorUnitTests {
    private NodeList select(String xml, String locator) throws Exception {
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(("<App>" + xml + "</App>").getBytes(StandardCharsets.UTF_8)));
        return (NodeList) XPathFactory.newInstance().newXPath().evaluate(
                locator.substring("xpath:".length()), document, XPathConstants.NODESET);
    }

    @Test public void v7ReadsProgramTitleInsteadOfCategoryBadge() throws Exception {
        String xml = "<XCUIElementTypeOther name='CoachProgramDetailedSectionViewCoachProgramDetailedManV7Card'>"
                + "<XCUIElementTypeStaticText name='Browse Programs'/>"
                + "<XCUIElementTypeCollectionView><XCUIElementTypeCell>"
                + "<XCUIElementTypeOther name='CoachProgramDetailedManV7Card'><XCUIElementTypeImage/>"
                + "<XCUIElementTypeOther><XCUIElementTypeStaticText label='Sexual health'/></XCUIElementTypeOther>"
                + "<XCUIElementTypeStaticText label='Sex Is a Skill'/>"
                + "<XCUIElementTypeStaticText label='Program description'/>"
                + "</XCUIElementTypeOther></XCUIElementTypeCell></XCUIElementTypeCollectionView></XCUIElementTypeOther>";
        NodeList titles = select(xml, iOSExplorePageObject.programTitlesLocator());
        Assert.assertEquals(1, titles.getLength());
        Assert.assertEquals("Sex Is a Skill", titles.item(0).getAttributes().getNamedItem("label").getNodeValue());
        Assert.assertEquals(1, select(xml, iOSExplorePageObject.programCollectionLocator()).getLength());
    }

    @Test public void legacyReadsTitleNestedInsideImage() throws Exception {
        String xml = "<XCUIElementTypeOther name='CoachProgramDetailedSectionViewCoachProgramDetailedManCard'>"
                + "<XCUIElementTypeStaticText name='ALL PROGRAMS'/><XCUIElementTypeCollectionView>"
                + "<XCUIElementTypeOther name='CoachProgramDetailedManCard'><XCUIElementTypeImage>"
                + "<XCUIElementTypeStaticText label='Overall Health'/></XCUIElementTypeImage></XCUIElementTypeOther>"
                + "</XCUIElementTypeCollectionView></XCUIElementTypeOther>";
        NodeList titles = select(xml, iOSExplorePageObject.programTitlesLocator());
        Assert.assertEquals(1, titles.getLength());
        Assert.assertEquals("Overall Health", titles.item(0).getAttributes().getNamedItem("label").getNodeValue());
    }

    @Test public void unrelatedCardsCannotSatisfyCatalogReadiness() throws Exception {
        String xml = "<XCUIElementTypeOther name='CoachProgramDetailedManV7Card'>"
                + "<XCUIElementTypeStaticText label='Unrelated card'/></XCUIElementTypeOther>";
        Assert.assertEquals(0, select(xml, iOSExplorePageObject.programTitlesLocator()).getLength());
        Assert.assertEquals(0, select(xml, iOSExplorePageObject.programCollectionLocator()).getLength());
    }
}
