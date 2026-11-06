package tests;

import lib.ui.ios.iOSDailyPlanPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Match the workout duration field, including whole minutes, never Stretching PF. */
public class KegelStartDurationLocatorUnitTests {
    private int count(String children) throws Exception {
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(("<XCUIElementTypeOther>" + children + "</XCUIElementTypeOther>")
                        .getBytes(StandardCharsets.UTF_8)));
        return ((NodeList) XPathFactory.newInstance().newXPath().evaluate(
                iOSDailyPlanPageObject.kegelStartDurationValueLocator().substring("xpath:".length()),
                document, XPathConstants.NODESET)).getLength();
    }
    private String field(String value, boolean visible) {
        return "<XCUIElementTypeStaticText name='Duration:' visible='true'/>"
                + "<XCUIElementTypeStaticText name='" + value + "' visible='" + visible + "'/>";
    }
    @Test public void wholeMinutesAreAValidDuration() throws Exception {
        Assert.assertEquals(1, count(field("1 min", true)));
    }
    @Test public void secondsAndMixedDurationsAreValid() throws Exception {
        Assert.assertEquals(1, count(field("45 sec", true)));
        Assert.assertEquals(1, count(field("2 min 25 sec", true)));
    }
    @Test public void hiddenDurationCannotSatisfyTheField() throws Exception {
        Assert.assertEquals(0, count(field("1 min", false)));
    }
    @Test public void stretchingOrALaterFieldCannotReplaceMissingDuration() throws Exception {
        Assert.assertEquals(0, count("<XCUIElementTypeStaticText name='Stretching PF (+8 min)' visible='true'/>"));
        Assert.assertEquals(0, count(field("Intensity:", true)
                + "<XCUIElementTypeStaticText name='Stretching PF (+8 min)' visible='true'/>"));
    }
}
