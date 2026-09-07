package tests;

import lib.ui.DailyPlanPageObject;
import lib.ui.ios.iOSProgramSelectorPageObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.lang.reflect.Field;

/** Deferred scope, fixture allowlists, and observed day-label variants; no Appium. */
public class TestModelScopeUnitTests {
    @Test public void coachForHerCaseIsExplicitlyDeferred() throws Exception {
        Ignore ignored = TestModelAutomationTests.class.getMethod("test08Coa8518FirstLockedNextDayTapShowsPopup")
                .getAnnotation(Ignore.class);
        Assert.assertNotNull(ignored);
        Assert.assertTrue(ignored.value().contains("Coach for Her"));
    }

    @Test(expected = AssertionError.class)
    public void existingSelectorFixtureScopeIsNotExpanded() {
        new iOSProgramSelectorPageObject(null).selectProgram("Overall Health");
    }

    @Test(expected = AssertionError.class)
    public void customizationInspectionCannotSelectAnUnrelatedProgram() {
        new iOSProgramSelectorPageObject(null).selectCustomizationInspectionProgram("Sex Is a Skill");
    }

    @Test public void maintenanceDayWithoutTotalIsReadable() throws Exception { assertDay("Day 42"); }
    @Test public void ordinaryDayWithTotalIsStillReadable() throws Exception { assertDay("Day 2 of 70"); }
    @Test public void moduleStageIsStillReadable() throws Exception { assertDay("Stage 1 of 7"); }

    private void assertDay(String label) throws Exception {
        new iOSProgramSelectorPageObject(null);
        Field field = DailyPlanPageObject.class.getDeclaredField("CURRENT_DAY_LABEL");
        field.setAccessible(true);
        String xpath = ((String)field.get(null)).substring("xpath:".length());
        String xml = "<Application><XCUIElementTypeOther name='DailyDaySwitcherView'>"
                + "<XCUIElementTypeStaticText name='" + label + "'/></XCUIElementTypeOther>"
                + "<XCUIElementTypeStaticText name='Day 999'/></Application>";
        org.w3c.dom.Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
        org.w3c.dom.NodeList matches = (org.w3c.dom.NodeList)XPathFactory.newInstance().newXPath()
                .evaluate(xpath, doc, XPathConstants.NODESET);
        Assert.assertEquals("Only the day switcher label may match", 1, matches.getLength());
        Assert.assertEquals(label, matches.item(0).getAttributes().getNamedItem("name").getNodeValue());
    }
}
