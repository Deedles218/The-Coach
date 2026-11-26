package tests;

import lib.Platform;
import lib.ui.android.AndroidKegelPageObject;
import org.junit.*;
import org.openqa.selenium.WebElement;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class AndroidKegelContractUnitTests {
    private String previousTitle,previousCount;
    private static class Screen extends AndroidKegelPageObject {
        String instruction;
        Screen() {super(null);}
        String workout(String title) {return generatedWorkoutCard(title).substring(6);}
        @Override public WebElement waitForElementAndClick(String locator,String message,long timeout) {instruction=locator;return null;}
        @Override public void assertKegelExerciseInstructionIsDisplayed() { }
    }
    @Before public void save() {
        previousTitle=System.getProperty("coach.kegel.instructionTitle");previousCount=System.getProperty("coach.kegel.instructionStepCount");
        System.clearProperty("coach.kegel.instructionTitle");System.clearProperty("coach.kegel.instructionStepCount");
    }
    @After public void restore() {
        restore("coach.kegel.instructionTitle",previousTitle);restore("coach.kegel.instructionStepCount",previousCount);
    }
    private void restore(String key,String value) {if(value==null)System.clearProperty(key);else System.setProperty(key,value);}
    private int matches(String query,String body) throws Exception {
        Object xml=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(
                ("<hierarchy>"+body+"</hierarchy>").getBytes(StandardCharsets.UTF_8)));
        return ((org.w3c.dom.NodeList)XPathFactory.newInstance().newXPath().evaluate(query,xml,XPathConstants.NODESET)).getLength();
    }
    private String prefix() {return Platform.getInstance().getAndroidAppPackage()+":id/";}
    private String section(String label) {return "<android.widget.LinearLayout><android.widget.TextView resource-id='"+prefix()+"tvGroupName' text='"+label+"'/></android.widget.LinearLayout>";}
    private String card(String name) {return "<android.view.ViewGroup><android.widget.TextView resource-id='"+prefix()+"tvLessonName' text='"+name+"'/></android.view.ViewGroup>";}
    @Test public void exactWorkoutKeepsTheDailyPracticeBoundary() throws Exception {
        String query=new Screen().workout("Evening Kegel Workout");
        Assert.assertEquals(1,matches(query,section("DAILY PRACTICE")+card("Morning Kegel Workout")+card("Evening Kegel Workout")));
        Assert.assertEquals(0,matches(query,section("DAILY PRACTICE")+card("Morning Kegel Workout")+section("TO CATCH-UP")+card("Evening Kegel Workout")));
    }
    @Test public void unknownExactWorkoutCannotBroadenTheGeneratedCardLocator() throws Exception {
        Assert.assertEquals(0,matches(new Screen().workout("Evening Yoga Workout"),section("DAILY PRACTICE")+card("Evening Yoga Workout")));
    }
    @Test(expected=AssertionError.class) public void blankWorkoutIsRejected() {new Screen().workout(" ");}
    @Test public void configuredInstructionSelectsOnlyTheExactExercise() throws Exception {
        System.setProperty("coach.kegel.instructionTitle","Waves");Screen screen=new Screen();screen.openFirstKegelExerciseInstruction();
        String body="<androidx.recyclerview.widget.RecyclerView resource-id='"+prefix()+"rvActions'>"
                +"<android.widget.TextView resource-id='"+prefix()+"tvTitle' text='Clasp'/>"
                +"<android.widget.TextView resource-id='"+prefix()+"tvTitle' text='Waves'/>"
                +"</androidx.recyclerview.widget.RecyclerView>";
        Assert.assertEquals(1,matches(screen.instruction.substring(6),body));
        Assert.assertEquals(0,matches(screen.instruction.substring(6),body.replace("Waves","Waves Guide")));
    }
    @Test(expected=AssertionError.class) public void blankInstructionIsRejected() {
        System.setProperty("coach.kegel.instructionTitle","");new Screen().openFirstKegelExerciseInstruction();
    }
    @Test(expected=AssertionError.class) public void malformedStepCountCannotFallBackToThree() {
        System.setProperty("coach.kegel.instructionStepCount","three");new AndroidKegelPageObject(null).assertKegelExerciseInstructionIsDisplayed();
    }
    @Test(expected=AssertionError.class) public void outOfRangeStepCountIsRejectedBeforeReadingUi() {
        System.setProperty("coach.kegel.instructionStepCount","0");new AndroidKegelPageObject(null).assertKegelExerciseInstructionIsDisplayed();
    }
}
