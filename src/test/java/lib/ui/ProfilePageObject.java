package lib.ui;
import org.openqa.selenium.remote.RemoteWebDriver;

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
    NUTRITION,
    FASTING,
    FASTING_OFF,
    FASTING_ON,
    CHANGE_TIME,
            CHANGED_TIME,
    SAVE_BUTTON,
            FASTING_PIKER_OPEN,
        BACK_TO_PROFILE;


    public ProfilePageObject(RemoteWebDriver driver) {
        super(driver);
    }
    public void tapOnProgramTab() {
        this.waitForElementAndClick(TAB_PROGRAM, "Cannot tap on Profile tab", 10);
    }
    public void tapOnProfileMenu(){
        this.waitForElementAndClick(USER_PROFILE,"Cannot Profile menu",10); }
        public void assertTerms(){
        this.waitForElementPresent(TERMS,"cannot find Terms",10);
        }
    public void assertPrivacy(){
        this.waitForElementPresent(PRIVACY,"cannot find privacy",10);
    }
    public void assertSendFeedback(){
        this.waitForElementAndClick(SEND_FEEDBACK,"cannot feedback",10);
    }
    public void openNutrition(){
        this.waitForElementAndClick(NUTRITION,"lol",10);
    }
    public void openFasting(){
        this.waitForElementAndClick(FASTING,"fasting not found",10);
    }
    public void setFastingOff(){
        this.waitForElementAndClick(FASTING_OFF,"Cannot set toggle to off position",10);

    }
    public void setFastingOn(){
        this.waitForElementAndClick(FASTING_ON,"Cannot set toggle to fasting on position",10);

    }
    public void startFastPiker(){
        this.waitForElementAndClick(FASTING_PIKER_OPEN,"Cannot change fasting time",10);
    }
    public void setFastTimeInPiker(){
        this.swipeUp(3);
    }
    public void saveFastTime(){
        this.waitForElementAndClick(SAVE_BUTTON,"Cannot tap save button",5);
    }
}
