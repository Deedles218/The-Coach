package lib.ui;


import lib.Platform;
import org.openqa.selenium.remote.RemoteWebDriver;



abstract public class NavigationUI extends MainPageObject{
    protected static String
    MY_LISTS_LINK,
    places_auth_close = "id:places auth close",
    OPEN_NAVIGATION;

    public NavigationUI(RemoteWebDriver driver)
    {
        super(driver);
    }
    public void openNavigation(){
        if(Platform.getInstance().isMw()) {
            this.waitForElementAndClick(OPEN_NAVIGATION, "Cannot find and click open navigation button", 5);
        } else {
         System.out.println("Method openNavigation() do nothing for platform " + Platform.getInstance().getPlatformVar());
        }
    }
    public void clickMyLists()
    {
        if (Platform.getInstance().isMw()) {
            this.tryClickElementWithFewAttempts(
                    MY_LISTS_LINK,
                    "Cannot find navigation button to My list",
                    5
            );
        } else {
            this.waitForElementAndClick(
                    MY_LISTS_LINK,
                    "Cannot find navigation button to My list",
                    5
            );
        }
    }
    public  void closeIOSSync()
    {
        waitForElementAndClick(places_auth_close,"Cannot find close button for sync articles", 5);
    }
}
