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

@Epic(value = "The Coach Profile")
public class CoachProfileTests extends CoreTestCase {
    @Test
    @Features(value = {@Feature(value = "Profile"), @Feature(value = "Authorization")})
    @DisplayName("Verify Profile interactions and log out")
    @Description("Starts from an authorized state, opens Profile, verifies visible Profile actions, safely cancels destructive account deletion, and logs out.")
    @Step("Start test testProfileVisibleActionsAndLogout")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testProfileVisibleActionsAndLogout() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject CoachFlowPageObject = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", CoachFlowPageObject);

        CoachFlowPageObject.assertAuthorizedDashboardIsDisplayed();
        CoachFlowPageObject.openProfile();
        CoachFlowPageObject.assertProfileScreenIsDisplayed();

        CoachFlowPageObject.openAccountSettingsFromProfile();
        CoachFlowPageObject.returnToProfileFromSubscreen();

        CoachFlowPageObject.openSupportFromProfile();
        CoachFlowPageObject.returnToProfileFromSubscreen();

        CoachFlowPageObject.openMyWorkbookFromProfile();
        CoachFlowPageObject.closeMyWorkbookScreen();

        CoachFlowPageObject.openFaqFromProfile();
        CoachFlowPageObject.closeProfileBrowser();

        CoachFlowPageObject.openTermsFromProfile();
        CoachFlowPageObject.closeProfileBrowser();

        CoachFlowPageObject.verifyDeleteAccountDialogAndCancel();
        CoachFlowPageObject.logOut();
        CoachFlowPageObject.waitForStartScreen();
    }
}
