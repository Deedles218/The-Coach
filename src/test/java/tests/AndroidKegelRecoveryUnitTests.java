package tests;

import lib.ui.android.AndroidKegelPageObject;
import org.junit.Assert;
import org.junit.Test;

/** Recovery leaves a prepared intensity fixture available to its existing test. */
public class AndroidKegelRecoveryUnitTests {
    private static class Screen extends AndroidKegelPageObject {
        boolean survey,intensity;int closed;
        Screen(boolean survey,boolean intensity){super(null);this.survey=survey;this.intensity=intensity;}
        @Override public boolean isElementVisible(String locator){return survey&&locator.endsWith("/rvFeedBackDetails");}
        @Override public boolean isKegelCompletionIntensityFeedbackDisplayed(){return intensity;}
        @Override public void closePracticeCompletionFeedbackIfPresent(){closed++;survey=false;}
        @Override public void closeKegelPlayerInstructionsIfPresent(){}
    }
    @Test public void postWorkoutSurveyCanBeLeftDuringCleanup(){
        Screen screen=new Screen(true,false);screen.closeKegelExerciseFlowIfPresent();
        Assert.assertEquals(1,screen.closed);Assert.assertFalse(screen.survey);
    }
    @Test public void preparedIntensityFeedbackIsPreserved(){
        Screen screen=new Screen(true,true);screen.closeKegelExerciseFlowIfPresent();
        Assert.assertEquals(0,screen.closed);Assert.assertTrue(screen.intensity);
    }
    @Test public void ordinaryDailyPlanNeedsNoFeedbackAction(){
        Screen screen=new Screen(false,false);screen.closeKegelExerciseFlowIfPresent();
        Assert.assertEquals(0,screen.closed);
    }
}
