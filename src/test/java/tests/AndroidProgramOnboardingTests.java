package tests;

import io.qameta.allure.*;
import lib.CoreTestCase;
import lib.Platform;
import lib.ui.ProgramSwitchFeaturePageObject;
import lib.ui.android.AndroidMyProgramsPageObject;
import lib.ui.android.AndroidOnboardingPageObject;
import lib.ui.android.AndroidProgramSelectionPageObject;
import org.junit.*;
import java.util.*;
import java.util.stream.Collectors;

/** One fresh anonymous profile per case, on the explicitly supplied preprod APK. No purchases. */
@Epic("COA-9583 Program activation")
@Feature("Onboarding UI v3 Android")
public class AndroidProgramOnboardingTests extends CoreTestCase {
    private AndroidProgramSelectionPageObject selection;
    private AndroidMyProgramsPageObject today;
    private String main;

    @Before @Override public void setUp() throws Exception {
        Assert.assertTrue("Run on Android", Platform.getInstance().isAndroid());
        Assert.assertTrue("Fresh v3 scenarios require the supplied APK, fullReset=true, noReset=false",
                Platform.getInstance().isFreshAndroidInstallConfigured());
        super.setUp();
        selection = new AndroidProgramSelectionPageObject(driver);
        today = new AndroidMyProgramsPageObject(driver);
        AndroidOnboardingPageObject onboarding = new AndroidOnboardingPageObject(driver);
        onboarding.completeNewUserJourneyToPaywall(Platform.getInstance().getOnboardingGoal());
        onboarding.closePaywallsAndPopupsUntil(() -> selection.isElementVisible(
                "xpath://*[@text=" + org.openqa.selenium.support.ui.Quotes.escape(AndroidProgramSelectionPageObject.FIRST_INTERSTITIAL) + "]"));
        main = selection.showMyJourney();
        Assert.assertEquals("Recommendation differs from the configured goal dataset",
                Objects.requireNonNull(System.getProperty("coach.programOnboarding.expectedMain"),
                        "Configure coach.programOnboarding.expectedMain for the supplied goal"), main);
    }

    @Override protected void cleanupSessionForNextTest() { /* Full reset owns isolation; do not complete a failed draft. */ }

    @Test @Issues({@Issue("COA-9787"), @Issue("COA-9788"), @Issue("COA-9789")})
    public void interstitialsAndMainReplacementUseTheSavedJourney() {
        selection.assertRecommendationActionsPinned();
        selection.openMainPicker(); selection.assertCurrentMainFirst(main);
        String alternative = selection.cardTitles().stream().filter(t -> !main.equals(t)).findFirst().get();
        selection.chooseMain(alternative); selection.goBackFromMainPicker();
        selection.openMainPicker(); selection.assertCurrentMainFirst(main);
        selection.chooseMain(alternative); selection.confirmMain(alternative);
        selection.assertContinueCount(0);
        List<String> available = selection.cardTitles();
        selection.openSummary(); selection.assertSummary(alternative, Collections.emptyList(), available);
        assertDailyPlanAndColdLaunch(alternative, Collections.emptyList());
    }

    @Test @Issue("COA-9790")
    public void zeroSecondaryUsesSkipWhileContinueIsDisabled() {
        selection.acceptRecommendation(main);
        List<String> available = selection.cardTitles();
        selection.selectOnly(Collections.emptyList());
        selection.openSummary();
        selection.assertSummary(main, Collections.emptyList(), available);
        assertDailyPlanAndColdLaunch(main, Collections.emptyList());
    }

    @Test @Issues({@Issue("COA-9790"), @Issue("COA-9792")})
    public void previewOfAnotherProgramPreservesSelectionWhenItsActionRequiresScrolling() {
        selection.acceptRecommendation(main);
        List<String> available = selection.cardTitles();
        Assert.assertTrue("Prepare at least three Secondary choices", available.size() >= 3);
        List<String> chosen = Collections.singletonList(available.get(0));
        selection.selectOnly(chosen);
        selection.openDetailsAndReturn(available.get(available.size() - 1));
        selection.assertSelection(chosen);
        selection.openSummary(); selection.assertSummary(main, chosen, available);
        assertDailyPlanAndColdLaunch(main, chosen);
    }

