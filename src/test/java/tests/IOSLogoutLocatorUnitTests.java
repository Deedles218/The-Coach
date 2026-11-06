package tests;

import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

public class IOSLogoutLocatorUnitTests {
    private static class Screen extends iOSCoachFlowPageObject {
        Screen() { super(new RemoteWebDriver() { }); }
        String confirmation() { return LOG_OUT_CONFIRM_TITLE.substring(6); }
    }

    private int matches(String name, String label) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element title = document.createElement("XCUIElementTypeStaticText");
        title.setAttribute("name", name);
        title.setAttribute("label", label);
        document.appendChild(title);
        NodeList result = (NodeList) XPathFactory.newInstance().newXPath().evaluate(
                new Screen().confirmation(), document, XPathConstants.NODESET);
        return result.getLength();
    }

    @Test public void legacyLogoutTitleStillMatches() throws Exception {
        Assert.assertEquals(1, matches("Log out of your account?", "Log out of your account?"));
    }

    @Test public void identifiedTitleUsesExactLogoutLabel() throws Exception {
        Assert.assertEquals(1, matches("TitleBlock.Title", "Log out of your account?"));
    }

    @Test public void identifiedAnonymousLogoutTitleMatches() throws Exception {
        Assert.assertEquals(1, matches("TitleBlock.Title", "Your account is currently anonymous because no email is linked."));
    }

    @Test public void unrelatedConfirmationDoesNotMatch() throws Exception {
        Assert.assertEquals(0, matches("TitleBlock.Title", "Delete your account?"));
    }
}
