package lib.ui;

import lib.EmailEnforcementFixture.Stage;

/** Platform-specific UI for the shared COA-9222 scenarios. No credentials in Allure steps. */
public interface EmailEnforcementScreen {
    void waitForLaunchEntry();
    void assertStage(Stage stage);
    void dismiss(Stage stage, String action);
    void waitForDaily();
    void assertNoRequest();
    void revisitDaily();
    void enterEmail(String email);
    void submit();
    void submitRepeatedly();
    void assertSingleOtp(String email);
    void changeEmailAndCheckValidation(Stage stage);
    void typeSecurityCode(String code);
    void finishConfirmation();
    void assertProfileEmail(String email);
    void openEmailSupport();
    void returnFromEmailSupport(Stage stage);
    void assertExistingAccount(String email);
    void assertLegacyOutcome();
    void assertDailyUnavailable();
}
