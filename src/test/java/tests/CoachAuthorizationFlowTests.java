package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.ui.CoachFlowPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

@Epic(value = "The Coach current iOS flow")
public class CoachAuthorizationFlowTests extends CoreTestCase {
    @Test
    @Features(value = {@Feature(value = "Feed"), @Feature(value = "Profile"), @Feature(value = "Start screen")})
    @DisplayName("Open Feed, log out, and verify start screen actions")
    @Description("Starts from an authorized app state, handles Feed deprecation popup, logs out, and verifies Login and Start buttons on the start screen.")
    @Step("Start test testFeedPopupLogoutAndStartScreenButtons")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testFeedPopupLogoutAndStartScreenButtons() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject CoachFlowPageObject = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", CoachFlowPageObject);

        CoachFlowPageObject.assertAuthorizedDashboardIsDisplayed();
        CoachFlowPageObject.openFeed();
        CoachFlowPageObject.assertFeedDeprecationPopupIsDisplayed();
        CoachFlowPageObject.closeFeedDeprecationPopup();

        CoachFlowPageObject.openProfile();
        CoachFlowPageObject.logOut();

        CoachFlowPageObject.waitForStartScreen();
        CoachFlowPageObject.openLoginFlow();
        CoachFlowPageObject.returnFromLoginFlowToStartScreen();
        CoachFlowPageObject.openStartFlow();
        CoachFlowPageObject.returnFromOnboardingFlowToStartScreen();
    }
}