    @Test @Issues({@Issue("COA-9790"), @Issue("COA-9791"), @Issue("COA-9792"), @Issue("COA-9803")})
    public void secondaryCounterPreviewAndSummaryPreserveTheChosenSet() {
        selection.acceptRecommendation(main);
        List<String> available = selection.cardTitles();
        Assert.assertTrue("Two Secondary programs are required", available.size() >= 2);
        List<String> chosen = new ArrayList<>(available.subList(0, 2));
        selection.selectOnly(chosen); selection.toggle(chosen.get(1)); selection.assertContinueCount(1);
        selection.toggle(chosen.get(1)); selection.assertContinueCount(2);
        selection.openDetailsAndReturn(chosen.get(0)); selection.assertSelection(chosen);
        selection.openSummary(); selection.assertSummary(main, chosen, available);
        selection.startDailyPlan(main);
        today.openProgramSelector(); today.assertPrograms(main, chosen, main); today.closeProgramSelector();
        closeAndReopenCoachApplication(); selection.closeStartupPrompts();
        today.openProgramSelector(); today.assertPrograms(main, chosen, main); today.closeProgramSelector();
    }

    @Test @Issues({@Issue("COA-9793"), @Issue("COA-9804")})
    public void myProgramsUsesEachProgramsDayOrStage() {
        List<String> secondary = completeWithOneSecondary();
        List<String> programs = new ArrayList<>(); programs.add(main); programs.addAll(secondary);
        Map<String, String> expected = new LinkedHashMap<>();
        try {
            for (String title : programs) {
                if (!title.equals(today.getActiveProgramName())) {
                    today.openProgramSelector(); today.selectSavedProgram(title, false);
                }
                expected.put(title, today.getCurrentDayLabel());
            }
            today.openProgramSelector(); today.assertPrograms(main, secondary, secondary.get(0));
            today.assertProgressLabels(expected); today.closeProgramSelector();
        } finally {
            if (selection.isElementVisible("id:" + Platform.getInstance().getAndroidAppPackage() + ":id/containerMain")) {
                today.closeProgramSelector();
            }
            if (!main.equals(today.getActiveProgramName())) {
                today.openProgramSelector(); today.selectSavedProgram(main, false);
            }
        }
    }

    @Test @Issues({@Issue("COA-9797"), @Issue("COA-9798"), @Issue("COA-9805")})
    public void allProgramsOrdersMembershipAndHidesSelectionForAddedPrograms() {
        List<String> chosen = completeWithOneSecondary();
        today.openProgramSelector(); today.openAllPrograms(); selection.assertAddCount(0);
        Set<String> added = new LinkedHashSet<>(chosen); added.add(main);
        selection.assertCatalogMembership(added);
    }

    @Test @Issue("COA-9797")
    public void allProgramsAddsTwoJourneysAndReturnsTheExpandedSelector() {
        List<String> chosen = completeWithOneSecondary();
        today.openProgramSelector(); today.openAllPrograms();
        List<String> newPrograms = selection.cardTitles().stream().filter(t -> !main.equals(t) && !chosen.contains(t))
                .limit(2).collect(Collectors.toList());
        Assert.assertEquals("Two unadded programs are required", 2, newPrograms.size());
        selection.assertAddCount(0);
        selection.toggle(newPrograms.get(0)); selection.assertAddCount(1);
        selection.toggle(newPrograms.get(1)); selection.assertAddCount(2);
        selection.addSelectedPrograms(2);
        List<String> expected = new ArrayList<>(chosen); expected.addAll(newPrograms);
        today.assertPrograms(main, expected, main); today.closeProgramSelector();
    }

