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
        FASTING="xpath://XCUIElementTypeStaticText[@label='Fasting']";
        FASTING_OFF="xpath://XCUIElementTypeSwitch[@value='1']";
        FASTING_ON="xpath://XCUIElementTypeSwitch[@value='0']";
        FASTING_PIKER_OPEN ="id:ic outline disclosure down";
        CHANGE_TIME="xpath://XCUIElementTypePickerWheel";
        //CHANGED_TIME="xpath://XCUIElementTypePickerWheel[@value='16 o’clock']";
        SAVE_BUTTON="xpath://XCUIElementTypeButton[@name='SAVE']";

        //элементы экрана програмы
//
//        PROGRAM_DP="xpath://XCUIElementTypeStaticText[@name='Your Plan for Today']";
//        TRACKS="id:YOUR TRACKS";
        NUTRITION="id:Nutrition";
//        SPORT="id:Sport";
//        SLEEP="id:Sleep";
//        HAIR_LOSS="id:Hair loss";
//        MENTAL_HEALTH="id:Mental Health";
//        MALE_HEALTH="id:Male Health";
//        SUPPLEMENTS="id:Supplements";
    }
    public iOSProfilePageObject(RemoteWebDriver driver)
    {
        super(driver);
    }

}
