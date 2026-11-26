package tests;

import lib.Platform;
import lib.ui.android.AndroidCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** The current CONNECT capture and legacy Login must share the email-form entry point. */
public class AndroidLoginFormLocatorUnitTests {
    private static class Form extends AndroidCoachFlowPageObject {
        Form() { super(null); }
        String title() { return LOGIN_SCREEN_TITLE.substring(6); }
    }
    private int matches(String id, String title) throws Exception {
        String xml="<hierarchy><android.widget.TextView resource-id='"+id+"' text='"+title+"'/></hierarchy>";
        Object document=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return ((org.w3c.dom.NodeList)XPathFactory.newInstance().newXPath().evaluate(
                new Form().title(),document,XPathConstants.NODESET)).getLength();
    }
    private String id() { return Platform.getInstance().getAndroidAppPackage()+":id/tvTitleText"; }
    @Test public void currentSyncFormIsRecognized() throws Exception {
        Assert.assertEquals(1,matches(id(),"ENTER YOUR EMAIL TO SYNC YOUR PROGRESS AND SETTINGS"));
    }
    @Test public void legacyLoginStillOpens() throws Exception {
        Assert.assertEquals(1,matches(id(),"ENTER THE EMAIL YOU USED TO REGISTER"));
    }
    @Test public void otpCannotBeMistakenForAnEmailForm() throws Exception {
        Assert.assertEquals(0,matches(id(),"ENTER SECURITY CODE"));
    }
    @Test public void lessonTextCannotBecomeALoginScreen() throws Exception {
        Assert.assertEquals(0,matches(Platform.getInstance().getAndroidAppPackage()+":id/tvTextContent",
                "ENTER YOUR EMAIL TO SYNC YOUR PROGRESS AND SETTINGS"));
    }
    @Test public void anotherAppCannotSupplyTheTitle() throws Exception {
        Assert.assertEquals(0,matches("another.app:id/tvTitleText","ENTER YOUR EMAIL TO SYNC YOUR PROGRESS AND SETTINGS"));
    }
}
