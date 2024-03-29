package tests.HW;

import lib.CoreTestCase;
import lib.Platform;
import lib.ui.HWPageObject.WelcomePageObject;
import org.junit.Test;

public class GetStartedTest extends CoreTestCase {
    @Test
    public void testPassThroughWelcome() {
        if ((Platform.getInstance().isAndroid())|| (Platform.getInstance().isMw())){
            return;
        }
        WelcomePageObject WelcomePage = new WelcomePageObject(driver);
            WelcomePage.waitForLearnMoreLink();
            WelcomePage.clickNextButton();

            WelcomePage.waitForNewWaysText();
            WelcomePage.clickNextButton();

            WelcomePage.waitForAddOrEditPreferredLangText();
            WelcomePage.clickNextButton();

            WelcomePage.waitForLearnMoreAboutDataCollectedText();
            WelcomePage.clickGetStartedButton();
        }
    }
