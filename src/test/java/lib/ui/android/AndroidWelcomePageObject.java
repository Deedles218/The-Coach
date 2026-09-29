package lib.ui.android;

import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.util.*;

/** Welcome controls; legal links are two ClickableSpans in the observed tvPolicy TextView. */
public final class AndroidWelcomePageObject extends AndroidCoachFlowPageObject {
    private static final String P="com.vamapps.thecoach:id/";
    public AndroidWelcomePageObject(RemoteWebDriver driver) {super(driver);}
    public void verifyButtonsAndLegalLinks() {
        ensureLoggedOutOnStartScreen();
        openLegalSpan(false);openLegalSpan(true);
        openStartFlow();
        waitForElementVisible("id:"+P+"tvAnswerNum","Start did not open the goal questionnaire",15);
        waitForElementAndClick("id:"+P+"ivBackArrow","Cannot leave goal questionnaire",10);
        waitForStartScreen();openLoginFlow();returnFromLoginFlowToStartScreen();
    }
    private void openLegalSpan(boolean privacy) {
        WebElement policy=waitForElementVisible("id:"+P+"tvPolicy","Welcome legal links absent",10);
        Assert.assertEquals("Legal span gesture is only calibrated for the observed English text",
                "By continuing, you agree to our Terms and Privacy",policy.getText());
        Rectangle bounds=policy.getRect();
        Assert.assertTrue("Legal spans wrapped; semantic links must be exposed before testing this layout",bounds.height<driver.manage().window().getSize().height*.05);
        // Observed English single-line layout; no separate accessibility nodes exist for the spans.
        Map<String,Object> tap=new HashMap<String,Object>();
        tap.put("x",(int)(driver.manage().window().getSize().width*(privacy?.81:.635)));
        tap.put("y",bounds.y+bounds.height/2);
        ((JavascriptExecutor)driver).executeScript("mobile: clickGesture",tap);
        String title=privacy?"PRIVACY POLICY":"TERMS OF";
        waitForElementVisible("xpath://*[contains(translate(@text,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'"+title+"')]","Legal destination did not load: "+title,30);
        driver.navigate().back();activateAppIfPossible();waitForStartScreen();
    }
}
