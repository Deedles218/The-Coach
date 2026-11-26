package tests;

import io.appium.java_client.HasOnScreenKeyboard;
import io.appium.java_client.HidesKeyboard;
import lib.ui.android.AndroidKegelPageObject;
import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.*;
import java.util.*;

/** Form validation and submission are exercised entirely through in-memory controls. */
public class AndroidKegelQuestionnaireUnitTests {
    private static class Field extends RemoteWebElement {
        String text;boolean retain=true,enabled=true;int clicks;
        Field(String text) {this.text=text;}
        @Override public String getText() {return retain?text:"";}
        @Override public void clear() {text="";}
        @Override public void sendKeys(CharSequence... keys) {text=String.join("",keys);}
        @Override public boolean isEnabled() {return enabled;}
        @Override public void click() {clicks++;}
    }
    private static class Driver extends RemoteWebDriver implements HasOnScreenKeyboard,HidesKeyboard {
        final List<WebElement> fields=new ArrayList<>(),headings=new ArrayList<>(),prompts=new ArrayList<>();
        int keyboardDismissals;
        Driver() {
            for(int n=1;n<=3;n++) {fields.add(new Field(""));headings.add(new Field("Question "+n));prompts.add(new Field("Reviewed question "+n));}
        }
        @Override public List<WebElement> findElements(By by) {
            String query=by.toString();
            Assert.assertTrue("Questionnaire lookup escaped its container",query.contains("rvFeedBackDetails"));
            if(query.contains("etAnswerField"))return fields;
            if(query.contains("tvHeader"))return headings;
            if(query.contains("tvTextContent"))return prompts;
            throw new AssertionError("Unexpected questionnaire lookup");
        }
        @Override public boolean isKeyboardShown() {return true;}
        @Override public void hideKeyboard() {keyboardDismissals++;}
        @Override public io.appium.java_client.ExecutesMethod assertExtensionExists(String name) {
            throw new AssertionError("An offline form must not invoke Appium extensions");
        }
        @Override public io.appium.java_client.ExecutesMethod markExtensionAbsence(String name) {
            throw new AssertionError("An offline form must not invoke Appium extensions");
        }
        @Override public Response execute(String name,Map<String,?> parameters) {
            throw new AssertionError("An offline form must not execute driver commands");
        }
        @Override public Response execute(String name) {
            throw new AssertionError("An offline form must not execute driver commands");
        }
    }
    private static class Screen extends AndroidKegelPageObject {
        final Driver form;
        final Field finish=new Field("FINISH");
        boolean dismissed,returned;
        Screen(Driver form) {super(form);this.form=form;}
        @Override public WebElement waitForElementVisible(String locator,String message,long timeout) {
            if(locator.endsWith("/rvFeedBackDetails"))return new Field("");
            if(locator.endsWith("/rvQuestions")) {Assert.assertTrue(dismissed);returned=true;return new Field("");}
            if(locator.endsWith("/bContinueLesson"))return finish;
            throw new AssertionError("Unexpected questionnaire visibility wait");
        }
        @Override public WebElement waitForElementEnabled(String locator,String message,long timeout) {
            Assert.assertTrue(locator.endsWith("/bContinueLesson"));Assert.assertTrue(message,finish.isEnabled());return finish;
        }
        @Override public void waitForElementNotVisible(String locator,String message,long timeout) {
            Assert.assertTrue(locator.endsWith("/rvFeedBackDetails"));Assert.assertEquals(1,finish.clicks);dismissed=true;
        }
    }
    private void rejected(Screen screen) {
        AssertionError failure=null;
        try {screen.submitKegelCompletionQuestionnaire();}
        catch(AssertionError expected) {failure=expected;}
        Assert.assertNotNull("Invalid feedback was accepted",failure);
        Assert.assertEquals(0,screen.finish.clicks);Assert.assertFalse(screen.returned);
    }
    @Test public void threeAnswersAreRetainedBeforeSubmissionAndReturnToToday() {
        Driver form=new Driver();Screen screen=new Screen(form);screen.submitKegelCompletionQuestionnaire();
        Assert.assertEquals(3,form.keyboardDismissals);Assert.assertEquals(1,screen.finish.clicks);
        for(WebElement field:form.fields)Assert.assertFalse(field.getText().trim().isEmpty());
        Assert.assertTrue(screen.dismissed);Assert.assertTrue(screen.returned);
    }
    @Test public void missingAnswerFieldBlocksSubmission() {
        Driver form=new Driver();form.fields.remove(2);rejected(new Screen(form));
        Assert.assertEquals(0,form.keyboardDismissals);
    }
    @Test public void wrongQuestionOrderBlocksSubmission() {
        Driver form=new Driver();((Field)form.headings.get(0)).text="Question 2";rejected(new Screen(form));
        Assert.assertEquals(0,form.keyboardDismissals);
    }
    @Test public void emptyPromptBlocksSubmission() {
        Driver form=new Driver();((Field)form.prompts.get(1)).text=" ";rejected(new Screen(form));
    }
    @Test public void lostAnswerBlocksSubmission() {
        Driver form=new Driver();((Field)form.fields.get(1)).retain=false;rejected(new Screen(form));
    }
    @Test public void disabledFinishBlocksSubmission() {
        Screen screen=new Screen(new Driver());screen.finish.enabled=false;rejected(screen);
    }
    @Test public void unexpectedSubmitLabelBlocksSubmission() {
        Screen screen=new Screen(new Driver());screen.finish.text="CONTINUE";rejected(screen);
    }
}
