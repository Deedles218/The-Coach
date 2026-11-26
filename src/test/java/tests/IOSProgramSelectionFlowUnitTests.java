package tests;

import lib.ui.ios.IOSProgramSelectionPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/** Replays the native v3 card/footer contract, including the independent Skip action. */
public class IOSProgramSelectionFlowUnitTests {
    @Test public void emptySelectionUsesSkipAndSummaryHasNoAddedOrEditAction() {
        FlowDriver driver = new FlowDriver();
        IOSProgramSelectionPageObject page = new IOSProgramSelectionPageObject(driver);
        page.selectOnly(Collections.emptyList());
        page.openSummary();
        Assert.assertEquals("SKIP", driver.savedWith);
        page.assertSummary("Main", Collections.emptyList(), Arrays.asList("Program B", "Program C"));
    }

    @Test public void deselectingOneOfTwoSavesExactlyOneAndUsesTheCountedContinue() {
        FlowDriver driver = new FlowDriver();
        IOSProgramSelectionPageObject page = new IOSProgramSelectionPageObject(driver);
        page.selectOnly(Arrays.asList("Program B", "Program C"));
        page.toggle("Program C");
        page.assertContinueCount(1);
        page.openSummary();
        Assert.assertEquals("Continue 1 selected", driver.savedWith);
        page.assertSummary("Main", Collections.singletonList("Program B"), Arrays.asList("Program B", "Program C"));
    }

    @Test public void twoSelectionsProduceTwoSeparateAddedEntries() {
        FlowDriver driver = new FlowDriver();
        IOSProgramSelectionPageObject page = new IOSProgramSelectionPageObject(driver);
        List<String> chosen = Arrays.asList("Program B", "Program C");
        page.selectOnly(chosen);
        page.openSummary();
        Assert.assertEquals("Continue 2 selected", driver.savedWith);
        page.assertSummary("Main", chosen, chosen);
    }

    @Test(expected = AssertionError.class) public void anEnabledEmptyContinueIsAContractFailure() {
        FlowDriver driver = new FlowDriver();
        driver.emptyContinueEnabled = true;
        new IOSProgramSelectionPageObject(driver).assertContinueCount(0);
    }

    @Test public void mainRadioSelectionCanChangeAndReturnToTheRecommendation() {
        FlowDriver driver = new FlowDriver();
        driver.mainPicker = true;
        IOSProgramSelectionPageObject page = new IOSProgramSelectionPageObject(driver);
        page.assertMainCatalog(mainCatalog(), "main");
        page.toggle("Alternative");
        Assert.assertEquals("Alternative", driver.mainChoice);
        page.toggle("Main");
        page.assertMainCatalog(mainCatalog(), "main");
    }

    @Test(expected = AssertionError.class) public void recommendedBadgeCannotSubstituteForASelectedMainRadio() {
        FlowDriver driver = new FlowDriver();
        driver.mainPicker = true;
        driver.mainChoice = "Alternative";
        new IOSProgramSelectionPageObject(driver).assertMainCatalog(mainCatalog(), "main");
    }

    @Test public void informationalDetailsRejectEveryAssignmentRouteIgnoringCase() {
        for (String action : Arrays.asList("Add and start today", "add to daily plan",
                "Make It My Main Journey", "sTaRt WiTh Last Longer")) {
            FlowDriver driver = new FlowDriver();
            driver.informationAction = action;
            try {
                new IOSProgramSelectionPageObject(driver).assertInformationalDetails();
                Assert.fail("An informational preview accepted an assignment action");
            } catch (AssertionError expected) {
                Assert.assertTrue(expected.getMessage().contains("Learn More must not launch or choose"));
            }
        }
    }

    @Test public void informationalDetailsAllowLearnMoreWithoutAssignment() {
        new IOSProgramSelectionPageObject(new FlowDriver()).assertInformationalDetails();
    }

    @Test public void hiddenAssignmentActionIsNotAnAvailableInformationControl() {
        FlowDriver driver = new FlowDriver();
        driver.informationAction = "START WITH Last Longer";
        driver.informationActionVisible = false;
        new IOSProgramSelectionPageObject(driver).assertInformationalDetails();
    }

