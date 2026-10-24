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
    private static final String FOR_HER_CAROUSEL =
            "<XCUIElementTypeOther name='CoachProgramDetailedSectionViewCoachProgramDetailedManV7Card'>"
                    + "<XCUIElementTypeCollectionView><XCUIElementTypeCell>"
                    + "<XCUIElementTypeOther name='CoachProgramDetailedManV7Card'><XCUIElementTypeImage/>"
                    + "<XCUIElementTypeOther><XCUIElementTypeStaticText label='30-Day Program'/></XCUIElementTypeOther>"
                    + "<XCUIElementTypeStaticText label='Sex Is a Skill'/>"
                    + "</XCUIElementTypeOther></XCUIElementTypeCell></XCUIElementTypeCollectionView></XCUIElementTypeOther>";

    private static class Locators extends iOSExplorePageObject {
        Locators() { super(null); }
        String readiness() { return RECOMMENDED_SECTION; }
        String firstTitle() { return FIRST_PROGRAM_TITLE; }
        String lastTitle() { return LAST_RECOMMENDED_PROGRAM_TITLE; }
    }

    private String[] variantLocators(boolean forHer) {
        String previous = System.getProperty("ios.bundleId");
        try {
            System.setProperty("ios.bundleId", forHer ? "com.vamapps.The-Coach-for-her" : "com.vamapps.The-Coach");
            Locators page = new Locators();
            return new String[]{iOSExplorePageObject.programTitlesLocator(),
                    iOSExplorePageObject.programCollectionLocator(), page.readiness(),
                    page.firstTitle(), page.lastTitle()};
        } finally {
            if (previous == null) System.clearProperty("ios.bundleId");
            else System.setProperty("ios.bundleId", previous);
            new Locators();
        }
    }

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

    @Test public void forHerAllowsItsConfiguredEmptyHeadingButStillReadsTheProgramTitle() throws Exception {
        String[] locators = variantLocators(true);
        NodeList titles = select(FOR_HER_CAROUSEL, locators[0]);
        Assert.assertEquals(1, titles.getLength());
        Assert.assertEquals("Sex Is a Skill", titles.item(0).getAttributes().getNamedItem("label").getNodeValue());
        for (int index = 1; index < locators.length; index++)
            Assert.assertEquals("For Her locator " + index, 1, select(FOR_HER_CAROUSEL, locators[index]).getLength());
    }

    @Test public void maleStillRequiresItsExpectedCatalogHeading() throws Exception {
        String[] locators = variantLocators(false);
        Assert.assertEquals(0, select(FOR_HER_CAROUSEL, locators[0]).getLength());
        Assert.assertEquals(0, select(FOR_HER_CAROUSEL, locators[1]).getLength());
    }

    @Test public void forHerRejectsUnrelatedCardsOutsideTheProgramSection() throws Exception {
        String xml = FOR_HER_CAROUSEL.replace("CoachProgramDetailedSectionViewCoachProgramDetailedManV7Card",
                "CoachProgramDetailedSectionViewExploreExerciseChallengeCellView");
        for (String locator : variantLocators(true))
            Assert.assertEquals(0, select(xml, locator).getLength());
    }

    @Test public void forHerReadinessRejectsEmptyCollectionsBlankTitlesAndMissingImages() throws Exception {
        String[] locators = variantLocators(true);
        for (String xml : new String[]{
                "<XCUIElementTypeOther name='CoachProgramDetailedSectionViewCoachProgramDetailedManV7Card'>"
                        + "<XCUIElementTypeCollectionView/></XCUIElementTypeOther>",
                FOR_HER_CAROUSEL.replace("label='Sex Is a Skill'", "label=' '"),
                FOR_HER_CAROUSEL.replace("<XCUIElementTypeImage/>", "")}) {
            Assert.assertEquals(0, select(xml, locators[0]).getLength());
            Assert.assertEquals(0, select(xml, locators[2]).getLength());
        }
    }

    @Test public void constructingMaleAfterForHerRestoresTheMaleReadinessContract() throws Exception {
        variantLocators(true);
        String[] male = variantLocators(false);
        Assert.assertEquals(0, select(FOR_HER_CAROUSEL, male[2]).getLength());
        String headed = FOR_HER_CAROUSEL.replace("<XCUIElementTypeCollectionView>",
                "<XCUIElementTypeStaticText name='Browse Programs'/><XCUIElementTypeCollectionView>");
        for (String locator : male) Assert.assertEquals(1, select(headed, locator).getLength());
    }
}
