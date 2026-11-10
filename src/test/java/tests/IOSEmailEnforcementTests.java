package tests;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.*;
import io.qameta.allure.junit4.DisplayName;
import lib.*;
import lib.EmailEnforcementFixture.Stage;
import lib.ui.factories.OnboardingPageObjectFactory;
import lib.ui.EmailEnforcementScreen;
import lib.ui.ios.iOSEmailEnforcementPageObject;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.experimental.categories.Category;

import java.util.Map;

@Epic("Account access")
@Feature("COA-9222 Mandatory email collection — iOS")
public class IOSEmailEnforcementTests extends CoreTestCase {
    @Rule public TestName name = new TestName();
    private EmailEnforcementFixture fixture;
    private EmailEnforcementScreen ui;
    private Map<String, Object> baseline;

    @Before
    @Override public void setUp() throws Exception {
        requireScenarioPlatform();
        fixture = new EmailEnforcementFixture(name.getMethodName());
        Assert.assertFalse("Email datasets must retain their installation", "reinstall".equals(TestData.testIsolationMode()));
        fixture.requireEnabled();
        super.setUp();
        ui = createEmailScreen(fixture);
        if (fixture.newUser()) {
            OnboardingPageObjectFactory.get(driver).completeNewUserJourneyToPaywall(
                    fixture.value("onboarding.goal", Platform.getInstance().getOnboardingGoal()));
        }
        reachDailyWithoutDismissingRequest();
        ui.assertStage(fixture.stage());
        baseline = fixture.evidence("account");
        EmailEnforcementFixture.assertUnlinked(baseline);
    }

    protected void requireScenarioPlatform() { requireIOSPlatform(); }

    protected EmailEnforcementScreen createEmailScreen(EmailEnforcementFixture fixture) {
        return new iOSEmailEnforcementPageObject(driver, fixture);
    }

    @Test @Issue("COA-9696")
    @DisplayName("COA-9696 E1/E2 can be dismissed; E3 remains mandatory across launches")
    public void testCoa9696ProgressiveEnforcement() throws Exception {
        String action = fixture.required("dismiss.action");
        ui.dismiss(Stage.E1, action);
        reopenAtDaily();
        ui.assertStage(Stage.E2);
        ui.dismiss(Stage.E2, action);
        reopenAtDaily();
        ui.assertStage(Stage.E3);
        assertUnchangedAndUnlinked();
        reopenAtDaily();
        ui.assertStage(Stage.E3);
        assertUnchangedAndUnlinked();
    }

    @Test @Issue("COA-9697")
    @Category(EmailEnforcementScope.NotRequired.class)
    @DisplayName("COA-9697 Applied OFF immediately releases enforcement and restores the applicable legacy flow")
    public void testCoa9697FeatureOffUnlocksImmediately() throws Exception {
        requireMandatory();
        RestoringTestAction.run(() -> {
            fixture.control("feature-off");
            Map<String, Object> config = fixture.evidence("config");
            Assert.assertEquals("OFF was not applied in the app", Boolean.FALSE, config.get("enabled"));
            Assert.assertEquals("Applying OFF changed the legacy A/B flag",
                    Boolean.valueOf(fixture.required("legacy.abEnabled")), config.get("legacyEnabled"));
            // No relaunch: this is the immediate-unlock assertion in Xray.
            ui.assertLegacyOutcome();
            assertUnchangedAndUnlinked();
        }, () -> {
            fixture.control("feature-restore");
            Assert.assertEquals("Original ON config was not restored", Boolean.TRUE, fixture.evidence("config").get("enabled"));
        });
    }

    @Test @Issue("COA-9698")
    @Category(EmailEnforcementScope.NotRequired.class)
    @DisplayName("COA-9698 A genuinely unavailable Daily does not consume the next request")
    public void testCoa9698UnavailableDailyDoesNotConsumeStage() throws Exception {
        Assert.assertEquals("This dataset must start with the actually displayed E1", Stage.E1, fixture.stage());
        ui.dismiss(Stage.E1, fixture.required("dismiss.action"));
        RestoringTestAction.run(() -> {
            fixture.control("network-off");
            reopenAtDaily();
            ui.assertDailyUnavailable();
            fixture.control("network-on");
            reopenAtDaily();
            ui.assertStage(Stage.E2);
            ui.dismiss(Stage.E2, fixture.required("dismiss.action"));
            reopenAtDaily();
            ui.assertStage(Stage.E3);
            assertUnchangedAndUnlinked();
        }, () -> fixture.control("network-on"));
    }

    @Test @Issue("COA-9699")
    @DisplayName("COA-9699 Killing the displayed request consumes its stage without requiring dismissal")
    public void testCoa9699KillAdvancesStage() throws Exception {
        Assert.assertNotEquals("Use independent E1, E2 and E3 existing-user datasets", Stage.N, fixture.stage());
        reopenAtDaily();
        assertNextStage();
        assertUnchangedAndUnlinked();
    }

    @Test @Issue("COA-9700")
    @Category(EmailEnforcementScope.Deferred.class)
    @DisplayName("COA-9700 In-place update preserves enforcement history, identity and progress")
    public void testCoa9700UpdatePreservesStage() throws Exception {
        Assert.assertNotEquals("Update datasets start at E1/E2/E3", Stage.N, fixture.stage());
        String app = fixture.required("update.app");
        Map<String, Object> before = fixture.evidence("build");
        Map<String, Object> update = fixture.evidence("artifact", app);
        EmailEnforcementFixture.assertNewerBuild(before, update);
        InteractsWithApps apps = (InteractsWithApps) driver;
        apps.terminateApp(Platform.getInstance().getIOSBundleId());
        apps.installApp(app); // Deliberately no removeApp/fullReset: installation-local history is measured.
        apps.activateApp(Platform.getInstance().getIOSBundleId());
        reachDailyWithoutDismissingRequest();
        ui.assertStage(fixture.stage().next());
        Assert.assertEquals("Updated artifact is not the installed build", update, fixture.evidence("build"));
        assertUnchangedAndUnlinked();
    }