    @Test @Issues({@Issue("COA-9796"), @Issue("COA-9797"), @Issue("COA-9798")})
    public void exploreAddsWithoutConfirmationAndRemovesOnlyAfterConfirmation() {
        List<String> chosen = completeWithOneSecondary();
        String added = selection.exploreTitles().stream().filter(t -> !main.equals(t) && !chosen.contains(t)).findFirst()
                .orElseThrow(() -> new AssertionError("Explore needs an unadded program"));
        selection.openExploreProgram(added); selection.assertExploreAction(false); selection.addFromExplore(added);
        List<String> expanded = new ArrayList<>(chosen); expanded.add(added);
        today.openProgramSelector(); today.assertPrograms(main, expanded, added);
        today.selectSavedProgram(main, false);
        selection.openExploreProgram(added); selection.assertExploreAction(true);
        selection.removeFromExplore(false); selection.assertExploreAction(true);
        selection.removeFromExplore(true); selection.assertExploreAction(false);
        selection.closeExploreProgram(); today.openTodayTab();
        today.openProgramSelector(); today.assertPrograms(main, chosen, main); today.closeProgramSelector();
    }

    @Test @Issue("COA-9798")
    public void exploreOrdersAddedProgramsAfterUnaddedPrograms() {
        List<String> chosen = completeWithOneSecondary();
        List<String> catalog = selection.exploreTitles();
        String added = catalog.stream().filter(t -> !main.equals(t) && !chosen.contains(t)).findFirst()
                .orElseThrow(() -> new AssertionError("Explore needs an unadded program"));
        selection.openExploreProgram(added); selection.addFromExplore(added);
        List<String> after = selection.exploreTitles();
        Set<String> members = new LinkedHashSet<>(chosen); members.add(main); members.add(added);
        boolean seenAdded = false;
        for (String title : after) {
            if (members.contains(title)) seenAdded = true;
            else Assert.assertFalse("Explore must show unadded programs before added: " + after, seenAdded);
        }
    }

    @Test @Issue("COA-9802")
    public void firstEntrySelectorTutorialDoesNotRepeatAfterColdLaunch() {
        selection.acceptRecommendation(main);
        List<String> available = selection.cardTitles(), chosen = Collections.singletonList(available.get(0));
        selection.selectOnly(chosen); selection.openSummary();
        selection.waitForElementAndClick("id:" + Platform.getInstance().getAndroidAppPackage() + ":id/btnAction",
                "Go to my daily plan is unavailable", 10);
        selection.closeStartupPrompts();
        ProgramSwitchFeaturePageObject tutorial = new ProgramSwitchFeaturePageObject(driver);
        tutorial.waitForFirstTooltip(); tutorial.dismissFirstTooltip();
        if (selection.isElementVisible("id:" + Platform.getInstance().getAndroidAppPackage() + ":id/containerMain")) today.closeProgramSelector();
        closeAndReopenCoachApplication(); selection.closeStartupPrompts(); tutorial.assertTooltipsAbsent();
    }

    private List<String> completeWithOneSecondary() {
        selection.acceptRecommendation(main);
        List<String> available = selection.cardTitles(), chosen = Collections.singletonList(available.get(0));
        selection.selectOnly(chosen); selection.openSummary(); selection.assertSummary(main, chosen, available);
        selection.startDailyPlan(main); return chosen;
    }

    private void assertDailyPlanAndColdLaunch(String expectedMain, List<String> secondary) {
        selection.startDailyPlan(expectedMain);
        today.openProgramSelector(); today.assertPrograms(expectedMain, secondary, expectedMain); today.closeProgramSelector();
        closeAndReopenCoachApplication(); selection.closeStartupPrompts();
        Assert.assertEquals("Cold launch changed the active Main", expectedMain, today.getActiveProgramName());
        today.openProgramSelector(); today.assertPrograms(expectedMain, secondary, expectedMain); today.closeProgramSelector();
    }
}
