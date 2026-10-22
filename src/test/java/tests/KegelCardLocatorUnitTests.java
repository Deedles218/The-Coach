package tests;

import lib.ui.ios.iOSKegelCustomizationPageObject;
import lib.ui.ios.iOSDailyPlanPageObject;
import org.junit.Assert;
import org.junit.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Observed collection layout: the same headline can exist in Practice and Catch-Up. */
public class KegelCardLocatorUnitTests {
    private static String header(String label) {
        return "<XCUIElementTypeCell><XCUIElementTypeStaticText name='TitleBlock.Title' label='" + label + "'/></XCUIElementTypeCell>";
    }
    private static String card() {
        return "<XCUIElementTypeCell><XCUIElementTypeOther name='DailyPlanItem'><XCUIElementTypeStaticText name='Resistance Kegel Training'/></XCUIElementTypeOther></XCUIElementTypeCell>";
    }
    private static int count(String cells) throws Exception {
        return count(cells, iOSKegelCustomizationPageObject.catchUpCardLocator("Resistance Kegel Training"));
    }
    private static int count(String cells, String locator) throws Exception {
        String xml = "<XCUIElementTypeCollectionView>" + cells + "</XCUIElementTypeCollectionView>";
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        String query = locator.substring("xpath:".length());
        return ((NodeList) XPathFactory.newInstance().newXPath().evaluate(query, document, XPathConstants.NODESET)).getLength();
    }
    @Test public void scheduledTwinIsExcluded() throws Exception {
        Assert.assertEquals(1, count(header("DAILY PRACTICE") + card() + header("TO CATCH-UP") + card()));
    }
    @Test public void scheduledCardCannotSatisfyMissingCatchUp() throws Exception {
        Assert.assertEquals(0, count(header("DAILY PRACTICE") + card() + header("TO CATCH-UP")));
    }
    @Test public void laterSectionDoesNotBelongToCatchUp() throws Exception {
        Assert.assertEquals(0, count(header("TO CATCH-UP") + header("OTHER") + card()));
    }
    @Test public void duplicateCatchUpRemainsDetectable() throws Exception {
        Assert.assertEquals(2, count(header("TO CATCH-UP") + card() + card()));
    }
    @Test public void practiceLocatorExcludesLessonTwin() throws Exception {
        Assert.assertEquals(1, count(header("DAILY LESSONS") + card() + header("DAILY PRACTICE") + card(),
                iOSDailyPlanPageObject.dailyPlanCardLocator("DAILY PRACTICE", "Resistance Kegel Training")));
    }
    @Test public void catchUpCannotSatisfyPracticeFixture() throws Exception {
        Assert.assertEquals(0, count(header("DAILY PRACTICE") + header("TO CATCH-UP") + card(),
                iOSDailyPlanPageObject.dailyPlanCardLocator("DAILY PRACTICE", "Resistance Kegel Training")));
    }
    @Test public void completedPracticeCannotSatisfyUnfinishedFixture() throws Exception {
        String completed = card().replace("</XCUIElementTypeOther>",
                "<XCUIElementTypeImage name='dailyCompletedCheckmarkWoman' visible='true'/></XCUIElementTypeOther>");
        Assert.assertEquals(1, count(header("DAILY PRACTICE") + completed + card(),
                iOSDailyPlanPageObject.unfinishedDailyPracticeCardLocator("Resistance Kegel Training")));
    }
}
