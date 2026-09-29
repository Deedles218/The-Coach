package lib.ui.android;

import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.*;
import java.util.*;

/** Native Kegel contracts from the installed APK layouts; no generic back-button aliases. */
public class AndroidKegelPageObject extends AndroidDailyPlanPageObject {
    private static final String P="com.vamapps.thecoach:id/";
    private String originalIntensity;
    public AndroidKegelPageObject(RemoteWebDriver driver) { super(driver); }
    private String id(String value) { return "id:"+P+value; }
    private WebElement visible(String value) { return waitForElementVisible(id(value),"Kegel control absent: "+value,10); }
    private void tap(String value) { waitForElementAndClick(id(value),"Kegel control unavailable: "+value,10); }
    private boolean shown(String value) { return isElementVisible(id(value)); }
    private void scroll(String container,String direction) {
        WebElement view=visible(container);Map<String,Object> args=new HashMap<String,Object>();
        args.put("elementId",((RemoteWebElement)view).getId());args.put("direction",direction);args.put("percent",.8);
        ((JavascriptExecutor)driver).executeScript("mobile: scrollGesture",args);
    }
    @Override public void openKegelExerciseFromDailyPlan() {
        String card="xpath://android.widget.TextView[@resource-id='"+P+"tvLessonName' and contains(@text,'Custom Kegel')]";
        for(int n=0;n<10;n++) {
            if(isElementVisible(card)) { waitForElementAndClick(card,"Cannot open Custom Kegel",10);assertKegelStartScreenIsDisplayed();return; }
            scroll("rvQuestions","down");
        }
        Assert.fail("Dedicated Kegel fixture has no Custom Kegel card on this stage");
    }
    @Override public void assertKegelStartScreenIsDisplayed() {
        Assert.assertTrue("Wrong Kegel start screen",visible("tvTitle").getText().toUpperCase(Locale.ROOT).contains("KEGEL"));
        visible("tvGoalBody");visible("tvWorkutDuration");visible("tvWorkutIntensity");
        waitForElementEnabled(id("btStartWork"),"Kegel start unavailable",10);
    }
    @Override public void assertKegelStartScreenExerciseInformation() {
        assertKegelStartScreenIsDisplayed();visible("tvExercisesTitle");visible("rvActions");
        List<WebElement> exercises=driver.findElements(By.xpath("//*[@resource-id='"+P+"rvActions']//*[@resource-id='"+P+"tvMainTitleStr']"));
        Assert.assertTrue("Kegel fixture requires multiple exercise descriptions",exercises.size()>=2);
        for(WebElement exercise:exercises) Assert.assertFalse("Empty exercise title",exercise.getText().trim().isEmpty());
        Assert.assertFalse("Exercise duration/intensity metadata absent",driver.findElements(By.id(P+"tvValue")).isEmpty());
    }
    @Override public void openFirstKegelExerciseInstruction() {
        waitForElementAndClick("xpath://*[@resource-id='"+P+"rvActions']//*[@resource-id='"+P+"tvMainTitleStr']", "Cannot open exercise instruction",10);
        assertKegelExerciseInstructionIsDisplayed();
    }
    @Override public void assertKegelExerciseInstructionIsDisplayed() {
        visible("tvName");visible("ivInstructionImage");visible("rvInstr");visible("tvAdjust");
        List<WebElement> steps=driver.findElements(By.xpath("//*[@resource-id='"+P+"rvInstr']//*[@resource-id='"+P+"tvText']"));
        Assert.assertEquals("Instruction needs three steps",3,steps.size());
        for(WebElement step:steps)Assert.assertFalse("Empty instruction step",step.getText().trim().isEmpty());
        visible("tvSelectedIntens");
    }
    @Override public void selectNextKegelExerciseInstructionIntensity() {
        originalIntensity=visible("tvSelectedIntens").getText();tap("mcIntensContainer");
        WebElement picker=visible("npIntensity");
        List<WebElement> values=picker.findElements(By.className("android.widget.TextView"));
        Assert.assertFalse("Intensity picker has no adjacent option",values.isEmpty());
        values.get(values.size()-1).click();tap("btnSave");
        createWait(10).until(d->!originalIntensity.equals(visible("tvSelectedIntens").getText()));
    }
    @Override public void restorePreviousKegelExerciseInstructionIntensity() {
        Assert.assertNotNull("No original intensity captured",originalIntensity);
        tap("mcIntensContainer");WebElement picker=visible("npIntensity");
        List<WebElement> values=picker.findElements(By.className("android.widget.TextView"));
        WebElement target=null;for(WebElement value:values)if(originalIntensity.equals(value.getText()))target=value;
        Assert.assertNotNull("Original intensity is not an adjacent picker value",target);target.click();tap("btnSave");
        Assert.assertEquals("Intensity did not restore",originalIntensity,visible("tvSelectedIntens").getText());
    }
    @Override public void returnFromKegelExerciseInstruction() { tap("btnNavigateUp");assertKegelStartScreenIsDisplayed(); }
    @Override public void startKegelExercise() {
        tap("btStartWork");
        waitForFirstElementPresent(new String[]{id("ivPause"),id("btnPause"),id("exo_player")},"Kegel player did not open",20);
        // A stretching video must be exited explicitly; do not mark any activity complete.
        if(shown("exo_player")) {
            tap("btnNavigateUp");
            waitForElementAndClick("xpath://*[@text=\"LET'S GO\"]", "Stretching continuation absent",10);
        }
        assertKegelPlayerIsDisplayed();
    }
    @Override public void assertKegelPlayerIsDisplayed() {
        visible("tvTimer");visible("tvActionName");visible("rvExercise");
        waitForFirstElementPresent(new String[]{id("ivPause"),id("ivPlay")},"Player has no pause/play control",10);
        waitForFirstElementPresent(new String[]{id("ivAudioOn"),id("ivAudioOff")},"Sound state is absent",10);
        waitForFirstElementPresent(new String[]{id("ivVibrationOn"),id("ivvibrationOff")},"Vibration state is absent",10);
    }
    @Override public void pauseKegelExercise() { tap("ivPause");visible("ivPlay"); }
    @Override public void resumeKegelExercise() { tap("ivPlay");visible("ivPause"); }
    @Override public void setKegelPlayerSoundEnabled(boolean enabled) {
        if(!shown(enabled?"ivAudioOn":"ivAudioOff"))tap(enabled?"ivAudioOff":"ivAudioOn");
        assertKegelPlayerSoundEnabled(enabled);
    }
    @Override public void assertKegelPlayerSoundEnabled(boolean enabled) {visible(enabled?"ivAudioOn":"ivAudioOff");}
    @Override public void setKegelPlayerVibrationEnabled(boolean enabled) {
        if(!shown(enabled?"ivVibrationOn":"ivvibrationOff"))tap(enabled?"ivvibrationOff":"ivVibrationOn");
        assertKegelPlayerVibrationEnabled(enabled);
    }
    @Override public void assertKegelPlayerVibrationEnabled(boolean enabled) {visible(enabled?"ivVibrationOn":"ivvibrationOff");}
    @Override public void toggleKegelPlayerSound() {setKegelPlayerSoundEnabled(!shown("ivAudioOn"));}
    @Override public void toggleKegelPlayerVibration() {setKegelPlayerVibrationEnabled(!shown("ivVibrationOn"));}
    @Override public boolean isKegelPlayerSoundControlDisplayed() {return shown("ivAudioOn")||shown("ivAudioOff");}
    @Override public void closeKegelPlayerInstructionsIfPresent() {
        if(shown("rvContent")&&shown("btnClose")) {tap("btnClose");waitForElementNotVisible(id("rvContent"),"Info did not close",10);}
    }
    @Override public void openKegelPlayerInfo() {
        tap("ivExerciseInfo");visible("rvContent");visible("tvTitle");closeKegelPlayerInstructionsIfPresent();visible("ivPlay");
    }
    @Override public void pressKegelPlayerPhaseControls() {
        for(String phase:new String[]{"Squeeze","Rest","Waves"}) {
            waitForElementAndClick("xpath://*[@resource-id='"+P+"rvExercise']//*[translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='"+phase.toLowerCase(Locale.ROOT)+"']", "Kegel phase absent: "+phase,10);
            Assert.assertEquals("Wrong phase selected",phase.toLowerCase(Locale.ROOT),visible("tvActionName").getText().toLowerCase(Locale.ROOT));
        }
    }
    @Override public void pressAllKegelPlayerControls() {
        pauseKegelExercise();resumeKegelExercise();toggleKegelPlayerSound();toggleKegelPlayerVibration();
        openKegelPlayerInfo();resumeKegelExercise();pressKegelPlayerPhaseControls();
    }
    @Override public void exitKegelExercisePlayer() {
        tap("btnNavigateUp");visible("tvDialogtitle");tap("btnYes");
        waitForElementVisible(id("rvQuestions"),"Today did not return after quitting workout",15);
    }
    @Override public boolean isKegelCompletionIntensityFeedbackDisplayed() {
        return isElementVisible("xpath://*[@text='Too Hard' or @text='Too hard']");
    }
    @Override public void selectTooHardKegelCompletionIntensityFeedback() {
        for(String label:new String[]{"Too Easy","Great","Too Hard"})waitForElementVisible("xpath://*[translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='"+label.toLowerCase(Locale.ROOT)+"']","Intensity feedback missing: "+label,10);
        waitForElementAndClick("xpath://*[translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='too hard']","Cannot select Too Hard",10);
    }
    @Override public void closePracticeCompletionFeedbackIfPresent() {
        if(shown("rvFeedBackDetails")) {tap("btnClose");waitForElementNotVisible(id("rvFeedBackDetails"),"Feedback did not close",10);}
    }
    @Override public void closeKegelExerciseFlowIfPresent() {
        closeKegelPlayerInstructionsIfPresent();
        if(shown("tvTimer"))exitKegelExercisePlayer();
        if(shown("tvSelectedIntens"))returnFromKegelExerciseInstruction();
        if(shown("btStartWork"))returnFromDailyPracticeToDailyPlan();
    }
}
