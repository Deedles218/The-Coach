package tests;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
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

@Epic(value = "The Coach Push permissions")
public class PushPermissionTests extends CoreTestCase {
    @Test
    @Features(value = {@Feature(value = "Push"), @Feature(value = "Clean install"), @Feature(value = "Permissions")})
    @Issue("COA-7918")
    @DisplayName("COA-7918 Clean install shows the in-app and iOS push permission prompts")
    @Description("Runs the first-install flow with Appium fullReset/noReset=false, verifies the app push permission screen, allows it, and then accepts the iOS system notification dialog.")
    @Step("Start test testCleanInstallShowsPushPermissionPrompt")
    @Severity(value = SeverityLevel.BLOCKER)
    public void testCleanInstallShowsPushPermissionPrompt() {
        requireIOSPlatform();

        requireCleanInstallConfiguration();
        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.activateAppIfPossible();
        completeOnboardingIfThePromptIsNotAlreadyVisible(coachFlow);
        allowInAppAndSystemPermission(coachFlow);
    }

    @Test
    @Features(value = {@Feature(value = "Push"), @Feature(value = "Reinstall"), @Feature(value = "Permissions")})
    @Issue("COA-7918")
    @DisplayName("COA-7918 Reinstall shows the push permission prompt again")
    @Description("Removes and reinstalls the configured iOS app artifact, then verifies that the one-time push permission flow is available again. This case requires an app path so the reinstall is real, not only a data reset.")
    @Step("Start test testReinstallShowsPushPermissionPromptAgain")
    @Severity(value = SeverityLevel.CRITICAL)
    public void testReinstallShowsPushPermissionPromptAgain() {
        requireIOSPlatform();

        requireCleanInstallConfiguration();
        String appPath = Platform.getInstance().getIOSAppPath();
        Assert.assertTrue(
                "A real reinstall requires -Dios.app=/path/to/The-Coach.app or IOS_APP",
                appPath != null && !appPath.trim().isEmpty()
        );
        Assert.assertTrue("The current driver must support app install/remove", driver instanceof InteractsWithApps);

        InteractsWithApps apps = (InteractsWithApps) driver;
        Assert.assertTrue(
                "The existing app could not be removed before reinstall",
                apps.removeApp(Platform.getInstance().getIOSBundleId())
        );
        apps.installApp(appPath);

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        coachFlow.activateAppIfPossible();
        completeOnboardingIfThePromptIsNotAlreadyVisible(coachFlow);
        allowInAppAndSystemPermission(coachFlow);
    }

    private void completeOnboardingIfThePromptIsNotAlreadyVisible(CoachFlowPageObject coachFlow) {
        if (coachFlow.isNotificationPromptDisplayed()) {
            return;
        }

        coachFlow.waitForStartScreen();
        coachFlow.openStartFlow();
        coachFlow.completeOnboardingUsingConfiguredSteps(Platform.getInstance().getIOSOnboardingStepLocators());
    }

    private void allowInAppAndSystemPermission(CoachFlowPageObject coachFlow) {
        coachFlow.assertNotificationPromptIsDisplayed();
        coachFlow.allowNotificationPrompt();
        String systemAlertText = coachFlow.allowSystemNotificationPermission();
        Assert.assertTrue(
                "The iOS system permission dialog must contain notification permission text",
                systemAlertText != null && !systemAlertText.trim().isEmpty()
        );
    }

    private void requireCleanInstallConfiguration() {
        Assert.assertTrue(
                "Push permission suite requires -Dios.fullReset=true and -Dios.noReset=false (or IOS_FULL_RESET=true / IOS_NO_RESET=false)",
                Platform.getInstance().isCleanInstallConfigured()
        );
        Assert.assertTrue(
                "A clean-install permission test requires the app artifact: set -Dios.app=/path/to/The-Coach.app or IOS_APP",
                Platform.getInstance().getIOSAppPath() != null && !Platform.getInstance().getIOSAppPath().trim().isEmpty()
        );
    }
}
