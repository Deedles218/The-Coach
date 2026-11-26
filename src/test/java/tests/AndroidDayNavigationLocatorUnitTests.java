package tests;

import lib.Platform;
import lib.ui.android.AndroidDailyPlanPageObject;
import org.junit.Assert;
import org.junit.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Day and Stage headers expose different arrow IDs in the current Android builds. */
public class AndroidDayNavigationLocatorUnitTests {
    private static class Screen extends AndroidDailyPlanPageObject {
        Screen() { super(null); }
        String arrow(boolean left) { return (left ? LEFT_SWITCHER_ARROW : RIGHT_SWITCHER_ARROW).substring(6); }
        String lesson() { return FIRST_LESSON_CARD_IN_SECTION.substring(6); }
        String lockedDay() { return LOCKED_DAY_BALLOON.substring(6); }
        String nextDay(String day) { return nextDayStartConfirmation(day).substring(6); }
    }
    private int matches(boolean left, String pkg, String id) throws Exception {
        String xml="<hierarchy><android.widget.ImageView resource-id='"+pkg+":id/"+id+"'/></hierarchy>";
        Object doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return ((org.w3c.dom.NodeList)XPathFactory.newInstance().newXPath().evaluate(
                new Screen().arrow(left),doc,XPathConstants.NODESET)).getLength();
    }
    private String pkg() { return Platform.getInstance().getAndroidAppPackage(); }
    @Test public void currentDayArrowsAreRecognized() throws Exception {
        Assert.assertEquals(1,matches(true,pkg(),"ivArrowLeft"));
        Assert.assertEquals(1,matches(false,pkg(),"iv_arrow_right"));
    }
    @Test public void stageArrowsRemainRecognized() throws Exception {
        Assert.assertEquals(1,matches(true,pkg(),"ivBack"));
        Assert.assertEquals(1,matches(false,pkg(),"ivNext"));
    }
    @Test public void directionsCannotBeSwapped() throws Exception {
        Assert.assertEquals(0,matches(true,pkg(),"iv_arrow_right"));
        Assert.assertEquals(0,matches(false,pkg(),"ivArrowLeft"));
    }
    @Test public void otherAppsAndUnrelatedBackButtonsAreRejected() throws Exception {
        Assert.assertEquals(0,matches(true,"another.app","ivArrowLeft"));
        Assert.assertEquals(0,matches(false,"another.app","ivNext"));
        Assert.assertEquals(0,matches(true,pkg(),"btnNavigateUp"));
    }
    private String section(String name) {
        return "<android.widget.LinearLayout><android.widget.TextView resource-id='"+pkg()+":id/tvGroupName' text='"+name+"'/></android.widget.LinearLayout>";
    }
    private String card(String tag) {
        return "<android.view.ViewGroup><androidx.cardview.widget.CardView resource-id='"+pkg()+":id/cvLessonContainer'><android.widget.TextView resource-id='"+pkg()+":id/tvTagText' text='"+tag+"'/></androidx.cardview.widget.CardView></android.view.ViewGroup>";
    }
    private int lessonMatches(String children) throws Exception {
        Object doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(("<hierarchy>"+children+"</hierarchy>").getBytes(StandardCharsets.UTF_8)));
        return ((org.w3c.dom.NodeList)XPathFactory.newInstance().newXPath().evaluate(
                new Screen().lesson(),doc,XPathConstants.NODESET)).getLength();
    }
    @Test public void dayLessonsDoNotRequireLegacyNumberedTags() throws Exception {
        Assert.assertEquals(1,lessonMatches(section("DAILY LESSONS")+card("How it all works")));
    }
    @Test public void stageLessonsRemainRecognized() throws Exception {
        Assert.assertEquals(1,lessonMatches(section("DAILY LESSONS")+card("Lesson 1")));
    }
    @Test public void practiceCannotSatisfyMissingLesson() throws Exception {
        Assert.assertEquals(0,lessonMatches(section("DAILY PRACTICE")+card("Lesson 1")));
    }
    @Test public void emptyLessonSectionCannotConsumeTheNextSection() throws Exception {
        Assert.assertEquals(0,lessonMatches(section("DAILY LESSONS")+section("DAILY PRACTICE")+card("Practice")));
    }
    private int lockedDayMatches(String id, String text) throws Exception {
        Object doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(("<hierarchy><android.widget.TextView resource-id='"+pkg()+":id/"+id+"' text='"+text+"'/></hierarchy>").getBytes(StandardCharsets.UTF_8)));
        return ((org.w3c.dom.NodeList)XPathFactory.newInstance().newXPath().evaluate(
                new Screen().lockedDay(),doc,XPathConstants.NODESET)).getLength();
    }
    @Test public void observedDayLockTooltipIsRecognized() throws Exception {
        Assert.assertEquals(1,lockedDayMatches("balloon_text","Complete current activities to unlock the next day"));
    }
    @Test public void anotherTooltipOrLessonTextCannotProveDayLock() throws Exception {
        Assert.assertEquals(0,lockedDayMatches("balloon_text","Tap to explore other programs"));
        Assert.assertEquals(0,lockedDayMatches("tvTextContent","Complete current activities to unlock the next day"));
    }
    private int nextDayMatches(String pkg,String id,String label) throws Exception {
        Object doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(("<hierarchy><android.widget.Button resource-id='"+pkg+":id/"+id+"' text='"+label+"'/></hierarchy>").getBytes(StandardCharsets.UTF_8)));
        return ((org.w3c.dom.NodeList)XPathFactory.newInstance().newXPath().evaluate(
                new Screen().nextDay("Day 2 of 60"),doc,XPathConstants.NODESET)).getLength();
    }
    @Test public void onlyTheRequestedDayCanBeStarted() throws Exception {
        Assert.assertEquals(1,nextDayMatches(pkg(),"btnYes","START DAY 2 RIGHT NOW"));
        Assert.assertEquals(0,nextDayMatches(pkg(),"btnYes","START DAY 3 RIGHT NOW"));
        Assert.assertEquals(0,nextDayMatches(pkg(),"btnYes","RESTART"));
    }
    @Test public void nextDayConfirmationRejectsOtherAppsAndButtons() throws Exception {
        Assert.assertEquals(0,nextDayMatches("another.app","btnYes","START DAY 2 RIGHT NOW"));
        Assert.assertEquals(0,nextDayMatches(pkg(),"btnNo","START DAY 2 RIGHT NOW"));
    }
}