    private static List<Map<String, Object>> mainCatalog() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String title : Arrays.asList("Main", "Alternative")) {
            Map<String, Object> row = new HashMap<>();
            row.put("program_id", title.equals("Main") ? "main" : "alternative");
            row.put("title", title);
            row.put("is_recommended", title.equals("Main"));
            rows.add(row);
        }
        return rows;
    }

    private static final class FlowDriver extends RemoteWebDriver {
        final Set<String> selected = new LinkedHashSet<>();
        boolean summary;
        boolean emptyContinueEnabled;
        boolean mainPicker;
        String mainChoice = "Main";
        String savedWith;
        String informationAction;
        boolean informationActionVisible = true;

        @Override public WebElement findElement(By by) {
            List<WebElement> found = findElements(by);
            if (found.isEmpty()) throw new NoSuchElementException(by.toString());
            return found.get(0);
        }

        @Override public List<WebElement> findElements(By by) {
            String locator = by.toString();
            String xpath = locator.startsWith("By.id: ") ? "//*[@name='" + locator.substring(7) + "']"
                    : locator.substring("By.xpath: ".length());
            StringBuilder xml = new StringBuilder("<App>");
            if (mainPicker) {
                xml.append(node("XCUIElementTypeStaticText", "Choose your main journey", true));
                for (String title : Arrays.asList("Main", "Alternative")) {
                    xml.append("<XCUIElementTypeOther><XCUIElementTypeOther><XCUIElementTypeOther><XCUIElementTypeImage/>");
                    if (mainChoice.equals(title)) {
                        xml.append("<XCUIElementTypeOther/><XCUIElementTypeOther><XCUIElementTypeOther/></XCUIElementTypeOther>");
                    } else {
                        xml.append("<XCUIElementTypeOther><XCUIElementTypeOther><XCUIElementTypeOther/></XCUIElementTypeOther></XCUIElementTypeOther>");
                    }
                    if (title.equals("Main")) xml.append(node("XCUIElementTypeStaticText", "RECOMMENDED", true));
                    xml.append("</XCUIElementTypeOther><XCUIElementTypeOther>")
                            .append(node("XCUIElementTypeStaticText", title, true))
                            .append(node("XCUIElementTypeStaticText", "Description", true))
                            .append("</XCUIElementTypeOther></XCUIElementTypeOther><XCUIElementTypeOther>")
                            .append(node("XCUIElementTypeButton", "LEARN MORE", true))
                            .append("</XCUIElementTypeOther></XCUIElementTypeOther>");
                }
                xml.append(node("XCUIElementTypeButton", "CHANGE MAIN JOURNEY", !mainChoice.equals("Main")));
                xml.append(node("XCUIElementTypeButton", "GO BACK", true));
            } else if (summary) {
                xml.append(node("XCUIElementTypeStaticText", "Your plan is ready", true));
                xml.append(node("XCUIElementTypeStaticText", "MAIN JOURNEY", true));
                xml.append(node("XCUIElementTypeStaticText", "Main", true));
                for (String title : selected) {
                    xml.append(node("XCUIElementTypeStaticText", title, true));
                    xml.append(node("XCUIElementTypeStaticText", "ADDED", true));
                }
                xml.append(node("XCUIElementTypeButton", "GO TO MY DAILY PLAN", true));
                xml.append(node("XCUIElementTypeStaticText", "You can change your journeys at any time.", true));
            } else {
                xml.append(node("XCUIElementTypeStaticText", "What else would you like to work on?", true));
                for (String title : Arrays.asList("Program B", "Program C")) {
                    xml.append("<XCUIElementTypeOther><XCUIElementTypeOther><XCUIElementTypeOther><XCUIElementTypeImage/>");
                    if (selected.contains(title)) xml.append(node("XCUIElementTypeStaticText", "✓", true));
                    xml.append("</XCUIElementTypeOther><XCUIElementTypeOther>")
                            .append(node("XCUIElementTypeStaticText", title, true))
                            .append(node("XCUIElementTypeStaticText", "Description", true))
                            .append("</XCUIElementTypeOther></XCUIElementTypeOther><XCUIElementTypeOther>")
                            .append(node("XCUIElementTypeButton", "LEARN MORE", true))
                            .append("</XCUIElementTypeOther></XCUIElementTypeOther>");
                }
                xml.append(node("XCUIElementTypeButton", selected.isEmpty() ? "Continue"
                        : "Continue " + selected.size() + " selected", !selected.isEmpty() || emptyContinueEnabled));
                xml.append(node("XCUIElementTypeButton", "SKIP", true));
            }
            if (informationAction != null) {
                String action = node("XCUIElementTypeButton", informationAction, true);
                xml.append(informationActionVisible ? action : action.replace("visible='true'", "visible='false'"));
            }
            xml.append("</App>");
            try {
                Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                        new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8)));
                NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath().evaluate(xpath, document, XPathConstants.NODESET);
                List<WebElement> found = new ArrayList<>();
                for (int i = 0; i < nodes.getLength(); i++) {
                    final Element node = (Element) nodes.item(i);
                    found.add(new RemoteWebElement() {
                        @Override public boolean isDisplayed() { return !"false".equals(node.getAttribute("visible")); }
                        @Override public boolean isEnabled() { return "true".equals(node.getAttribute("enabled")); }
                        @Override public String getAttribute(String key) { return node.getAttribute(key); }
                        @Override public void click() {
                            String name = node.getAttribute("name");
                            if (mainPicker && (name.equals("Main") || name.equals("Alternative"))) {
                                mainChoice = name;
                            } else if (name.startsWith("Program ")) {
                                if (!selected.add(name)) selected.remove(name);
                            } else {
                                Assert.assertTrue("Unexpected save control", "SKIP".equals(name) || name.startsWith("Continue "));
                                savedWith = name;
                                summary = true;
                            }
                        }
                    });
                }
                return found;
            } catch (Exception failure) { throw new AssertionError(failure); }
        }

        private static String node(String type, String name, boolean enabled) {
            return "<" + type + " name='" + name + "' label='" + name + "' visible='true' enabled='" + enabled + "'/>";
        }
    }
}
