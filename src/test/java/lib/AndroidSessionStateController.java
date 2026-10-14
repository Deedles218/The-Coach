package lib;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Allure;
import lib.ui.CoachFlowPageObject;
import org.openqa.selenium.remote.RemoteWebDriver;

/** Restores Android to Welcome and verifies the result before another test starts. */
public final class AndroidSessionStateController {
    private final RemoteWebDriver driver;
    private final CoachFlowPageObject coachFlow;

    public AndroidSessionStateController(RemoteWebDriver driver, CoachFlowPageObject coachFlow) {
        this.driver = driver;
        this.coachFlow = coachFlow;
    }

    public boolean restoreWelcome() {
        Throwable firstFailure;
        try {
            normalizeAndVerify();
            return false;
        } catch (Throwable failure) {
            firstFailure = failure;
            attachRecoveryEvidence("Initial Android cleanup failed", failure);
        }

        try {
            recoverApplicationOnce();
            normalizeAndVerify();
            attachRecoveryEvidence("Android cleanup recovered after one "
                    + TestData.androidRecoveryMode(), firstFailure);
            return "reinstall".equals(TestData.androidRecoveryMode());
        } catch (Throwable recoveryFailure) {
            recoveryFailure.addSuppressed(firstFailure);
            attachRecoveryEvidence("Android cleanup recovery failed", recoveryFailure);
            throw new SessionStateRecoveryException(
                    "Android session could not be restored to Welcome after one "
                            + TestData.androidRecoveryMode() + " attempt",
                    recoveryFailure
            );
        }
    }

    private void normalizeAndVerify() {
        coachFlow.cleanupForNextTest();
        coachFlow.assertReadyForNextTest();
    }

    private void recoverApplicationOnce() {
        if (!(driver instanceof InteractsWithApps)) {
            throw new IllegalStateException("Android recovery requires an Appium mobile driver");
        }

        InteractsWithApps apps = (InteractsWithApps) driver;
        String packageName = Platform.getInstance().getAndroidAppPackage();
        if ("reinstall".equals(TestData.androidRecoveryMode())) {
            String appPath = Platform.getInstance().getAndroidAppPath();
            if (appPath == null || appPath.trim().isEmpty()) {
                throw new IllegalStateException(
                        "ANDROID_RECOVERY_MODE=reinstall requires -Dandroid.app=... or ANDROID_APP"
                );
            }
            if (apps.isAppInstalled(packageName) && !apps.removeApp(packageName)) {
                throw new IllegalStateException("Could not remove Android app during recovery: " + packageName);
            }
            apps.installApp(appPath);
        }
        apps.terminateApp(packageName);
        apps.activateApp(packageName);
    }

    private void attachRecoveryEvidence(String title, Throwable failure) {
        String failureText = String.valueOf(failure);
        String state = failureText.contains("root AccessibilityNodeInfo")
                ? "unavailable (UiAutomator2 accessibility)"
                : safelyDescribeSessionState();
        String body = title + "\n"
                + "state=" + state + "\n"
                + "recoveryMode=" + TestData.androidRecoveryMode() + "\n"
                + "failure=" + TestData.sanitizeSensitiveData(String.valueOf(failure));
        try {
            Allure.addAttachment("Android session recovery", "text/plain", body, ".txt");
        } catch (Throwable attachmentFailure) {
            System.err.println("Could not attach Android recovery evidence: "
                    + attachmentFailure.getClass().getSimpleName());
        }
    }

    private String safelyDescribeSessionState() {
        try {
            return coachFlow.describeSessionState();
        } catch (Throwable stateFailure) {
            return "unavailable (" + stateFailure.getClass().getSimpleName() + ")";
        }
    }
}
