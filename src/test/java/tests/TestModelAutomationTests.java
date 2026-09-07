package tests;

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
import lib.TestData;
import lib.TestModelFixtureSupport;
import lib.SimulatorAnalyticsEvidence;
import lib.SimulatorTestIdentity;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.ExplorePageObject;
import lib.ui.OnboardingGoal;
import lib.ui.factories.OnboardingPageObjectFactory;
import lib.ui.ios.iOSTestAccountPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import lib.ui.factories.ExplorePageObjectFactory;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.Ignore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Direct automation handoff for the eight reviewed Xray test cases.
 *
 * The test names deliberately keep the Jira keys so an Allure/Jira handoff
 * remains traceable without relying on a Jira API integration. Daily Plan
 * mutation scenarios use a separately provisioned, resettable test fixture.
 */
@Epic(value = "The Coach reviewed test-model automation")
public class TestModelAutomationTests extends CoreTestCase {
    private String anonymousMailUid;

    private iOSTestAccountPageObject prepareAnonymousMailForm(CoachFlowPageObject coachFlow) throws Exception {
        iOSTestAccountPageObject mail = new iOSTestAccountPageObject(driver);
        boolean resumed = mail.resumeAnonymousMailProfile();
        if (!resumed) {
            coachFlow.ensureLoggedOutOnStartScreen();
            OnboardingPageObjectFactory.get(driver).completeNewUserJourney(OnboardingGoal.BEAT_PREMATURE_EJACULATION);
        }
        String uid = SimulatorTestIdentity.currentUid();
        anonymousMailUid = uid;
        if (resumed) SimulatorTestIdentity.requireOwnedAnonymousMailFixture(uid);
        mail.openAnonymousEmailForm();
        if (!resumed) SimulatorTestIdentity.recordCreatedAnonymousMailFixture(uid);
        return mail;
    }

    @Override
    protected void cleanupSessionForNextTest() {
        if (anonymousMailUid != null) {
            try {
                if (anonymousMailUid.equals(SimulatorTestIdentity.currentUid())) {
                    new iOSTestAccountPageObject(driver).preserveAnonymousMailProfile();
                    return;
                }
            } catch (Exception failure) {
                throw new IllegalStateException("Cannot safely restore the anonymous mail fixture", failure);
            }
        }
        super.cleanupSessionForNextTest();
    }

    @Test
    @Issue("COA-7947")
    @Features({@Feature("Authorization"), @Feature("Email validation")})
    @DisplayName("COA-7947 Valid email enables Continue")
    @Description("Opens the mail form, enters a valid email, and verifies that Continue becomes enabled.")
    @Step("Start COA-7947")
    @Severity(SeverityLevel.CRITICAL)
    public void test01Coa7947ValidEmailEnablesContinue() {
        requireIOSForTestModelSuite();

        CoachFlowPageObject coachFlow = coachFlow();
        coachFlow.ensureLoggedOutOnStartScreen();
        coachFlow.openLoginFlow();
        coachFlow.typeLoginEmail(TestData.validEmailWithoutProgress());
        coachFlow.assertLoginContinueButtonIsEnabled();
        coachFlow.returnFromLoginFlowToStartScreen();
    }

    @Test
    @Issue("COA-7949")
    @Features({@Feature("Authorization"), @Feature("OTP")})
    @DisplayName("COA-7949 Valid email authorization into an existing account")
    @Description("Opens the anonymous profile mail form, follows Continue → Login → Send code → OTP, and verifies that the authenticated UID belongs to the existing email. Direct-OTP Welcome login cannot replace the tested flow.")
    @Step("Start COA-7949")
    @Severity(SeverityLevel.BLOCKER)
    public void test02Coa7949ValidEmailAuthorization() throws Exception {
        requireIOSForTestModelSuite();

        CoachFlowPageObject coachFlow = coachFlow();
        TestData.TestAccount account = TestData.testModelAccount("COA-7949");
        iOSTestAccountPageObject mail = prepareAnonymousMailForm(coachFlow);
        coachFlow.typeLoginEmail(account.getEmail());
        coachFlow.assertLoginContinueButtonIsEnabled();
        coachFlow.submitEmailAndOpenExistingAccountLoginStep();
        mail.assertExistingAccountPrompt(account.getEmail());
        coachFlow.openSecurityCodeRequestStep();
        coachFlow.requestSecurityCode();
        coachFlow.assertOtpScreenIsDisplayedForEmail(account.getEmail());
        coachFlow.typeSecurityCode(account.getOtp());
        coachFlow.completeAuthorizationAfterSecurityCode();
        coachFlow.assertAuthorizedDashboardIsDisplayed();
        Assert.assertNotEquals("Authorization left the anonymous account active",
                anonymousMailUid, SimulatorTestIdentity.currentUid());
        new TestModelFixtureSupport(driver, "COA-7949").verifyExistingAccount();
    }

