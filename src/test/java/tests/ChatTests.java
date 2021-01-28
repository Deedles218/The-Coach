package tests;

import lib.CoreTestCase;
import lib.ui.SearchPageObject;
import lib.ui.factories.SearchPageObjectFactory;
import org.junit.Test;

public class ChatTests extends CoreTestCase {

    @Test
    public void testChatInput1() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_image";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
       //дописать тап на картинку или проверку что картинка пришла
    }
    public void testChatInput2() throws InterruptedException
    {
        SearchPageObject SearchPageObject = SearchPageObjectFactory.get(driver);
        SearchPageObject.clickOnInputField();
        String debug_message = "/debug_gif";
        SearchPageObject.typeMessage(debug_message);
        SearchPageObject.sendMessage();
        Thread.sleep(7000);
        //дописать тап на гифку или проверку что гиф пришла
    }
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

