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
}