    @Test
    @Issue("COA-7950")
    @Features({@Feature("Authorization"), @Feature("Email validation")})
    @DisplayName("COA-7950 Invalid email shows validation error and keeps Continue disabled")
    @Description("Enters an invalid email, verifies the validation error and confirms that Continue remains disabled.")
    @Step("Start COA-7950")
    @Severity(SeverityLevel.CRITICAL)
    public void test03Coa7950InvalidEmailIsRejected() throws Exception {
        requireIOSForTestModelSuite();

        CoachFlowPageObject coachFlow = coachFlow();
        iOSTestAccountPageObject mail = prepareAnonymousMailForm(coachFlow);
        coachFlow.typeLoginEmail(TestData.invalidEmail());
        mail.finishMailEditing();
        coachFlow.assertLoginContinueButtonIsDisabled();
        mail.assertDisabledMailContinueIsNoOp();
        coachFlow.assertLoginValidationErrorIsDisplayed();
        new iOSTestAccountPageObject(driver).preserveAnonymousMailProfile();
    }

    @Test
    @Issue("COA-8235")
    @Features({@Feature("Daily Plan"), @Feature("Explore"), @Feature("Program selector")})
    @DisplayName("COA-8235 Daily Plan program list matches Explore")
    @Description("Compares the Daily Plan program selector with Explore in the current A/B variant and verifies that Retain is absent from both lists.")
    @Step("Start COA-8235")
    @Severity(SeverityLevel.NORMAL)
    public void test04Coa8235DailyPlanProgramsMatchExplore() {
        requireIOSForTestModelSuite();

        CoachFlowPageObject coachFlow = coachFlow();
        DailyPlanPageObject dailyPlan = dailyPlan();
        ExplorePageObject explore = explore();
        ensureTestModelUser(coachFlow, "COA-8235");

        dailyPlan.openTodayTab();
        dailyPlan.openProgramSelector();
        List<String> selectorPrograms = dailyPlan.getProgramNamesFromSelector();
        dailyPlan.closeProgramSelector();

        explore.openExploreTab();
        List<String> explorePrograms = explore.getProgramNamesFromExplore();
        assertProgramListsMatch(selectorPrograms, explorePrograms);
    }

    @Test
    @Issue("COA-8511")
    @Features({@Feature("Daily Plan customization"), @Feature("Catch-up")})
    @DisplayName("COA-8511 Daily Plan card can be postponed repeatedly day by day")
    @Description("Long-presses the prepared TO CATCH-UP card, moves it to tomorrow, verifies the postponed clock state, and finds it in TO CATCH-UP on the target day.")
    @Step("Start COA-8511")
    @Severity(SeverityLevel.CRITICAL)
    public void test05Coa8511RepeatedPostponeMovesCardToTargetDay() throws Exception {
        runKegelCustomization("COA-8511");
    }

    @Test
    @Issue("COA-8512")
    @Features({@Feature("Daily Plan customization"), @Feature("Program progress")})
    @DisplayName("COA-8512 Postponing the only task does not change program progress")
    @Description("Moves the only remaining task and verifies that the observed program progress is unchanged after postponement and application restart (user-approved module oracle).")
    @Step("Start COA-8512")
    @Severity(SeverityLevel.CRITICAL)
    public void test06Coa8512PostponeDoesNotChangeProgressAfterRestart() throws Exception {
        runKegelCustomization("COA-8512");
    }

