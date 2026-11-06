package tests;

import lib.ui.ios.iOSOnboardingPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

public class IOSQuestionnaireLocatorUnitTests {
    private int matches(boolean question, boolean scale, boolean visible) throws Exception {
        String xml = "<App><XCUIElementTypeWebView>"
                + (question ? "<XCUIElementTypeStaticText name='How satisfied are you with your intimate life?'/>": "")
                + (scale ? "<XCUIElementTypeStaticText name='Rate from 1 to 5:'/>": "")
                + "<XCUIElementTypeStaticText name='1' visible='" + visible + "' enabled='true' y='297'/>"
                + "<XCUIElementTypeStaticText name='Not at all' visible='true' enabled='true' y='340'/>"
                + "</XCUIElementTypeWebView></App>";
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return ((NodeList) XPathFactory.newInstance().newXPath().evaluate(
                iOSOnboardingPageObject.satisfactionRatingAnswerLocator().substring(6),
                document, XPathConstants.NODESET)).getLength();
    }

    @Test public void observedAnswerAboveOldCoordinateCutoffIsAccepted() throws Exception {
        Assert.assertEquals(1, matches(true, true, true));
    }
    @Test public void unrelatedNumberAndUnconfirmedScaleAreRejected() throws Exception {
        Assert.assertEquals(0, matches(false, true, true));
        Assert.assertEquals(0, matches(true, false, true));
    }
    @Test public void hiddenAnswerIsRejected() throws Exception {
        Assert.assertEquals(0, matches(true, true, false));
    }
}
