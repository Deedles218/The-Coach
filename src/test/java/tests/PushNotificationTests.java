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
import org.junit.Assume;
import org.junit.Test;

@Epic(value = "The Coach Push notifications")
public class PushNotificationTests extends CoreTestCase {
    @Test
    @Features(value = {@Feature(value = "Push"), @Feature(value = "Permissions")})
    @DisplayName("COA-3461 COA-7918 Notification prompt is shown and can be closed")
    @Description("Verifies the app-level push notification prompt when it is present in the current launch configuration. The test skips cleanly if the one-time prompt is already consumed on the device.")
    @Step("Start test testNotificationPromptCanBeClosed")
    @Severity(value = SeverityLevel.NORMAL)
    public void testNotificationPromptCanBeClosed() {
        if (!Platform.getInstance().isIOS()) {
            return;
        }

        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);

        coachFlow.activateAppIfPossible();
        Assume.assumeTrue(
                "Notification prompt is not present in the current app state; this one-time prompt requires a clean install or launch config that shows Push permissions.",
                coachFlow.isNotificationPromptDisplayed()
        );

        coachFlow.assertNotificationPromptIsDisplayed();
        coachFlow.closeNotificationPromptIfPresent();
        Assert.assertFalse(
                "Notification prompt should be closed after tapping the close button",
                coachFlow.isNotificationPromptDisplayed()
        );
    }
}