    private void runKegelCustomization(String key) throws Exception {
        requireIOSForTestModelSuite();
        CoachFlowPageObject coachFlow = coachFlow();
        ensureTestModelUser(coachFlow, key);
        TestModelFixtureSupport fixture = new TestModelFixtureSupport(driver, key);
        fixture.kegel("approve-kegel");
        lib.ui.ios.iOSProgramSelectorPageObject selector = new lib.ui.ios.iOSProgramSelectorPageObject(driver);
        lib.ui.ios.iOSKegelCustomizationPageObject daily = new lib.ui.ios.iOSKegelCustomizationPageObject(driver);
        selector.openTodayTab();
        if ("Kegel Challenge".equals(selector.getActiveProgramName())) {
            selector.openProgramSelector();
            selector.selectCustomizationInspectionProgram("Last Longer: Retain");
            selector.waitForSelectedProgram("Last Longer: Retain");
        }
        String original = selector.getActiveProgramName();
        String originalDay = selector.getCurrentDayLabel();
        String originalProgress = selector.getProgramProgressValue();
        Assert.assertEquals("Unexpected shared-account baseline; no reset performed", "Last Longer: Retain", original);
        lib.RestoringTestAction.run(() -> {
            try {
                selector.openProgramSelector();
                selector.selectCustomizationInspectionProgram("Kegel Challenge");
                selector.waitForSelectedProgram("Kegel Challenge");
                if (daily.getCurrentDayLabel().startsWith("Stage 1 ")) daily.openNextDayAndAssert("Stage 2");
                lib.ui.ios.iOSExplorePageObject explore = new lib.ui.ios.iOSExplorePageObject(driver);
                explore.openExploreTab();
                explore.openKegelProgramSettings();
                daily.resetKegelFromSettings();
                java.util.Map<String,Object> prepared = fixture.kegel("prepare-kegel");
                reopenCustomizationToday(coachFlow, daily);
                daily.assertActiveProgramMatches("Kegel Challenge");
                String sourceLabel = (String) prepared.get("sourceLabel");
                io.qameta.allure.Allure.addAttachment("Kegel stage after fixture preparation", daily.getCurrentDayLabel());
                if (daily.getCurrentDayLabel().startsWith("Stage 1 ")) daily.openNextDayAndAssert(sourceLabel);
                Assert.assertEquals("Fixture must start on the exact second stage", sourceLabel, daily.getCurrentDayLabel());
                String title = (String) prepared.get("title");
                String progressBefore = daily.getProgramProgressValue();
                if ("COA-8512".equals(key)) {
                    Assert.assertEquals("Fixture must leave exactly one pending task", Boolean.TRUE, prepared.get("singlePending"));
                    assertCoa8512ProgressMatches(progressBefore, progressBefore, "before move");
                }
                daily.postponeExactCatchUp(title);
                daily.returnToDayHeader();
                if ("COA-8512".equals(key)) {
                    String afterMove = daily.getProgramProgressValue();
                    io.qameta.allure.Allure.addAttachment("COA-8512 progress after move", progressBefore + " -> " + afterMove);
                    assertCoa8512ProgressMatches(afterMove, progressBefore, "after move");
                    reopenCustomizationToday(coachFlow, daily);
                    daily.assertActiveProgramMatches("Kegel Challenge");
                    String afterRestart = daily.getProgramProgressValue();
                    io.qameta.allure.Allure.addAttachment("COA-8512 progress after restart", progressBefore + " -> " + afterRestart);
                    assertCoa8512ProgressMatches(afterRestart, progressBefore, "after restart");
                } else {
                    daily.openNextDayAndAssert((String) prepared.get("targetLabel"));
                    daily.assertExactCatchUp(title);
                }
                fixture.kegel("verify-kegel");
                io.qameta.allure.Allure.addAttachment("Kegel business scenario verified", key + "; " + title + "; baseline " + progressBefore);
            } catch (Exception | AssertionError failure) {
                try {
                    io.qameta.allure.Allure.addAttachment("Kegel failure before restoration", "application/xml",
                            TestData.sanitizeSensitiveData(driver.getPageSource()), "xml");
                    io.qameta.allure.Allure.addAttachment("Kegel screenshot before restoration", "image/png",
                            new java.io.ByteArrayInputStream(((org.openqa.selenium.TakesScreenshot) driver)
                                    .getScreenshotAs(org.openqa.selenium.OutputType.BYTES)), "png");
                } catch (Exception artifactFailure) { failure.addSuppressed(artifactFailure); }
                throw failure;
            }
        }, () -> {
            reopenCustomizationToday(coachFlow, selector);
            if (!original.equals(selector.getActiveProgramName())) {
                selector.openProgramSelector();
                selector.selectCustomizationInspectionProgram(original);
            }
            selector.waitForSelectedProgram(original);
            Assert.assertEquals("Original program day changed", originalDay, selector.getCurrentDayLabel());
            Assert.assertEquals("Original program progress changed", originalProgress, selector.getProgramProgressValue());
            io.qameta.allure.Allure.addAttachment("Shared account program restored", original + "; " + originalDay + "; " + originalProgress);
        });
    }

