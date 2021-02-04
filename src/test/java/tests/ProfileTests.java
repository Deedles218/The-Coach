package tests;

import lib.CoreTestCase;
import lib.ui.ProfilePageObject;
import lib.ui.SearchPageObject;
import lib.ui.factories.ProfilePageObjectFactory;
import lib.ui.factories.SearchPageObjectFactory;
import org.junit.Test;

public class ProfileTests extends CoreTestCase {
    @Test
    public void testOpenProfile(){
        ProfilePageObject ProfilePageObject = ProfilePageObjectFactory.get(driver);
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        ProfilePageObject.tapOnProgramTab();
        ProfilePageObject.tapOnProfileMenu();
    }
    @Test
    public void testAssertUserData(){
        ProfilePageObject ProfilePageObject = ProfilePageObjectFactory.get(driver);
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        ProfilePageObject.tapOnProgramTab();
        ProfilePageObject.tapOnProfileMenu();
        ProfilePageObject.assertTerms();
        ProfilePageObject.assertPrivacy();
        ProfilePageObject.assertSendFeedback();
    }
    @Test
    public  void testOpenNutrition()throws InterruptedException{
        ProfilePageObject ProfilePageObject = ProfilePageObjectFactory.get(driver);
        ProfilePageObject.tapOnProgramTab();
        ProfilePageObject.openNutrition();
        Thread.sleep(7000);
        ProfilePageObject.openFasting();
        Thread.sleep(7000);
//        ProfilePageObject.setFastingOff();
//        Thread.sleep(7000);
//        ProfilePageObject.setFastingOn();
        ProfilePageObject.startFastPiker();
        Thread.sleep(7000);
        ProfilePageObject.setFastTimeInPiker();
        ProfilePageObject.saveFastTime();
        Thread.sleep(7000);
        ProfilePageObject.startFast();
        Thread.sleep(7000);
    }

}
