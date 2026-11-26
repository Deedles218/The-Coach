package lib.ui.android;

import io.qameta.allure.Step;
import lib.Platform;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.*;
import java.util.*;

/** Native Kegel contracts from the installed APK layouts; no generic back-button aliases. */
public class AndroidKegelPageObject extends AndroidDailyPlanPageObject {
    private static final String P=Platform.getInstance().getAndroidAppPackage()+":id/";
    protected static final String GENERATED_KEGEL_CARD =
            "xpath://android.widget.TextView[@resource-id='"+P+"tvGroupName' and @text='DAILY PRACTICE']"
            + "/parent::android.widget.LinearLayout/following-sibling::android.view.ViewGroup"
            + "[preceding-sibling::android.widget.LinearLayout[android.widget.TextView"
            + "[@resource-id='"+P+"tvGroupName']][1]/android.widget.TextView"
            + "[@resource-id='"+P+"tvGroupName' and @text='DAILY PRACTICE']]"
            + "//android.widget.TextView[@resource-id='"+P+"tvLessonName' and ("
            + "contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'custom kegel')"
            + " or @text='Morning Kegel Workout' or @text='Evening Kegel Workout')]";
    protected static final String START_EXERCISE_TITLES =
            "xpath://*[@resource-id='"+P+"rvActions']//*[@resource-id='"+P+"tvTitle'"
            +" or @resource-id='"+P+"tvMainTitleStr']";
    protected static final String INTENSITY_PICKER =
            "xpath://*[@resource-id='"+P+"npIntensity' or (self::android.widget.NumberPicker"
            +" and @package='"+Platform.getInstance().getAndroidAppPackage()+"'"
            +" and .//*[@resource-id='android:id/numberpicker_input'])]";
    private String originalIntensity;
    private final Set<String> workoutPhases=new LinkedHashSet<String>();
    public AndroidKegelPageObject(RemoteWebDriver driver) {
        super(driver);
        if(driver instanceof io.appium.java_client.android.AndroidDriver)configureAnimatedKegelUi();
    }
    private String id(String value) { return "id:"+P+value; }
    private WebElement visible(String value) { return waitForElementVisible(id(value),"Kegel control absent: "+value,10); }
    private void tap(String value) { waitForElementAndClick(id(value),"Kegel control unavailable: "+value,10); }
    private boolean shown(String value) { return isElementVisible(id(value)); }
    private void configureAnimatedKegelUi() {
        // The instruction GIF and workout animation do not become idle.
        // Match the existing KegelCasePageObject setting; retain explicit UI waits.
        ((io.appium.java_client.android.AndroidDriver)driver).setSetting("waitForIdleTimeout",1000);
    }
    private void scroll(String container,String direction) {
        WebElement view=visible(container);Map<String,Object> args=new HashMap<String,Object>();
        args.put("elementId",((RemoteWebElement)view).getId());args.put("direction",direction);args.put("percent",.8);
        ((JavascriptExecutor)driver).executeScript("mobile: scrollGesture",args);
    }
    @Override public void openKegelExerciseFromDailyPlan() {
        String card=GENERATED_KEGEL_CARD;
        for(int n=0;n<10;n++) {
            if(isElementVisible(card)) { waitForElementAndClick(card,"Cannot open Custom Kegel",10);assertKegelStartScreenIsDisplayed();return; }
            scroll("rvQuestions","down");
        }
        Assert.fail("Dedicated Kegel fixture has no generated Kegel workout on this day");
    }
    @Override public void assertKegelStartScreenIsDisplayed() {
        Assert.assertTrue("Wrong Kegel start screen",visible("tvTitle").getText().toUpperCase(Locale.ROOT).contains("KEGEL"));
        visible("tvGoalBody");visible("tvWorkutDuration");visible("tvWorkutIntensity");
        waitForElementEnabled(id("btStartWork"),"Kegel start unavailable",10);
        workoutPhases.clear();
        for(WebElement title:driver.findElements(By.xpath(START_EXERCISE_TITLES.substring(6)))) {
            String phase=title.getText().trim().toUpperCase(Locale.ROOT);
            if(!phase.isEmpty())workoutPhases.add(phase);
        }
    }
    @Override public void assertKegelStartScreenExerciseInformation() {
        assertKegelStartScreenIsDisplayed();visible("tvExercisesTitle");visible("rvActions");
        List<WebElement> exercises=driver.findElements(By.xpath(START_EXERCISE_TITLES.substring(6)));
        Assert.assertTrue("Kegel fixture requires multiple exercise descriptions",exercises.size()>=2);
        for(WebElement exercise:exercises) Assert.assertFalse("Empty exercise title",exercise.getText().trim().isEmpty());
        Assert.assertFalse("Exercise duration/intensity metadata absent",driver.findElements(By.id(P+"tvValue")).isEmpty());
    }
    @Override public void openFirstKegelExerciseInstruction() {
        waitForElementAndClick(START_EXERCISE_TITLES, "Cannot open exercise instruction",10);
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
        WebElement picker=waitForElementVisible(INTENSITY_PICKER,"Intensity picker is absent",10);
        List<WebElement> values=intensityChoices(picker);
        Assert.assertFalse("Intensity picker has no adjacent option",values.isEmpty());
        WebElement next=values.get(values.size()-1);String requested=next.getText().trim();next.click();
        createWait(5).withMessage("Intensity picker did not select "+requested).until(d -> {
            List<WebElement> input=picker.findElements(By.id("android:id/numberpicker_input"));
            return input.isEmpty()||requested.equalsIgnoreCase(input.get(0).getText().trim());
        });
        tap("btnSave");
        createWait(10).withMessage("Instruction intensity did not change").until(d->
                !originalIntensity.equalsIgnoreCase(visible("tvSelectedIntens").getText().trim()));
        io.qameta.allure.Allure.addAttachment("Kegel instruction intensity change",
                "before="+originalIntensity+"; after="+visible("tvSelectedIntens").getText());
    }
    @Override public void restorePreviousKegelExerciseInstructionIntensity() {
        Assert.assertNotNull("No original intensity captured",originalIntensity);
        tap("mcIntensContainer");WebElement picker=waitForElementVisible(INTENSITY_PICKER,"Intensity picker is absent",10);
        List<WebElement> values=intensityChoices(picker);
        List<WebElement> input=picker.findElements(By.id("android:id/numberpicker_input"));
        boolean alreadySelected=!input.isEmpty()&&originalIntensity.equalsIgnoreCase(input.get(0).getText().trim());
        if(!alreadySelected) {
            WebElement target=null;for(WebElement value:values)if(originalIntensity.equalsIgnoreCase(value.getText()))target=value;
            Assert.assertNotNull("Original intensity is absent from the picker",target);target.click();
        }
        tap("btnSave");
        Assert.assertTrue("Intensity did not restore",originalIntensity.equalsIgnoreCase(visible("tvSelectedIntens").getText().trim()));
    }
    private List<WebElement> intensityChoices(WebElement picker) {
        return picker.findElements(By.xpath(".//android.widget.Button | .//android.widget.TextView"));
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
        tap("ivExerciseInfo");visible("rvContent");
        WebElement title=waitForElementVisible("xpath://*[@resource-id='"+P+"rvContent']"
                +"//*[@resource-id='"+P+"tvHeader' or @resource-id='"+P+"tvTitle']",
                "Kegel information title is absent",10);
        Assert.assertFalse("Kegel information title is empty",title.getText().trim().isEmpty());
        closeKegelPlayerInstructionsIfPresent();visible("ivPlay");
    }
    @Override public void pressKegelPlayerPhaseControls() {
        Assert.assertFalse("The workout has no reviewed exercise phase names",workoutPhases.isEmpty());
        Set<String> phases=new LinkedHashSet<String>(workoutPhases);phases.add("REST");
        for(String phase:phases) {
            String label=org.openqa.selenium.support.ui.Quotes.escape(phase.toLowerCase(Locale.ROOT));
            String row="xpath://*[@resource-id='"+P+"rvExercise']"
                    +"//*[@resource-id='"+P+"llContainer' and .//*[translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')="+label+"]]";
            if(shown("ivPause"))pauseKegelExercise();
            for(int n=0;n<12&&!isElementVisible(row);n++)scroll("rvExercise","right");
            for(int n=0;n<12&&!isElementVisible(row);n++)scroll("rvExercise","left");
            waitForElementAndClick(row,"Kegel phase absent: "+phase,10);
            createWait(10).withMessage("The tapped Kegel phase did not become active: "+phase)
                    .until(d -> isElementVisible(activeKegelPhase(phase)));
            Assert.assertFalse("The active phase has no instruction",visible("tvActionName").getText().trim().isEmpty());
        }
    }
    protected String activeKegelPhase(String phase) {
        return "xpath://*[@resource-id='"+P+"rvExercise']//*[@resource-id='"+P+"tvExerciseName'"
                +" and translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')="
                +org.openqa.selenium.support.ui.Quotes.escape(phase.toLowerCase(Locale.ROOT))+"]";
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
        if(isElementVisible(INTENSITY_PICKER)) {
            driver.navigate().back();
            waitForElementNotVisible(INTENSITY_PICKER,"Intensity picker did not close",10);
        }
        if(shown("rvFeedBackDetails")&&!isKegelCompletionIntensityFeedbackDisplayed())
            closePracticeCompletionFeedbackIfPresent();
        closeKegelPlayerInstructionsIfPresent();
        if(shown("tvTimer"))exitKegelExercisePlayer();
        if(shown("tvSelectedIntens"))returnFromKegelExerciseInstruction();
        if(shown("btStartWork"))returnFromDailyPracticeToDailyPlan();
    }
}
