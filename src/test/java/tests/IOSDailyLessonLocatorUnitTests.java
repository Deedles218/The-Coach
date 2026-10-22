package tests;

import lib.ui.ios.iOSDailyPlanPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.openqa.selenium.WebElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/** Observed Daily Plan card structure; lesson content is program-specific. */
public class IOSDailyLessonLocatorUnitTests {
    private String card(String title, String metadata) {
        return "<XCUIElementTypeCell><XCUIElementTypeOther>"
                + "<XCUIElementTypeImage/><XCUIElementTypeStaticText name='" + title + "'/>"
                + "<XCUIElementTypeImage/><XCUIElementTypeOther>"
                + "<XCUIElementTypeStaticText name='" + metadata + "'/>"
                + "<XCUIElementTypeStaticText name='2 MIN'/></XCUIElementTypeOther>"
                + "</XCUIElementTypeOther></XCUIElementTypeCell>";
    }

    private NodeList select(String cards, boolean today) throws Exception {
        String xml = "<App><XCUIElementTypeCollectionView>"
                + (today ? "<XCUIElementTypeOther name='DailyDaySwitcherView'/>" : "")
                + cards + "</XCUIElementTypeCollectionView></App>";
        Object doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return (NodeList) XPathFactory.newInstance().newXPath().evaluate(
                iOSDailyPlanPageObject.firstLessonTitleLocator().substring("xpath:".length()),
                doc, XPathConstants.NODESET);
    }

    private void assertTitle(String title) throws Exception {
        NodeList found = select(card(title, "Lesson 1") + card("Second lesson", "Lesson 2"), true);
        Assert.assertEquals(1, found.getLength());
        Assert.assertEquals(title, found.item(0).getAttributes().getNamedItem("name").getNodeValue());
    }

    @Test public void overallHealthUsesActualFirstLesson() throws Exception { assertTitle("What is male hormonal balance?"); }
    @Test public void lastLongerStillFindsStartingPoint() throws Exception { assertTitle("Starting Point"); }
    @Test public void gettingStartedStillWorks() throws Exception { assertTitle("Getting started"); }
    @Test public void newProgramTitlesDoNotRequireLocatorChanges() throws Exception { assertTitle("A different first lesson"); }
    @Test public void secondLessonCannotReplaceMissingFirstLesson() throws Exception {
        Assert.assertEquals(0, select(card("Starting Point", "Lesson 2"), true).getLength());
    }
    @Test public void practiceCannotSatisfyLessonLocator() throws Exception {
        Assert.assertEquals(0, select(card("Starting Point", "Guide"), true).getLength());
    }
    @Test public void anotherScreenCannotSatisfyLessonLocator() throws Exception {
        Assert.assertEquals(0, select(card("Starting Point", "Lesson 1"), false).getLength());
    }

    private static class ExitFlow extends iOSDailyPlanPageObject {
        final List<String> actions = new ArrayList<>();
        boolean confirmation;
        boolean needsConfirmation;
        boolean returned;
        ExitFlow() { super(null); }
        static boolean body(String text, String title) { return isLessonBodyText(text, title); }
        @Override public boolean isElementVisible(String locator) {
            Assert.assertEquals(KEGEL_PLAYER_EXIT_CONFIRM_TITLE, locator);
            return confirmation;
        }
        @Override public WebElement waitForElementAndClick(String locator, String message, long timeout) {
            if (LESSON_SCREEN_BACK_BUTTON.equals(locator)) {
                Assert.assertTrue(locator.contains("ic outline close"));
                actions.add("close");
            } else {
                Assert.assertEquals(KEGEL_PLAYER_EXIT_CONFIRM_QUIT_BUTTON, locator);
                actions.add("quit");
                confirmation = false;
            }
            return null;
        }
        @Override public WebElement waitForFirstElementPresent(String[] locators, String message, long timeout) {
            Assert.assertTrue(Arrays.asList(locators).contains("id:Today"));
            Assert.assertTrue(Arrays.asList(locators).contains(KEGEL_PLAYER_EXIT_CONFIRM_TITLE));
            confirmation = confirmation || needsConfirmation;
            return null;
        }
        @Override public WebElement waitForElementPresent(String locator, String message, long timeout) {
            Assert.assertEquals("id:Today", locator);
            Assert.assertFalse(confirmation);
            returned = true;
            return null;
        }
        @Override public void waitForElementNotVisible(String locator, String message, long timeout) {
            Assert.assertEquals(KEGEL_PLAYER_EXIT_CONFIRM_TITLE, locator);
            Assert.assertFalse(confirmation);
        }
    }

    @Test public void closingUnfinishedLessonConfirmsQuitAndVerifiesToday() {
        ExitFlow flow = new ExitFlow();
        flow.needsConfirmation = true;
        flow.returnFromDailyLessonToDailyPlan();
        Assert.assertEquals(Arrays.asList("close", "quit"), flow.actions);
        Assert.assertTrue(flow.returned);
    }
    @Test public void directReturnDoesNotClickNonexistentConfirmation() {
        ExitFlow flow = new ExitFlow();
        flow.returnFromDailyLessonToDailyPlan();
        Assert.assertEquals(Arrays.asList("close"), flow.actions);
        Assert.assertTrue(flow.returned);
    }
    @Test public void alreadyOpenConfirmationIsHandledWithoutSecondClose() {
        ExitFlow flow = new ExitFlow();
        flow.confirmation = true;
        flow.returnFromDailyLessonToDailyPlan();
        Assert.assertEquals(Arrays.asList("quit"), flow.actions);
        Assert.assertTrue(flow.returned);
    }
    @Test public void longSelectedTitleCannotStandInForMissingBody() {
        String title = "What is male hormonal balance?";
        Assert.assertFalse(ExitFlow.body(title, title));
    }
    @Test public void actualBodyRemainsAccepted() {
        Assert.assertTrue(ExitFlow.body("Welcome to the first lesson in this program.", "Starting Point"));
    }
    @Test public void emptyAndShortMetadataCannotStandInForBody() {
        Assert.assertFalse(ExitFlow.body(null, "Starting Point"));
        Assert.assertFalse(ExitFlow.body("2 MIN", "Starting Point"));
    }
}
