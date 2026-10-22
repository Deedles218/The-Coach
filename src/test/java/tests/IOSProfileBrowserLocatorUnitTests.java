package tests;

import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/** Observed native accessibility contract, without Appium or a test account. */
public class IOSProfileBrowserLocatorUnitTests {
    private static class Locators extends iOSCoachFlowPageObject {
        Locators() { super(null); }
        String faq() { return PROFILE_FAQ_SCREEN; }
        String terms() { return PROFILE_TERMS_SCREEN; }
        String close() { return PROFILE_BROWSER_CLOSE_BUTTON; }
    }

    private int matches(String xml, String locator) throws Exception {
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(("<App>" + xml + "</App>").getBytes(StandardCharsets.UTF_8)));
        NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath().evaluate(
                locator.substring("xpath:".length()), document, XPathConstants.NODESET);
        return nodes.getLength();
    }

    @Test public void currentFaqQuestionIsRecognizedInsideItsWebDocument() throws Exception {
        String xml = "<XCUIElementTypeWebView><XCUIElementTypeOther name='FAQ'>"
                + "<XCUIElementTypeButton name='How do I use The Coach app?'/></XCUIElementTypeOther>"
                + "</XCUIElementTypeWebView>";
        Assert.assertEquals(1, matches(xml, new Locators().faq()));
    }

    @Test public void nativeFaqMenuAndUnrelatedQuestionCannotSatisfyPageReadiness() throws Exception {
        String xml = "<XCUIElementTypeStaticText name='FAQ'/>"
                + "<XCUIElementTypeOther name='FAQ'><XCUIElementTypeButton"
                + " name='How do I use The Coach app?'/></XCUIElementTypeOther>"
                + "<XCUIElementTypeWebView><XCUIElementTypeButton name='How do I use The Coach app?'/>"
                + "</XCUIElementTypeWebView>";
        Assert.assertEquals(0, matches(xml, new Locators().faq()));
    }

    @Test public void emptyLoadingBrowserDoesNotSatisfyEitherDocument() throws Exception {
        String xml = "<XCUIElementTypeOther name='BrowserView?IsPageLoaded=false&amp;WebViewProcessID=123'>"
                + "<XCUIElementTypeOther name='TopBrowserBar'/><XCUIElementTypeWebView/>"
                + "</XCUIElementTypeOther>";
        Locators locators = new Locators();
        Assert.assertEquals(0, matches(xml, locators.faq()));
        Assert.assertEquals(0, matches(xml, locators.terms()));
    }

    @Test public void termsStillRequiresActualDocumentHeading() throws Exception {
        Assert.assertEquals(1, matches("<XCUIElementTypeWebView>"
                + "<XCUIElementTypeStaticText name='The Coach — Terms of Service'/>"
                + "</XCUIElementTypeWebView>", new Locators().terms()));
        Assert.assertEquals(0, matches("<XCUIElementTypeStaticText name='Terms &amp; privacy policy'/>",
                new Locators().terms()));
    }

    @Test public void supportedSafariCloseControlsRemainRecognized() throws Exception {
        for (String label : new String[]{"Close", "Done", "Закрыть", "profile_browser_close"}) {
            Assert.assertEquals(label, 1, matches("<XCUIElementTypeButton name='" + label + "'/>",
                    new Locators().close()));
        }
    }

    private static class TermsBrowser extends iOSCoachFlowPageObject {
        boolean opened = true;
        boolean loading;
        final TimeoutException failure = new TimeoutException("Terms browser did not open");
        TermsBrowser() { super(null); }
        @Override public boolean isElementVisible(String locator) {
            if ("id:TopBrowserBar".equals(locator)) return opened;
            if (locator.contains("BrowserView?IsPageLoaded=false")) return loading;
            return true;
        }
        @Override public void swipeUpToFindFirstVisibleElement(String[] locators, String message, int swipes) { }
        @Override public WebElement waitForFirstElementAndClick(String[] locators, String message, long timeout) {
            return new RemoteWebElement();
        }
        @Override public WebElement waitForElementPresent(String locator, String message, long timeout) {
            Assert.assertEquals(PROFILE_TERMS_SCREEN, locator);
            Assert.assertEquals(20, timeout);
            throw failure;
        }
    }

    @Test public void termsLoadingTimeoutIsNotMisreportedAsFailureToOpenBrowser() {
        TermsBrowser browser = new TermsBrowser();
        browser.loading = true;
        try {
            browser.openTermsFromProfile();
            Assert.fail("An empty loading document must not pass");
        } catch (TimeoutException actual) {
            Assert.assertTrue(actual.getMessage().contains("web document is still loading"));
            Assert.assertSame(browser.failure, actual.getCause());
        }
    }

    @Test public void loadedBrowserWithWrongContentStillFails() {
        TermsBrowser browser = new TermsBrowser();
        try {
            browser.openTermsFromProfile();
            Assert.fail("A loaded browser without Terms content must not pass");
        } catch (TimeoutException actual) {
            Assert.assertTrue(actual.getMessage().contains("expected Terms content is not visible"));
            Assert.assertSame(browser.failure, actual.getCause());
        }
    }

    @Test public void failureBeforeBrowserAppearsPreservesOriginalTimeout() {
        TermsBrowser browser = new TermsBrowser();
        browser.opened = false;
        try {
            browser.openTermsFromProfile();
            Assert.fail("An absent browser must not pass");
        } catch (TimeoutException actual) {
            Assert.assertSame(browser.failure, actual);
        }
    }
}
