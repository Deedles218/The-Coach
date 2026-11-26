package tests;

import lib.Platform;
import lib.ui.android.AndroidKegelPageObject;
import org.junit.Assert;
import org.junit.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Reviewed Kegel locators stay scoped to their native controls. */
public class AndroidKegelCardLocatorUnitTests {
    private static class Screen extends AndroidKegelPageObject {
        Screen() { super(null); }
        String card() { return GENERATED_KEGEL_CARD.substring(6); }
        String exercises() { return START_EXERCISE_TITLES.substring(6); }
        String picker() { return INTENSITY_PICKER.substring(6); }
    }
    private String prefix() { return Platform.getInstance().getAndroidAppPackage()+":id/"; }
    private String section(String text) { return "<android.widget.LinearLayout><android.widget.TextView resource-id='"+prefix()+"tvGroupName' text='"+text+"'/></android.widget.LinearLayout>"; }
    private String card(String title) { return "<android.view.ViewGroup><android.widget.TextView resource-id='"+prefix()+"tvLessonName' text='"+title+"'/></android.view.ViewGroup>"; }
    private int matches(String children) throws Exception {
        Object doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(("<hierarchy>"+children+"</hierarchy>").getBytes(StandardCharsets.UTF_8)));
        return ((org.w3c.dom.NodeList)XPathFactory.newInstance().newXPath().evaluate(
                new Screen().card(),doc,XPathConstants.NODESET)).getLength();
    }
    @Test public void morningAndEveningWorkoutsAreRecognized() throws Exception {
        Assert.assertEquals(2,matches(section("DAILY PRACTICE")+card("Morning Kegel Workout")+card("Evening Kegel Workout")));
    }
    @Test public void legacyCustomWorkoutWithSuffixRemainsRecognized() throws Exception {
        Assert.assertEquals(1,matches(section("DAILY PRACTICE")+card("Custom Kegel (Morning)")));
    }
    @Test public void aLessonWithTheSameNameCannotBecomeAWorkout() throws Exception {
        Assert.assertEquals(0,matches(section("DAILY LESSONS")+card("Morning Kegel Workout")+section("DAILY PRACTICE")+card("Pelvic Pilates")));
    }
    @Test public void anEmptyPracticeSectionCannotConsumeALaterWorkoutTwin() throws Exception {
        Assert.assertEquals(0,matches(section("DAILY PRACTICE")+section("DAILY LESSONS")+card("Morning Kegel Workout")));
    }
    @Test public void aLaterSectionCannotAddAnotherWorkoutMatch() throws Exception {
        Assert.assertEquals(1,matches(section("DAILY PRACTICE")+card("Morning Kegel Workout")
                +section("TO CATCH-UP")+card("Evening Kegel Workout")));
    }
    @Test public void introductoryWorkoutDoesNotSatisfyTheGeneratedFixture() throws Exception {
        Assert.assertEquals(0,matches(section("DAILY PRACTICE")+card("Your first Kegel Workout")));
    }
    @Test public void guidesAndOtherWorkoutsCannotBecomeGeneratedKegels() throws Exception {
        Assert.assertEquals(0,matches(section("DAILY PRACTICE")+card("Morning Kegel Workout Guide")+card("Evening Yoga Workout")));
    }
    @Test public void anotherPackageCannotSatisfyTheWorkoutFixture() throws Exception {
        String xml=(section("DAILY PRACTICE")+card("Morning Kegel Workout")).replace(prefix(),"another.app:id/");
        Assert.assertEquals(0,matches(xml));
    }
    private int exerciseMatches(String container,String pkg,String titleId) throws Exception {
        String xml="<hierarchy><androidx.recyclerview.widget.RecyclerView resource-id='"+pkg+":id/"+container+"'>"
                +"<android.widget.TextView resource-id='"+pkg+":id/"+titleId+"' text='Clasp'/>"
                +"</androidx.recyclerview.widget.RecyclerView></hierarchy>";
        Object doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return ((org.w3c.dom.NodeList)XPathFactory.newInstance().newXPath().evaluate(
                new Screen().exercises(),doc,XPathConstants.NODESET)).getLength();
    }
    @Test public void currentAndLegacyExerciseTitlesAreRecognized() throws Exception {
        String pkg=Platform.getInstance().getAndroidAppPackage();
        Assert.assertEquals(1,exerciseMatches("rvActions",pkg,"tvTitle"));
        Assert.assertEquals(1,exerciseMatches("rvActions",pkg,"tvMainTitleStr"));
    }
    @Test public void stretchingAndOtherAppTitlesCannotBecomeExerciseRows() throws Exception {
        String pkg=Platform.getInstance().getAndroidAppPackage();
        Assert.assertEquals(0,exerciseMatches("svStrecting",pkg,"tvMainTitleStr"));
        Assert.assertEquals(0,exerciseMatches("rvActions","another.app","tvTitle"));
    }
    private int pickerMatches(String xml) throws Exception {
        Object doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return ((org.w3c.dom.NodeList)XPathFactory.newInstance().newXPath().evaluate(
                new Screen().picker(),doc,XPathConstants.NODESET)).getLength();
    }
    @Test public void currentNativeIntensityPickerIsRecognized() throws Exception {
        Assert.assertEquals(1,pickerMatches("<hierarchy><android.widget.NumberPicker package='"+Platform.getInstance().getAndroidAppPackage()+"'>"
                +"<android.widget.EditText resource-id='android:id/numberpicker_input'/></android.widget.NumberPicker></hierarchy>"));
    }
    @Test public void legacyIdentifiedIntensityPickerRemainsRecognized() throws Exception {
        Assert.assertEquals(1,pickerMatches("<hierarchy><android.widget.NumberPicker resource-id='"+prefix()+"npIntensity'/></hierarchy>"));
    }
    @Test public void otherAppOrUnrelatedNumberPickerCannotBecomeIntensityPicker() throws Exception {
        Assert.assertEquals(0,pickerMatches("<hierarchy><android.widget.NumberPicker package='another.app'>"
                +"<android.widget.EditText resource-id='android:id/numberpicker_input'/></android.widget.NumberPicker></hierarchy>"));
        Assert.assertEquals(0,pickerMatches("<hierarchy><android.widget.NumberPicker package='"+Platform.getInstance().getAndroidAppPackage()+"'/></hierarchy>"));
    }
}
