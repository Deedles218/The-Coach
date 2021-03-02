package tests;

import io.qameta.allure.*;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.ui.SearchPageObject;
import lib.ui.factories.SearchPageObjectFactory;
import org.junit.Test;
@Epic(value = "Chat")
public class ChatTests extends CoreTestCase {

    @Test
    @Features(value ={@Feature(value = "ChatBot"),@Feature(value = "Chat")} )
    @DisplayName("Интент /debug_image")
    @Description("Тест отправляет в чате дебажное сообщение и проверяет что пришла картинка")
    @Step("Start test testChatInput1")
    @Severity(value = SeverityLevel.NORMAL)
    public void testChatInput1() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_image";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
        //дописать тап на картинку или проверку что картинка пришла
        SearchPageObject.tapOnImage();
    }
    @Test
    @Features(value ={@Feature(value = "ChatBot"),@Feature(value = "Chat")} )
    @DisplayName("Интент /debug_gif")
    @Description("Тест отправляет в чате дебажное сообщение и проверяет что пришла гифка")
    @Step("Start test testChatInput2")
    @Severity(value = SeverityLevel.NORMAL)
    public void testChatInput2() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_gif";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
        //дописать тап на гифку или проверку что гиф пришла
        SearchPageObject.tapOnGif();
    }
    @Test
    @Features(value ={@Feature(value = "ChatBot"),@Feature(value = "Chat")} )
    @DisplayName("Интент /debug_pdf_test")
    @Description("Тест отправляет в чате дебажное сообщение и проверяет что пришел пдф файл")
    @Step("Start test testChatInput3")
    @Severity(value = SeverityLevel.NORMAL)
    public void testChatInput3() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_pdf_test";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
        //дописать тап на картинку или проверку что картинка пришла
    }

    @Test
    @Features(value ={@Feature(value = "ChatBot"),@Feature(value = "Chat")} )
    @DisplayName("Интент /debug_selector")
    @Description("Тест отправляет в чате дебажное сообщение и получает селекторы")
    @Step("Start test testChatInput4")
    @Severity(value = SeverityLevel.NORMAL)
    public void testChatInput4() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_selector";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
        //дописать тап на картинку или проверку что картинка пришла
    }

    @Test
    @Features(value ={@Feature(value = "ChatBot"),@Feature(value = "Chat")} )
    @DisplayName("Интент /debug_show_video")
    @Description("Тест отправляет в чате дебажное сообщение и получает видео")
    @Step("Start test testChatInput5")
    @Severity(value = SeverityLevel.NORMAL)
    public void testChatInput5() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_show_video";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
        //дописать тап на картинку или проверку что картинка пришла
    }

    @Test
    @Features(value ={@Feature(value = "ChatBot"),@Feature(value = "Chat")} )
    @DisplayName("Интент /debug_daily_plan")
    @Description("Тест отправляет в чате дебажное сообщение и получает карточку с планом на день")
    @Step("Start test testChatInput6")
    @Severity(value = SeverityLevel.NORMAL)
    public void testChatInput6() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_daily_plan";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
        //дописать тап на картинку или проверку что картинка пришла
    }

    @Test
    @Features(value ={@Feature(value = "ChatBot"),@Feature(value = "Chat")} )
    @DisplayName("Интент /debug_link")
    @Description("Тест отправляет в чате дебажное сообщение и получает сообщение с ссылкой")
    @Step("Start test testChatInput7")
    @Severity(value = SeverityLevel.NORMAL)

    public void testChatInput7() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_link";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
        //дописать тап на картинку или проверку что картинка пришла
    }

    @Test
    @Features(value ={@Feature(value = "ChatBot"),@Feature(value = "Chat")} )
    @DisplayName("Интент //debug_cards")
    @Description("Тест отправляет в чате дебажное сообщение и получает карточку с оранжевой кнопкой")
    @Step("Start test testChatInput8")
    @Severity(value = SeverityLevel.NORMAL)
    public void testChatInput8() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_cards";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
        //дописать тап на картинку или проверку что картинка пришла
    }
    @Test
    @Features(value ={@Feature(value = "ChatBot"),@Feature(value = "Chat")} )
    @DisplayName("Интент /debug_questionnaire")
    @Description("Тест отправляет в чате дебажное сообщение и получает карточку daily survey")
    @Step("Start test testChatInput9")
    @Severity(value = SeverityLevel.NORMAL)

    public void testChatInput9() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_questionnaire";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
        //дописать тап на картинку или проверку что картинка пришла
    }
    @Test
    @Features(value ={@Feature(value = "ChatBot"),@Feature(value = "Chat")} )
    @DisplayName("Интент /debug_controls_dimension_picker_height")
    @Description("Тест отправляет в чате дебажное сообщение и получает пикер роста")
    @Step("Start test testChatInput10")
    @Severity(value = SeverityLevel.NORMAL)
    public void testChatInput10() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_controls_dimension_picker_height";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
        //дописать тап на картинку или проверку что картинка пришла
    }
    @Test
    @Features(value ={@Feature(value = "ChatBot"),@Feature(value = "Chat")} )
    @DisplayName("Интент /debug_controls_dimension_picker_weight")
    @Description("Тест отправляет в чате дебажное сообщение и получает пикер веса")
    @Step("Start test testChatInput11")
    @Severity(value = SeverityLevel.NORMAL)
    public void testChatInput11() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_controls_dimension_picker_weight";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
        //дописать тап на картинку или проверку что картинка пришла
    }
}

