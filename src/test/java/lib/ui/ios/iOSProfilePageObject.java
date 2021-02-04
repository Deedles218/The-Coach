package lib.ui.ios;

import lib.ui.ProfilePageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

public class iOSProfilePageObject extends ProfilePageObject {
    static {
        //профиль пользователя
        USER_PROFILE="xpath://XCUIElementTypeNavigationBar[@name='The_Coach.DashboardView']/XCUIElementTypeButton";
        TAB_PROGRAM="id:Program";
        SEND_FEEDBACK="xpath://XCUIElementTypeButton[@name='Send Feedback to Developers']";
        PRIVACY="xpath://XCUIElementTypeButton[@name='Privacy Policy']";
        TERMS="xpath://XCUIElementTypeButton[@name='Terms of Service']";

        DATE_OF_BIRTH="id:Date of Birth";
        HEIGHT="id:Height";
        WEIGHT="id:Weight";
        EMAIL="id:Email";
        BACK_TO_PROFILE="xpath://XCUIElementTypeButton[@name='Program'])[1]";
    }
    public iOSProfilePageObject(RemoteWebDriver driver)
    {
        super(driver);
    }

}