    private void reopenCustomizationToday(CoachFlowPageObject coach, DailyPlanPageObject daily) {
        closeAndReopenCoachApplication();
        coach.activateAppIfPossible();
        coach.closePdfGuideUpsellIfPresent();
        coach.closeNotificationPromptIfPresent();
        coach.closeConnectEmailPromptIfPresent();
        daily.openTodayTab();
    }

    @Test
    @Issue("COA-8517")
    @Features({@Feature("Explore"), @Feature("Daily Plan customization"), @Feature("Settings")})
    @DisplayName("COA-8517 Removed exercise can be recovered from Settings")
    @Description("Opens Overall Health Settings, selects a prepared removed exercise, recovers it, and verifies that the card is present in every program day.")
    @Step("Start COA-8517")
    @Severity(SeverityLevel.CRITICAL)
    public void test07Coa8517RemovedExerciseCanBeRecovered() throws Exception {
        requireIOSForTestModelSuite();

        CoachFlowPageObject coachFlow = coachFlow();
        ExplorePageObject explore = explore();
        ensureTestModelUser(coachFlow, "COA-8517");
        TestModelFixtureSupport fixture = new TestModelFixtureSupport(driver);
        fixture.prepareRecovery();

        explore.openExploreTab();
        explore.openOverallHealthProgramSettings();
        explore.openRemovedExercises();
        explore.selectRemovedExerciseAndRecover();
        explore.assertRecoveredFixtureCardIsRendered();
        fixture.assertRecoveredOnEveryApplicableDay();
    }

    @Test
    @Ignore("Deferred by user: COA-8518 applies to Coach for Her; do not run against The Coach")
    @Issue("COA-8518")
    @Features({@Feature("Daily Plan customization"), @Feature("Tooltip")})
    @DisplayName("COA-8518 First locked next-day tap shows postpone popup")
    @Description("Taps the locked next-day arrow once on a dedicated first-day account, verifies the rendered animation and popup content, unchanged day, and fresh simulator analytics evidence.")
    @Step("Start COA-8518")
    @Severity(SeverityLevel.CRITICAL)
    public void test08Coa8518FirstLockedNextDayTapShowsPopup() throws Exception {
        requireIOSForTestModelSuite();

        CoachFlowPageObject coachFlow = coachFlow();
        DailyPlanPageObject dailyPlan = dailyPlan();
        ensureTestModelUser(coachFlow, "COA-8518");
        new TestModelFixtureSupport(driver, "COA-8518").requireCustomizationAccess();
        dailyPlan.openTodayTab();
        dailyPlan.assertActiveProgramMatches("Last Longer");
        dailyPlan.assertCurrentDayMatches("Stage 1 of 7");
        assertProgressMatches(dailyPlan.getProgramProgressValue(), "0%", "before the first locked-day tap");
        List<Throwable> failures = new ArrayList<Throwable>();
        try (SimulatorAnalyticsEvidence analytics = new SimulatorAnalyticsEvidence("COA-8518", "ToolTipPostponeActivity shown")) {
            try {
                dailyPlan.assertLockedNextDayPopupOnFirstTap();
            } catch (Exception | AssertionError error) {
                failures.add(error);
            }
            try {
                analytics.assertObserved();
            } catch (Exception | AssertionError error) {
                failures.add(error);
            }
        }
        if (!failures.isEmpty()) {
            AssertionError combined = new AssertionError("COA-8518 popup/analytics checks failed; see suppressed causes and attachments");
            for (Throwable failure : failures) combined.addSuppressed(failure);
            throw combined;
        }
    }

    private CoachFlowPageObject coachFlow() {
        CoachFlowPageObject coachFlow = CoachFlowPageObjectFactory.get(driver);
        Assert.assertNotNull("Coach page object is not available for current platform", coachFlow);
        return coachFlow;
    }

