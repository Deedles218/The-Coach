package lib.ui;
import org.openqa.selenium.remote.RemoteWebDriver;

public class ProfilePageObject extends MainPageObject {
    protected static String
            //профиль пользователя
        USER_PROFILE,
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
}
