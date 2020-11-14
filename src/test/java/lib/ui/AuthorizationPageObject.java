package lib.ui;

import org.openqa.selenium.remote.RemoteWebDriver;
public class AuthorizationPageObject extends MainPageObject {
    private  static final String
    LOGIN_BUTTON ="xpath://body/div//a[text()='Log in']",
    LOGIN_INPUT = "css:#wpName1",
    PASSWORD_INPUT = "css:#wpPassword1",
    SUBMIT_BUTTON = "css:button#wpLoginAttempt"; //wpLoginAttempt
    public AuthorizationPageObject(RemoteWebDriver driver) {
        super(driver);
    }
    public void clickAuthButton() throws InterruptedException {
        Thread.sleep(1000);
        this.waitForElementPresent(LOGIN_BUTTON, "Cannot find auth button",5);
        this.waitForElementAndClick(LOGIN_BUTTON, "Cannot find click auth button",5);}
    public void enterLoginData(String login, String password) throws InterruptedException {
        Thread.sleep(5000);
        this.waitForElementAndSendKeys(LOGIN_INPUT, login, "Cannot find and put login to the login input",5);
        this.waitForElementAndSendKeys(PASSWORD_INPUT, password,"Cannot find and put password to the password input",5);
    }
    public void submitForm(){
        this.waitForElementAndClick(SUBMIT_BUTTON, "Cannot find and click submit auth button",5);
    }
}
