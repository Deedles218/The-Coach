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

/** Observed module Today: lesson numbering continues across modules and stages. */
public class IOSModuleLessonLocatorUnitTests {
    private static final class Locators extends iOSDailyPlanPageObject {
        Locators() { super(null); }
        String metadata() { return FIRST_LESSON_TYPE; }
    }

    private String header(String section) {
        return "<XCUIElementTypeCell><XCUIElementTypeStaticText name='TitleBlock.Title' label='"
                + section + "'/></XCUIElementTypeCell>";
    }

    private String card(String title, String type) {
        return "<XCUIElementTypeCell><XCUIElementTypeOther name='DailyPlanItem'>"
                + "<XCUIElementTypeOther><XCUIElementTypeImage/>"
                + "<XCUIElementTypeStaticText name='" + title + "'/><XCUIElementTypeImage/>"
                + "<XCUIElementTypeOther><XCUIElementTypeStaticText name='" + type + "'/>"
                + "</XCUIElementTypeOther></XCUIElementTypeOther></XCUIElementTypeOther></XCUIElementTypeCell>";
    }

    private NodeList select(String cards, boolean module, boolean today) throws Exception {
        return select(cards, module, today, iOSDailyPlanPageObject.firstLessonTitleLocator());
    }

    private NodeList select(String cards, boolean module, boolean today, String locator) throws Exception {
        String xml = "<App><XCUIElementTypeCollectionView><XCUIElementTypeCell>"
                + (module ? "<XCUIElementTypeStaticText name='Module 3: Sex Flow Mastery'/>" : "")
                + (today ? "<XCUIElementTypeOther name='DailyDaySwitcherView'/>" : "")
                + "</XCUIElementTypeCell>" + cards + "</XCUIElementTypeCollectionView></App>";
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return (NodeList) XPathFactory.newInstance().newXPath().evaluate(
                locator.substring("xpath:".length()),
                document, XPathConstants.NODESET);
    }

    @Test public void firstAvailableModuleLessonNeedNotBeNumberOne() throws Exception {
        NodeList found = select(header("DAILY LESSONS") + card("Best Positions for Her", "Lesson 29")
                + card("Depth and Angle Control", "Lesson 30"), true, true);
        Assert.assertEquals("Only the first available lesson", 1, found.getLength());
        Assert.assertEquals("Best Positions for Her", found.item(0).getAttributes().getNamedItem("name").getNodeValue());
    }

    @Test public void numberedModuleLessonRequiresObservedModuleHeader() throws Exception {
        Assert.assertEquals(0, select(header("DAILY LESSONS") + card("Later lesson", "Lesson 29"), false, true).getLength());
    }

    @Test public void numberedModuleLessonRequiresTodayDaySwitcher() throws Exception {
        Assert.assertEquals(0, select(header("DAILY LESSONS") + card("Later lesson", "Lesson 29"), true, false).getLength());
    }

    @Test public void numberedModuleLessonRequiresItsSectionHeading() throws Exception {
        Assert.assertEquals(0, select(card("Later lesson", "Lesson 29"), true, true).getLength());
    }

    @Test public void practiceWithLessonMetadataCannotSatisfyModuleLessons() throws Exception {
        Assert.assertEquals(0, select(header("DAILY LESSONS") + header("DAILY PRACTICE")
                + card("Wrong section", "Lesson 29"), true, true).getLength());
    }

    @Test public void catchUpCannotSatisfyModuleLessons() throws Exception {
        Assert.assertEquals(0, select(header("DAILY LESSONS") + header("TO CATCH-UP")
                + card("Catch-up lesson", "Lesson 29"), true, true).getLength());
    }

    @Test public void legacyNumberOneCannotEscapeModuleSectionScope() throws Exception {
        Assert.assertEquals(0, select(header("DAILY PRACTICE")
                + card("Wrong section", "Lesson 1"), true, true).getLength());
    }

    @Test public void guideIsNotAModuleLesson() throws Exception {
        Assert.assertEquals(0, select(header("DAILY LESSONS") + card("Guide", "Guide"), true, true).getLength());
    }

    @Test public void arbitraryLessonPrefixIsNotNumericMetadata() throws Exception {
        Assert.assertEquals(0, select(header("DAILY LESSONS") + card("Wrong type", "Lesson notes"), true, true).getLength());
    }

    @Test public void zeroIsNotAnObservedLessonNumber() throws Exception {
        Assert.assertEquals(0, select(header("DAILY LESSONS") + card("Wrong type", "Lesson 0"), true, true).getLength());
    }

    @Test public void blankTitleCannotSatisfyModuleReadiness() throws Exception {
        Assert.assertEquals(0, select(header("DAILY LESSONS") + card(" ", "Lesson 29"), true, true).getLength());
    }

    @Test public void legacyNumberOneInsideModuleLessonsStillWorks() throws Exception {
        NodeList found = select(header("DAILY LESSONS") + card("First lesson", "Lesson 1")
                + card("Second lesson", "Lesson 2"), true, true);
        Assert.assertEquals(1, found.getLength());
        Assert.assertEquals("First lesson", found.item(0).getAttributes().getNamedItem("name").getNodeValue());
    }

    @Test public void metadataBelongsToFirstModuleLessonNotAnotherCard() throws Exception {
        NodeList found = select(header("DAILY LESSONS") + card("First available lesson", "Lesson 29")
                + card("Second available lesson", "Lesson 30") + header("DAILY PRACTICE")
                + card("Unrelated practice", "Lesson 1"), true, true, new Locators().metadata());
        Assert.assertEquals(1, found.getLength());
        Assert.assertEquals("Lesson 29", found.item(0).getAttributes().getNamedItem("name").getNodeValue());
    }

    @Test public void legacyIntroMetadataIsStillScopedToItsOwnCard() throws Exception {
        NodeList found = select(card("Intro lesson", "How it all works") + card("Practice", "Guide"),
                false, true, new Locators().metadata());
        Assert.assertEquals(1, found.getLength());
        Assert.assertEquals("How it all works", found.item(0).getAttributes().getNamedItem("name").getNodeValue());
    }
}
