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

/** Approved male production v7 content must not leak into For Her or preprod. */
public class IOSConfiguredExploreContractUnitTests {
    private static class Locators extends iOSExplorePageObject {
        Locators() { super(null); }
        boolean maleProduction() { return usesMaleProductionContract(); }
        String[] sections() { return configuredSectionTitles(); }
        String section(String title) { return configuredSection(title); }
        String action() { return coachingAction(); }
        String booking() { return coachingBookingLocator(); }
        String quickTip() { return quickTipCardLocator(); }
        String article() { return quickTipArticleTitleLocator(); }
    }

    private void variant(String bundle, java.util.function.Consumer<Locators> assertions) {
        String previous = System.getProperty("ios.bundleId");
        try {
            System.setProperty("ios.bundleId", bundle);
            assertions.accept(new Locators());
        } finally {
            if (previous == null) System.clearProperty("ios.bundleId");
            else System.setProperty("ios.bundleId", previous);
            new Locators();
        }
    }

    private int count(String xml, String locator) {
        try {
            Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new ByteArrayInputStream(("<App>" + xml + "</App>").getBytes(StandardCharsets.UTF_8)));
            return ((NodeList) XPathFactory.newInstance().newXPath().evaluate(
                    locator.substring("xpath:".length()), document, XPathConstants.NODESET)).getLength();
        } catch (Exception failure) { throw new AssertionError(failure); }
    }

    private String section(String title, String content) {
        return "<XCUIElementTypeOther><XCUIElementTypeStaticText label='" + title + "' visible='true'/>"
                + "<XCUIElementTypeCollectionView><XCUIElementTypeCell>" + content
                + "</XCUIElementTypeCell></XCUIElementTypeCollectionView></XCUIElementTypeOther>";
    }

    @Test public void maleProductionRequiresBodyPracticesAndPluralCoaching() {
        variant("com.vamapps.The-Coach", page -> {
            Assert.assertTrue(page.maleProduction());
            Assert.assertArrayEquals(new String[]{"Quick Tips", "Master Classes", "Private Coaching Sessions", "Body Practices"}, page.sections());
        });
    }

    @Test public void forHerKeepsItsExistingContract() {
        variant("com.vamapps.The-Coach-for-her", page -> {
            Assert.assertFalse(page.maleProduction());
            Assert.assertArrayEquals(new String[]{"Quick Tips", "Master Classes", "Private coaching session"}, page.sections());
            Assert.assertTrue(page.action().contains("learn more"));
        });
    }

    @Test public void otherBundlesDoNotInheritProductionContract() {
        variant("com.vamapps.The-Coach.preprod", page -> Assert.assertFalse(page.maleProduction()));
    }

    @Test public void maleBookActionIsScopedToThePluralSection() {
        variant("com.vamapps.The-Coach", page -> {
            String button = "<XCUIElementTypeButton type='XCUIElementTypeButton' label='BOOK' visible='true'/>";
            Assert.assertEquals(1, count(section("Private Coaching Sessions", button), page.action()));
            Assert.assertEquals(0, count(section("Quick Tips", button), page.action()));
            Assert.assertEquals(0, count(section("Private coaching session", button), page.action()));
        });
    }

    @Test public void oldLearnMoreCannotSatisfyProductionBookAction() {
        variant("com.vamapps.The-Coach", page -> Assert.assertEquals(0, count(
                section("Private Coaching Sessions", "<XCUIElementTypeButton type='XCUIElementTypeButton' label='LEARN MORE' visible='true'/>"), page.action())));
    }

    @Test public void hiddenBookActionCannotSatisfyTheContract() {
        variant("com.vamapps.The-Coach", page -> Assert.assertEquals(0, count(
                section("Private Coaching Sessions", "<XCUIElementTypeButton type='XCUIElementTypeButton' label='BOOK' visible='false'/>"), page.action())));
    }

    @Test public void firstQuickTipIsSelectedByItsConfiguredTitleAndSection() {
        variant("com.vamapps.The-Coach", page -> {
            String card = "<XCUIElementTypeStaticText label='Finish Too Soon' visible='true'/>";
            Assert.assertEquals(1, count(section("Quick Tips", card), page.quickTip()));
            Assert.assertEquals(0, count(section("Masterclasses", card), page.quickTip()));
            Assert.assertEquals(0, count(section("Quick Tips", card.replace("Finish Too Soon", "Other lesson")), page.quickTip()));
        });
    }

    @Test public void articleDestinationRequiresItsObservedReaderTitle() {
        variant("com.vamapps.The-Coach", page -> Assert.assertEquals("id:When You’re About to Finish Too Soon", page.article()));
    }

    @Test public void bookingLinkUsesTheCurrentRelationshipLandingNotLegacyConsultation() {
        variant("com.vamapps.The-Coach", page -> {
            Assert.assertEquals(1, count("<XCUIElementTypeLink label='Book a private 1:1 session'/>", page.booking()));
            Assert.assertEquals(0, count("<XCUIElementTypeLink label='Book 1:1 a private consultation'/>", page.booking()));
        });
    }

    @Test public void masterclassesHeadingAllowsObservedCapitalizationAndSpacing() {
        variant("com.vamapps.The-Coach", page -> {
            Assert.assertEquals(1, count(section("Masterclasses", ""), page.section("Master Classes")));
            Assert.assertEquals(1, count(section("MASTER CLASSES", ""), page.section("Master Classes")));
        });
    }
}
