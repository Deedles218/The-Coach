package tests;

import io.qameta.allure.*;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.ui.ProfilePageObject;
import lib.ui.SearchPageObject;
import lib.ui.factories.ProfilePageObjectFactory;
import lib.ui.factories.SearchPageObjectFactory;
import org.junit.Test;
@Epic(value = "Profile")
public class ProfileTests extends CoreTestCase {
    @Test

    @Features(value ={@Feature(value = "Program"),@Feature(value = "Tracks")} )
    @DisplayName("Открыть настройки профиля")
    @Description("Тест открывает экран с данными пользователя")
    @Step("Start test testOpenProfile")
    @Severity(value = SeverityLevel.NORMAL)
    public void testOpenProfile(){
        ProfilePageObject ProfilePageObject = ProfilePageObjectFactory.get(driver);
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        ProfilePageObject.tapOnProgramTab();
        ProfilePageObject.tapOnProfileMenu();
    }
    @Test
    @Features(value ={@Feature(value = "Program"),@Feature(value = "Tracks")} )
    @DisplayName("Данные на экране инфы о пользователе")
    @Description("Проверяем что на экране есть terms privacy и линк отправки фидбэка")
    @Step("Start test testAssertUserData")
    @Severity(value = SeverityLevel.BLOCKER)
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
    @Features(value ={@Feature(value = "Program"),@Feature(value = "Tracks")} )
    @DisplayName("Трэк нутришн фастинг")
    @Description("Открываем фастинг включаем его запускаем таймер")
    @Step("Start test testOpenNutrition")
    @Severity(value = SeverityLevel.BLOCKER)
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
