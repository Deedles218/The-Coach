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
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.ExplorePageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import lib.ui.factories.ExplorePageObjectFactory;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Ready automation for the reviewed Xray cases COA-7947, COA-8235 and COA-8517.
 *
 * The test names deliberately keep the Jira keys so an Allure/Jira handoff
 * remains traceable without relying on a Jira API integration. Daily Plan
 * mutation scenarios use a separately provisioned, resettable test fixture.
 */
@Epic(value = "The Coach reviewed test-model automation")
public class TestModelAutomationTests extends CoreTestCase {

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


}