    private void requireIOSForTestModelSuite() {
        Assume.assumeTrue(
                "The reviewed test-model suite is implemented for iOS; run it with -Dplatform=ios",
                Platform.getInstance().isIOS()
        );
    }

    private DailyPlanPageObject dailyPlan() {
        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        Assert.assertNotNull("Daily Plan page object is not available for current platform", dailyPlan);
        return dailyPlan;
    }

    private ExplorePageObject explore() {
        ExplorePageObject explore = ExplorePageObjectFactory.get(driver);
        Assert.assertNotNull("Explore page object is not available for current platform", explore);
        return explore;
    }

    private void ensureTestModelUser(CoachFlowPageObject coachFlow, String caseKey) {
        TestData.TestAccount account = TestData.testModelAccount(caseKey);
        coachFlow.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
    }

    @Step("Compare program lists from the Daily Plan selector and Explore")
    public static void assertProgramListsMatch(List<String> selectorPrograms, List<String> explorePrograms) {
        List<String> normalizedSelectorPrograms = normalizeProgramList(selectorPrograms, "Daily Plan selector");
        List<String> normalizedExplorePrograms = normalizeProgramList(explorePrograms, "Explore");

        Assert.assertFalse(
                "Retain must not be present in the Daily Plan selector",
                normalizedSelectorPrograms.contains("RETAIN")
        );
        Assert.assertFalse(
                "Retain must not be present in Explore",
                normalizedExplorePrograms.contains("RETAIN")
        );
        // COA-7646 explicitly puts the active program first in the selector.
        // COA-8235 compares membership; it does not require identical ordering.
        Assert.assertEquals("Daily Plan selector and Explore program lists differ",
                new HashSet<String>(normalizedExplorePrograms), new HashSet<String>(normalizedSelectorPrograms));
    }

    private static List<String> normalizeProgramList(List<String> names, String source) {
        Assert.assertNotNull(source + " program list must not be null", names);
        Assert.assertFalse(source + " program list must not be empty", names.isEmpty());

        List<String> normalizedInOrder = new ArrayList<String>();
        for (String name : names) {
            String normalized = canonicalProgramName(name);
            if (normalized != null && !normalized.isEmpty()) {
                normalizedInOrder.add(normalized);
            }
        }
        Assert.assertFalse(source + " contains no usable program names", normalizedInOrder.isEmpty());
        Assert.assertEquals(
                source + " contains duplicate program entries",
                normalizedInOrder.size(),
                new HashSet<String>(normalizedInOrder).size()
        );
        return normalizedInOrder;
    }

    private static String canonicalProgramName(String rawName) {
        if (rawName == null) {
            return "";
        }
        String normalized = rawName
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        List<String> knownPrograms = Arrays.asList(
                "LAST LONGER",
                "KEEP IT HARD",
                "OVERALL HEALTH",
                "SEX IS A SKILL",
                "UNHOOKED",
                "KEGEL CHALLENGE",
                "RETAIN"
        );
        for (String knownProgram : knownPrograms) {
            if (normalized.contains(knownProgram)) {
                return knownProgram;
            }
        }
        return normalized;
    }

    /** User clarified on 2026-09-07: modules preserve the measured baseline, not literal 2%. */
    public static void assertCoa8512ProgressMatches(String actual, String baseline, String state) {
        String expected = normalizeProgress(baseline);
        String observed = normalizeProgress(actual);
        Assert.assertTrue("COA-8512 baseline must be a rendered percentage", expected.matches("(?:100|[0-9]{1,2})(?:\\.[0-9]+)?%"));
        Assert.assertTrue("COA-8512 actual progress must be a rendered percentage", observed.matches("(?:100|[0-9]{1,2})(?:\\.[0-9]+)?%"));
        Assert.assertTrue("COA-8512 baseline is outside 0..100", Double.parseDouble(expected.replace("%", "")) <= 100);
        Assert.assertEquals("Program progress changed in COA-8512 " + state, expected, observed);
    }

    private static void assertProgressMatches(String actual, String expected, String state) {
        String normalizedActual = normalizeProgress(actual);
        String normalizedExpected = normalizeProgress(expected);
        Assert.assertEquals("Program progress changed " + state, normalizedExpected, normalizedActual);
    }

    private static String normalizeProgress(String progress) {
        return progress == null
                ? ""
                : progress.replaceAll("[\\s\\p{Z}]+", "").toUpperCase(Locale.ROOT);
    }

}
