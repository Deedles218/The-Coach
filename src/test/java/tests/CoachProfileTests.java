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
    private static final String EXISTING_PROGRESS_EMAIL = "ds@vamapps.com";
    private static final String OTP_CODE = "8654";

    @Test
    @Features(value = {@Feature(value = "Profile"), @Feature(value = "Authorization")})
    @DisplayName("COA-7205 Profile screen shows progress and settings blocks")
    @Description("Starts from the existing-progress account, opens Profile from Today, and verifies the main profile metrics and action blocks.")
    @Step("Start test test01ProfileScreenShowsProgressAndSettings")
    @Severity(value = SeverityLevel.BLOCKER)
    public void test01ProfileScreenShowsProgressAndSettings() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureExistingProgressUserIsLoggedIn(EXISTING_PROGRESS_EMAIL, OTP_CODE);
        coachFlow.openProfile();
        coachFlow.assertProfileScreenIsDisplayed();
    }

    @Test
    @Features(value = {@Feature(value = "Profile"), @Feature(value = "Navigation")})
    @DisplayName("COA-8503 Profile can be opened from main tabs")
    @Description("Verifies Profile access from Today and Explore for an authorized existing-progress user.")
    @Step("Start test test02ProfileCanBeOpenedFromMainTabs")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test02ProfileCanBeOpenedFromMainTabs() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureExistingProgressUserIsLoggedIn(EXISTING_PROGRESS_EMAIL, OTP_CODE);
        coachFlow.assertProfileCanBeOpenedFromMainTabs();
    }

    @Test
    @Features(value = {@Feature(value = "Profile"), @Feature(value = "Account safety")})
    @DisplayName("COA-8502 Delete account confirmation can be cancelled")
    @Description("Opens the destructive delete-account confirmation and cancels it; the test never confirms account deletion.")
    @Step("Start test test03DeleteAccountConfirmationCanBeCancelled")
    @Severity(value = SeverityLevel.CRITICAL)
    public void test03DeleteAccountConfirmationCanBeCancelled() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureExistingProgressUserIsLoggedIn(EXISTING_PROGRESS_EMAIL, OTP_CODE);
        coachFlow.openProfile();
        coachFlow.verifyDeleteAccountDialogAndCancel();
    }

    @Test
    @Features(value = {@Feature(value = "Profile"), @Feature(value = "Authorization")})
    @DisplayName("COA-8500 Authorized user can log out")
    @Description("Logs out from Profile and verifies the start screen is displayed.")
    @Step("Start test test04AuthorizedUserCanLogOut")
    @Severity(value = SeverityLevel.BLOCKER)
    public void test04AuthorizedUserCanLogOut() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.ensureExistingProgressUserIsLoggedIn(EXISTING_PROGRESS_EMAIL, OTP_CODE);
        coachFlow.openProfile();
        coachFlow.logOut();
        coachFlow.waitForStartScreen();
    }
}
