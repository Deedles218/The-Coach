package tests;

import lib.ui.ios.iOSDailyPlanPageObject;
import org.junit.Assert;
import org.junit.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Player text must be scoped to the native player, including advanced workouts. */
public class KegelPlayerLocatorUnitTests {
    private int count(String xml, String locator) throws Exception {
        Object document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return ((NodeList) XPathFactory.newInstance().newXPath().evaluate(
                locator.substring("xpath:".length()), document, XPathConstants.NODESET)).getLength();
    }
    private String player(String content) {
        return "<XCUIElementTypeOther><XCUIElementTypeButton name='navBarRoundClose'/>"
                + "<XCUIElementTypeOther><XCUIElementTypeButton name='PlayerPauseIcon'/></XCUIElementTypeOther>"
                + content + "</XCUIElementTypeOther>";
    }
    @Test public void basicAndAdvancedActiveTitlesAreRecognized() throws Exception {
        for (String title : new String[]{"Contract", "Contract 75%", "Contract slowly", "Rest", "Waves", "Squeeze"}) {
            Assert.assertEquals(title, 1, count(player("<XCUIElementTypeStaticText name='" + title + "' visible='true'/>"),
                    iOSDailyPlanPageObject.kegelPlayerExerciseTitleLocator()));
        }
    }
    @Test public void lessonTextCannotSatisfyPlayerReadiness() throws Exception {
        Assert.assertEquals(0, count("<XCUIElementTypeOther><XCUIElementTypeStaticText name='Contract slowly' visible='true'/></XCUIElementTypeOther>",
                iOSDailyPlanPageObject.kegelPlayerExerciseTitleLocator()));
    }
    @Test public void hiddenTitleCannotSatisfyPlayerReadiness() throws Exception {
        Assert.assertEquals(0, count(player("<XCUIElementTypeStaticText name='Contract 75%' visible='false'/>"),
                iOSDailyPlanPageObject.kegelPlayerExerciseTitleLocator()));
    }
    @Test public void advancedWorkoutPhaseIsScopedToPlayerCollection() throws Exception {
        String phase = "<XCUIElementTypeStaticText name='SLOW LADDER' visible='true'/>";
        Assert.assertEquals(1, count(player(phase + "<XCUIElementTypeCollectionView><XCUIElementTypeCell>" + phase
                + "</XCUIElementTypeCell></XCUIElementTypeCollectionView>"), iOSDailyPlanPageObject.kegelPlayerWorkoutPhaseLocator()));
    }
    @Test public void advancedInfoUsesInstructionBulletsInTheKegelsModal() throws Exception {
        String xml = "<XCUIElementTypeCollectionView><XCUIElementTypeStaticText name='Slow Ladder Kegels'/>"
                + "<XCUIElementTypeOther name='LessonBulletTextView'><XCUIElementTypeStaticText visible='true' "
                + "name='Begin by contracting at 25% of your effort, slowly feeling a light engagement over 3 seconds'/>"
                + "</XCUIElementTypeOther></XCUIElementTypeCollectionView>";
        Assert.assertEquals(1, count(xml, iOSDailyPlanPageObject.kegelPlayerInfoContentLocator()));
    }
    @Test public void titleAloneAndUnrelatedLessonBulletsCannotSatisfyInfoContent() throws Exception {
        String xml = "<root><XCUIElementTypeCollectionView><XCUIElementTypeStaticText name='Slow Ladder Kegels'/>"
                + "</XCUIElementTypeCollectionView><XCUIElementTypeCollectionView>"
                + "<XCUIElementTypeOther name='LessonBulletTextView'><XCUIElementTypeStaticText visible='true' "
                + "name='This instruction belongs to another lesson and must not match the modal'/>"
                + "</XCUIElementTypeOther></XCUIElementTypeCollectionView></root>";
        Assert.assertEquals(0, count(xml, iOSDailyPlanPageObject.kegelPlayerInfoContentLocator()));
    }
}
