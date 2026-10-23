package tests;

import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.Quotes;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/** Exact observed promo titles and scoped close behavior, without Appium or credentials. */
public class IOSPartnerPromoLocatorUnitTests {
    private static final String LEGACY = "Get an access for your partner";
    private static final String CURRENT = "More pleasure for both of you";

    private static class Promo extends iOSCoachFlowPageObject {
        final Document document;
        boolean clicked;
        boolean dismissalChecked;
        boolean keepOpen;
        TimeoutException clickFailure;

        Promo(String title, String closeVisibility) throws Exception {
            super(new RemoteWebDriver() { });
            String xml = "<App><XCUIElementTypeButton target='unrelated-close' visible='true'/>"
                    + "<XCUIElementTypeOther><XCUIElementTypeStaticText name='" + title + "' visible='true'/>"
                    + "<XCUIElementTypeButton target='partner-close' visible='" + closeVisibility + "'/>"
                    + "<XCUIElementTypeButton name='GIFT HER ACCESS' visible='true'/></XCUIElementTypeOther></App>";
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        }

        NodeList nodes(String locator) {
            String xpath = locator.startsWith("xpath:") ? locator.substring(6)
                    : "//*[@name=" + Quotes.escape(locator.substring(3)) + "]";
            try {
                return (NodeList) XPathFactory.newInstance().newXPath().evaluate(xpath, document, XPathConstants.NODESET);
            } catch (Exception failure) { throw new AssertionError(failure); }
        }

        @Override public boolean isElementVisible(String locator) {
            NodeList matches = nodes(locator);
            for (int i = 0; i < matches.getLength(); i++) {
                if ("true".equals(((Element) matches.item(i)).getAttribute("visible"))) return true;
            }
            return false;
        }

        @Override public WebElement waitForElementAndClick(String locator, String message, long timeout) {
            if (clickFailure != null) throw clickFailure;
            NodeList matches = nodes(locator);
            if (matches.getLength() == 0) throw new TimeoutException(message);
            Assert.assertEquals("Close must stay scoped to the promo", 1, matches.getLength());
            Element close = (Element) matches.item(0);
            Assert.assertEquals("Never choose Gift Access or another screen's close", "partner-close", close.getAttribute("target"));
            clicked = true;
            if (!keepOpen) close.getParentNode().getParentNode().removeChild(close.getParentNode());
            return new RemoteWebElement();
        }

        @Override public void waitForElementNotVisible(String locator, String message, long timeout) {
            dismissalChecked = true;
            if (isElementVisible(locator)) throw new TimeoutException(message);
        }

        @Override public WebDriverWait createWait(long timeout) {
            return new WebDriverWait(driver, Duration.ZERO);
        }

        @Override protected boolean isAuthorizedDashboardVisible() { return clicked && !keepOpen; }
    }

    @Test public void legacyPromoStillClosesItsOwnUnnamedButton() throws Exception {
        Promo promo = new Promo(LEGACY, "true");
        promo.closePartnerPromoIfPresent();
        Assert.assertTrue(promo.clicked);
        Assert.assertTrue(promo.dismissalChecked);
    }

    @Test public void currentPromoClosesItsOwnUnnamedButton() throws Exception {
        Promo promo = new Promo(CURRENT, "true");
        promo.closePartnerPromoIfPresent();
        Assert.assertTrue(promo.clicked);
        Assert.assertTrue(promo.dismissalChecked);
    }

    @Test public void unrelatedPromoCannotTriggerAnArbitraryClose() throws Exception {
        Promo promo = new Promo("An unrelated offer", "true");
        promo.closePartnerPromoIfPresent();
        Assert.assertFalse(promo.clicked);
        Assert.assertFalse(promo.dismissalChecked);
    }

    @Test(expected = TimeoutException.class) public void hiddenCloseDoesNotFallBackToAnotherScreen() throws Exception {
        new Promo(CURRENT, "false").closePartnerPromoIfPresent();
    }

    @Test public void originalCloseFailureIsNotSwallowed() throws Exception {
        Promo promo = new Promo(CURRENT, "true");
        promo.clickFailure = new TimeoutException("Injected close failure");
        try {
            promo.closePartnerPromoIfPresent();
            Assert.fail("An unclosed promo must fail");
        } catch (TimeoutException failure) { Assert.assertSame(promo.clickFailure, failure); }
    }

    @Test(expected = TimeoutException.class) public void aPromoThatSurvivesTheClickStillFails() throws Exception {
        Promo promo = new Promo(CURRENT, "true");
        promo.keepOpen = true;
        promo.closePartnerPromoIfPresent();
    }

    @Test public void authorizationDismissesCurrentPromoBeforeRequiringToday() throws Exception {
        Promo promo = new Promo(CURRENT, "true");
        promo.waitForAuthorizedDashboard();
        Assert.assertTrue(promo.clicked);
        Assert.assertTrue(promo.dismissalChecked);
    }

    @Test public void authorizationAlsoDismissesTheLegacyPromo() throws Exception {
        Promo promo = new Promo(LEGACY, "true");
        promo.waitForAuthorizedDashboard();
        Assert.assertTrue(promo.clicked);
    }
}
