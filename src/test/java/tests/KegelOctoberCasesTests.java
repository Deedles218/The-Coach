package tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.RestoringTestAction;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.KegelCasePageObject;
import lib.ui.KegelCasePageObject.Exercise;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Feature("October reviewed mobile UI cases")
public class KegelOctoberCasesTests extends CoreTestCase {
    private KegelCasePageObject preparedUi;
    @Test
    @Issue("COA-8096")
    @DisplayName("COA-8096 Guide close returns to the same regular Kegel workout")
    public void testCoa8096GuideReturnsToSameWorkout() throws Exception {
        KegelCasePageObject ui = new KegelCasePageObject(driver);
        preparedUi = ui;
        ui.requireKeys("regular.card", "regular.screen", "workout.identity", "player.phase", "guide.open", "guide.screen", "guide.close");
        DailyPlanPageObject daily = openRegularPlayer(ui, "COA-8096");
        String workout = ui.text("workout.identity");
        String phase = ui.text("player.phase");
        RestoringTestAction.run(() -> {
            ui.tap("guide.open");
            ui.visible("guide.screen");
            ui.tap("guide.close");
            ui.gone("guide.screen");
            ui.visible("player.paused");
            Assert.assertEquals("Guide returned to a different workout", workout, ui.text("workout.identity"));
            Assert.assertEquals("Guide changed the paused exercise", phase, ui.text("player.phase"));
        }, () -> {
            ui.closeGuideIfOpen();
            daily.closeKegelExerciseFlowIfPresent();
        });
    }

    @Test
    @Issue("COA-8094")
    @DisplayName("COA-8094 Information describes the current regular Kegel exercise")
    public void testCoa8094InfoDescribesCurrentExercise() throws Exception {
        KegelCasePageObject ui = new KegelCasePageObject(driver);
        preparedUi = ui;
        ui.requireKeys("regular.card", "regular.screen", "workout.identity", "player.phase", "info.open", "info.screen", "info.title", "info.content", "info.close");
        DailyPlanPageObject daily = openRegularPlayer(ui, "COA-8094");
        String phase = ui.text("player.phase");
        // Exact mapping comes from the reviewed content for this build, never from the modal under test.
        String exercise = ui.text("workout.identity");
        String expectedTitle = ui.required("info.expectedTitle." + exercise);
        String expectedContent = ui.required("info.expectedContent." + exercise);
        RestoringTestAction.run(() -> {
            ui.tap("info.open");
            ui.visible("info.screen");
            Assert.assertEquals("Information is for another exercise", expectedTitle, ui.text("info.title"));
            Assert.assertTrue("Expected current-exercise instructions are absent",
                    ui.text("info.content").contains(expectedContent));
            ui.tap("info.close");
            ui.gone("info.screen");
            ui.visible("player.paused");
            Assert.assertEquals("Closing information changed the exercise", phase, ui.text("player.phase"));
        }, () -> {
            if (ui.isElementVisible(ui.required("info.close"))) ui.tap("info.close");
            daily.closeKegelExerciseFlowIfPresent();
        });
    }

    @Test
    @Issue("COA-8170")
    @DisplayName("COA-8170 Cancel preserves Custom Kegel exercises and count")
    public void testCoa8170CancelKeepsExercise() throws Exception {
        KegelCasePageObject ui = new KegelCasePageObject(driver);
        preparedUi = ui;
        // Restoration is required even here: a broken Cancel can actually delete the item.
        ui.requireCustomSelectors(true);
        openWorkout(ui, "COA-8170", true);
        List<Exercise> before = ui.snapshot();
        Assert.assertFalse("Dedicated Custom Kegel fixture needs an exercise", before.isEmpty());
        RestoringTestAction.run(() -> {
            ui.requestFirstRemoval();
            ui.cancelRemoval();
            Assert.assertEquals("Cancel changed exercise names, count, order or settings", before, ui.snapshot());
        }, () -> ui.restore(before));
    }

    @Test
    @Issue("COA-8171")
    @DisplayName("COA-8171 Empty Custom Kegel disables Start and permits adding an exercise")
    public void testCoa8171EmptyWorkoutDisablesStart() throws Exception {
        KegelCasePageObject ui = new KegelCasePageObject(driver);
        preparedUi = ui;
        ui.requireCustomSelectors(true);
        openWorkout(ui, "COA-8171", true);
        List<Exercise> before = ui.snapshot();
        Assert.assertFalse("Dedicated Custom Kegel fixture needs an exercise", before.isEmpty());
        RestoringTestAction.run(() -> {
            ui.assertStartEnabled(true);
            ui.removeAll();
            ui.assertStartEnabled(false);
            ui.assertElementEnabled(ui.required("custom.add"), "Add exercise must remain enabled", 10);
            ui.add(before.get(0));
            Assert.assertEquals("Adding from an empty list must create exactly one exercise", 1, ui.snapshot().size());
            ui.assertStartEnabled(true);
        }, () -> ui.restore(before));
    }

