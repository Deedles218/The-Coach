package tests;

import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class IOSWelcomeLocatorUnitTests {
    private int count(String xml, String locator) throws Exception {
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return ((NodeList) XPathFactory.newInstance().newXPath().evaluate(
                locator.substring("xpath:".length()), document, XPathConstants.NODESET)).getLength();
    }
    @Test public void originalWelcomeActionsRemainSupported() throws Exception {
        String xml = "<root><XCUIElementTypeButton name='START NOW'/>"
                + "<XCUIElementTypeButton name=\"I'VE ALREADY PURCHASED\"/></root>";
        Assert.assertEquals(1, count(xml, iOSCoachFlowPageObject.WELCOME_START_BUTTON));
        Assert.assertEquals(1, count(xml, iOSCoachFlowPageObject.WELCOME_LOGIN_BUTTON));
    }
    @Test public void alternateWelcomeUsesButtonsInsteadOfDuplicateLabels() throws Exception {
        String xml = "<root><XCUIElementTypeButton name='Get Started'><XCUIElementTypeStaticText name='Get Started'/>"
                + "</XCUIElementTypeButton><XCUIElementTypeButton name='Log in'><XCUIElementTypeStaticText name='Log in'/>"
                + "</XCUIElementTypeButton></root>";
        Assert.assertEquals(1, count(xml, iOSCoachFlowPageObject.WELCOME_START_BUTTON));
        Assert.assertEquals(1, count(xml, iOSCoachFlowPageObject.WELCOME_LOGIN_BUTTON));
    }
    @Test public void standaloneTextCannotSatisfyWelcomeActions() throws Exception {
        String xml = "<root><XCUIElementTypeStaticText name='Get Started'/><XCUIElementTypeStaticText name='Log in'/></root>";
        Assert.assertEquals(0, count(xml, iOSCoachFlowPageObject.WELCOME_START_BUTTON));
        Assert.assertEquals(0, count(xml, iOSCoachFlowPageObject.WELCOME_LOGIN_BUTTON));
    }
}
