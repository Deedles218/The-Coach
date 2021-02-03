package lib.ui;
import org.openqa.selenium.remote.RemoteWebDriver;

import static lib.ui.SearchPageObject.TAB_FEED;

public class ProfilePageObject extends MainPageObject {
    protected static String
            //профиль пользователя
        USER_PROFILE,
            TAB_PROGRAM,
        SEND_FEEDBACK,
        PRIVACY,
        TERMS,
        DATE_OF_BIRTH,
        HEIGHT,
        WEIGHT,
        EMAIL,
        BACK_TO_PROFILE;
    public ProfilePageObject(RemoteWebDriver driver) {
        super(driver);
    }
    public void tapOnProgramTab() {
        this.waitForElementAndClick(TAB_PROGRAM, "Cannot tap on Profile tab", 10);
    }
    public void tapOnProfileMenu(){
        this.waitForElementAndClick(USER_PROFILE,"Cannot Profile menu",10);

    }

}