    @Test
    @Issue("COA-7937")
    @DisplayName("COA-7937 SIAS(M) marketing link logs into the corresponding account")
    public void testCoa7937MarketingLinkAutoLogin() throws Exception {
        requireMobilePlatform();
        String link = secret("COACH_COA7937_LINK");
        String email = secret("COACH_COA7937_EMAIL");
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        String fixture = System.getProperty("coach.kegelUi.fixture");
        if (fixture != null && !fixture.trim().isEmpty()) {
            preparedUi = new KegelCasePageObject(driver);
            preparedUi.ensureLoggedOutOnObservedStart(coach);
        } else {
            coach.ensureLoggedOutOnStartScreen();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("url", link);
        if (Platform.getInstance().isAndroid()) {
            // Let Android resolve the real HTTPS app link, including its redirect flow.
            // Forcing the application's package can bypass a marketing-link redirect.
            args.put("waitForLaunch", true);
        }
        try {
            driver.executeScript("mobile: deepLink", args);
        } catch (org.openqa.selenium.WebDriverException launchFailure) {
            // Driver errors may embed the URL/token; never attach that raw exception.
            throw new AssertionError("Could not open the marketing link; check Appium deepLink support and app-link association");
        }
        // Do not perform email/OTP login if the link lands on a manual authorization screen.
        try {
            coach.waitForAuthorizedDashboard();
        } catch (org.openqa.selenium.TimeoutException manualLoginOrOtherDestination) {
            throw new AssertionError("SIAS(M) link did not automatically open an authorized dashboard; manual login is a deviation",
                    manualLoginOrOtherDestination);
        }
        coach.openProfile();
        // Search rendered profile text without including the expected email in a locator/error.
        String textNodes = Platform.getInstance().isIOS()
                ? "//XCUIElementTypeStaticText" : "//android.widget.TextView";
        try {
            coach.createWait(10).withMessage("Marketing link logged into an unexpected profile").until(d ->
                    driver.findElements(org.openqa.selenium.By.xpath(textNodes)).stream().anyMatch(node ->
                            node.isDisplayed() && email.equalsIgnoreCase(node.getText().trim())));
        } catch (org.openqa.selenium.TimeoutException wrongAccount) {
            throw new AssertionError("Marketing link logged into an unexpected profile");
        }
    }

    private DailyPlanPageObject openRegularPlayer(KegelCasePageObject ui, String key) {
        DailyPlanPageObject daily = openWorkout(ui, key, false);
        ui.startRegularPlayer();
        return daily;
    }

    private DailyPlanPageObject openWorkout(KegelCasePageObject ui, String key, boolean custom) {
        requireMobilePlatform();
        preparedUi = ui;
        TestData.TestAccount account = TestData.testModelAccount(key);
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        DailyPlanPageObject daily = DailyPlanPageObjectFactory.get(driver);
        ui.closeCustomWorkoutIfOpen();
        daily.closeKegelExerciseFlowIfPresent();
        ui.loginExistingAccount(coach, account.getEmail(), account.getOtp());
        daily.openTodayTab();
        preparedUi = ui;
        ui.prepareProgram(daily, custom);
        ui.openWorkout(custom);
        return daily;
    }

    private String secret(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException("Required test data missing: " + name);
        return value.trim();
    }

    @Override
    protected void cleanupSessionForNextTest() {
        if (preparedUi != null) {
            preparedUi.restoreProgram();
            if (preparedUi.usesProfileFreeAndroidSession()) return;
            if (driver != null && Platform.getInstance().isIOS() && preparedUi.usesQuizProfileLogin()
                    && ("logout".equals(TestData.testIsolationMode()) || "reinstall".equals(TestData.testIsolationMode()))) {
                preparedUi.ensureLoggedOutOnObservedStart(CoachFlowPageObjectFactory.get(driver));
                return;
            }
        }
        // Core cleanup is iOS-only; these cases also isolate Android runs through UI logout.
        if (driver != null && Platform.getInstance().isAndroid()) {
            CoachFlowPageObjectFactory.get(driver).ensureLoggedOutOnStartScreen();
        } else {
            super.cleanupSessionForNextTest();
        }
    }
}