    @Test @Issue("COA-9701")
    @DisplayName("COA-9701 Repeated Daily navigation in one process does not advance enforcement")
    public void testCoa9701RepeatedDailyDoesNotAdvanceStage() throws Exception {
        Assert.assertTrue("Navigation datasets must start at E1 or E2", fixture.stage().dismissible());
        ui.dismiss(fixture.stage(), fixture.required("dismiss.action"));
        for (int i = 0; i < 3; i++) ui.revisitDaily();
        reopenAtDaily();
        assertNextStage();
        assertUnchangedAndUnlinked();
    }

    private void assertNextStage() {
        Stage next = fixture.stage().next();
        ui.assertStage(next);
        // E1 and E2 share their main form. Only E2's dismissal warning proves advancement.
        if (next == Stage.E2) ui.dismiss(Stage.E2, "cross");
    }

    @Test @Issue("COA-9702")
    @DisplayName("COA-9702 Support opens, returns to the mandatory request, and email confirmation still succeeds")
    public void testCoa9702SupportReturnsToMandatoryFlow() throws Exception {
        requireMandatory();
        ui.openEmailSupport();
        ui.returnFromEmailSupport(fixture.stage());
        assertUnchangedAndUnlinked();
        confirmEmail(false, false);
    }

    @Test @Issue("COA-9411")
    @DisplayName("COA-9411 Full email/OTP linking preserves the current account and suppresses future requests")
    public void testCoa9411EmailConfirmationPreservesAccount() throws Exception {
        confirmEmail(false, !fixture.stage().dismissible());
    }

    @Test @Issue("COA-9415")
    @Category(EmailEnforcementScope.NotRequired.class)
    @DisplayName("COA-9415 Repeated submission during processing yields one confirmation and one account link")
    public void testCoa9415RepeatedSubmitCreatesOneFlow() throws Exception {
        requireMandatory();
        confirmEmail(true, false);
    }

    @Test @Issue("COA-9424")
    @DisplayName("COA-9424 Occupied email offers Login without linking or switching the current account")
    public void testCoa9424OccupiedEmailOffersLogin() throws Exception {
        requireMandatory();
        String email = TestData.existingProgressAccount().getEmail();
        ui.enterEmail(email);
        ui.submit();
        ui.assertExistingAccount(email);
        assertUnchangedAndUnlinked();
    }

    private void confirmEmail(boolean repeated, boolean checkChangeEmail) throws Exception {
        String email = TestData.validEmailWithoutProgress();
        ui.enterEmail(email);
        if (repeated) ui.submitRepeatedly(); else ui.submit();
        ui.assertSingleOtp(email);
        assertPendingConfirmation();
        if (checkChangeEmail) {
            ui.changeEmailAndCheckValidation(fixture.stage());
            ui.enterEmail(email);
            ui.submit();
            ui.assertSingleOtp(email);
            assertPendingConfirmation();
        }
        ui.typeSecurityCode(TestData.existingProgressAccount().getOtp());
        ui.finishConfirmation();
        assertConfirmed();
        ui.assertProfileEmail(email);
        reopenAtDaily();
        ui.waitForDaily();
        ui.assertNoRequest();
        assertConfirmed();
    }

    private void assertPendingConfirmation() throws Exception {
        Map<String, Object> state = fixture.evidence("account");
        EmailEnforcementFixture.assertPreserved(baseline, state);
        Assert.assertEquals("Sending an address prematurely completed linking", Boolean.FALSE, state.get("emailVerified"));
    }

    private void assertConfirmed() throws Exception {
        Map<String, Object> state = fixture.evidence("account");
        EmailEnforcementFixture.assertPreserved(baseline, state);
        Assert.assertEquals("Email is not fully verified", Boolean.TRUE, state.get("emailVerified"));
        Assert.assertEquals("Another email was linked", Boolean.TRUE, state.get("emailMatchesExpected"));
    }

    private void assertUnchangedAndUnlinked() throws Exception {
        Map<String, Object> state = fixture.evidence("account");
        EmailEnforcementFixture.assertPreserved(baseline, state);
        EmailEnforcementFixture.assertUnlinked(state);
    }

    private void requireMandatory() {
        Assert.assertFalse("Use an independently prepared N/E3 fixture", fixture.stage().dismissible());
    }

    private void reopenAtDaily() {
        closeAndReopenCoachApplication();
        reachDailyWithoutDismissingRequest();
    }

    protected void reachDailyWithoutDismissingRequest() {
        ui.waitForLaunchEntry();
        // Stop at the measured form before generic optional-popup cleanup can dismiss E1/E2.
        OnboardingPageObjectFactory.get(driver).closePaywallsWithoutConsumingSlides(
                new String[]{fixture.required("form.title")});
    }

    @Override protected void cleanupSessionForNextTest() {
        // The measured local stage and anonymous fixture must survive failure for diagnosis.
        // Every invocation requires its own declared initial state; never log out to hide a blocked form.
    }
}
