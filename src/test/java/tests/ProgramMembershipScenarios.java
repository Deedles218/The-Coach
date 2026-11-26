package tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import lib.CoreTestCase;
import lib.MobileCaseFixture;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.ProgramSelectionApi;
import lib.ProgramSelectionState;
import lib.SimulatorProgramContent;
import lib.ui.ProgramSwitchFeaturePageObject;
import lib.ui.ios.IOSMyProgramsPageObject;
import lib.ui.ios.IOSProgramCatalogPageObject;
import lib.ui.ios.IOSProgramSelectionPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;

@Epic("COA-9583 Program activation")
@Feature("Saved programs and independent progress")
public class ProgramMembershipScenarios extends CoreTestCase {
    private IOSMyProgramsPageObject today;

    @Before @Override public void setUp() throws Exception {
        requireIOSPlatform();
        Assert.assertTrue("Use an installed preprod build with preserved data",
                Platform.getInstance().isTestFlightAttachConfigured()
                        && Platform.getInstance().getIOSBundleId().startsWith("com.vamapps.preprod."));
        PreparedMobileProfile.assertPreservationAllowed();
        super.setUp();
        today = new IOSMyProgramsPageObject(driver);
    }

    @Override protected void cleanupSessionForNextTest() {
        // Each mutating scenario owns its prepared QA fixture. Do not rearrange roles or progress in cleanup.
    }

    private MobileCaseFixture prepare(String key) throws Exception {
        MobileCaseFixture f = MobileCaseFixture.load(key);
        ProgramSelectionApi.assertDedicatedProfile(key);
        new IOSProgramSelectionPageObject(driver).closeOptionalStartupPrompts();
        return f;
    }

    @Test @Issue("COA-9793")
    public void switchingActiveProgramPreservesMainSecondaryAndDistinguishableDailyPlans() throws Exception {
        MobileCaseFixture f = prepare("COA-9793");
        ProgramSelectionState before = ProgramSelectionApi.read();
        before.assertRoles(f.required("main.id"), ProgramOnboardingScenarios.ids(f, "secondary.ids"));
        Assert.assertEquals("Selector fixture needs B and C", 2, before.secondary().size());
        before.assertActive(before.mainId());
        String secondary = f.required("switch.id");
        Assert.assertTrue("Switch destination is not Secondary", before.secondaryIds().contains(secondary));
        Assert.assertNotEquals("Fixture plans must contain distinguishable content", f.required("main.content"), f.required("switch.content"));
        today.openTodayTab();
        assertDaily(f, "main", (String) before.main().get("title"));
        today.openProgramSelector();
        today.assertMatchesState(before);
        today.selectSavedProgram((String) before.program(secondary).get("title"), bool(f, "confirmation"));
        assertDaily(f, "switch", (String) before.program(secondary).get("title"));
        ProgramSelectionState switched = ProgramSelectionApi.read();
        switched.assertActive(secondary);
        switched.assertSameRoles(before);
        switched.assertSameProgress(before);
        today.openProgramSelector();
        today.assertMatchesState(switched);
        today.selectSavedProgram((String) before.main().get("title"), false);
        assertDaily(f, "main", (String) before.main().get("title"));
        ProgramSelectionState returned = ProgramSelectionApi.read();
        returned.assertExactlyEquals(before);
        today.openProgramSelector();
        today.assertMatchesState(returned);
        today.closeProgramSelector();
    }

    @Test @Issue("COA-9793")
    public void mainOnlyProfileHasNoUnaddedRowsAndCanOpenTheCatalog() throws Exception {
        MobileCaseFixture f = prepare("COA-9793");
        ProgramSelectionState before = ProgramSelectionApi.read();
        before.assertRoles(f.required("main.id"), Collections.emptyList());
        before.assertActive(before.mainId());
        today.openTodayTab();
        today.openProgramSelector();
        today.assertMatchesState(before);
        today.openCatalogFromSelector();
        today.returnToToday();
        ProgramSelectionApi.read().assertExactlyEquals(before);
    }

    @Test @Issue("COA-9794")
    public void completingSecondaryContentChangesOnlyItsOwnProgress() throws Exception {
        MobileCaseFixture f = prepare("COA-9794");
        String item = f.required("switch.item.title");
        Map<String, String> answers = preparedItemAnswers(f, "switch.item");
        ProgramSelectionState before = ProgramSelectionApi.read();
        String main = f.required("main.id"), secondary = f.required("switch.id");
        before.assertRoles(main, ProgramOnboardingScenarios.ids(f, "secondary.ids"));
        before.assertActive(main);
        Assert.assertTrue("Progress fixture needs a saved Secondary", before.secondaryIds().contains(secondary));
        assertProgress(before, main, f, "main.before");
        assertProgress(before, secondary, f, "switch.before");
        Assert.assertTrue("Both programs must have nonzero progress", number(before.program(main).get("progress_percent")) > 0
                && number(before.program(secondary).get("progress_percent")) > 0);
        Assert.assertNotEquals("Prepared programs need distinguishable progress percentages",
                number(before.program(main).get("progress_percent")), number(before.program(secondary).get("progress_percent")), .01);
        today.openTodayTab();
        assertDaily(f, "main", (String) before.program(main).get("title"));
        before.assertDisplayedPercent(main, today.getProgramProgressValue());
        today.waitForElementVisible(f.selector("main.items.state"), "Prepared Main item state differs", 10);
        today.openProgramSelector();
        today.assertMatchesState(before);
        today.selectSavedProgram((String) before.program(secondary).get("title"), bool(f, "confirmation"));
        assertDaily(f, "switch", (String) before.program(secondary).get("title"));
        before.assertDisplayedPercent(secondary, today.getProgramProgressValue());
        today.waitForElementVisible(f.selector("switch.item.unfinished"), "Prepared Secondary item is not unfinished", 10);
        ProgramSelectionState switched = ProgramSelectionApi.read();
        switched.assertSameRoles(before);
        switched.assertSameProgress(before);
        new ProgramSwitchFeaturePageObject(driver).completeDailyPlanItemWithAnswers(item, answers);
        ProgramSelectionState completed = ProgramSelectionApi.read();
        completed.assertSameRoles(before);
        completed.assertActive(secondary);
        Assert.assertEquals("Completing Secondary changed Main progress", before.progress(main), completed.progress(main));
        Assert.assertNotEquals("Completing the prepared item did not change Secondary progress", before.progress(secondary), completed.progress(secondary));
        assertProgress(completed, secondary, f, "switch.after");
        assertDaily(f, "switch.after", (String) before.program(secondary).get("title"));
        completed.assertDisplayedPercent(secondary, today.getProgramProgressValue());
        today.waitForElementVisible(f.selector("switch.item.completed"), "Secondary completion is not shown", 10);
        today.openProgramSelector();
        today.assertMatchesState(completed);
        today.selectSavedProgram((String) before.program(main).get("title"), false);
        assertDaily(f, "main", (String) before.program(main).get("title"));
        completed.assertDisplayedPercent(main, today.getProgramProgressValue());
        today.waitForElementVisible(f.selector("main.items.state"), "Main item state changed after completing Secondary", 10);
        today.openProgramSelector();
        today.assertMatchesState(ProgramSelectionApi.read());
        today.selectSavedProgram((String) before.program(secondary).get("title"), false);
        assertDaily(f, "switch.after", (String) before.program(secondary).get("title"));
        completed.assertDisplayedPercent(secondary, today.getProgramProgressValue());
        today.waitForElementVisible(f.selector("switch.item.completed"), "Secondary completion was reset after switching", 10);
        ProgramSelectionApi.read().assertExactlyEquals(completed);
    }

    @Test @Issue("COA-9797")
    public void exploreAddsOneSecondaryAndActivatesItsDailyPlan() throws Exception { addFromExplore(); }

    @Test @Issue("COA-9797")
    public void selectorAddAddsOneSecondaryAndReturnsToExpandedSelector() throws Exception { addSelectedFromSelector(true); }

    @Test @Issue("COA-9797")
    public void firstValidActivationWithoutMainCreatesMainAndActiveWithNoSecondary() throws Exception {
        MobileCaseFixture f = prepare("COA-9797");
        String id = f.required("first.id");
        ProgramSelectionState after = ProgramSelectionApi.activateFirstProgram(id);
        after.assertRoles(id, Collections.emptyList());
        after.assertActive(id);
    }

    private void addFromExplore() throws Exception {
        MobileCaseFixture f = prepare("COA-9797");
        ProgramSelectionState before = ProgramSelectionApi.read();
        before.assertRoles(f.required("main.id"), ProgramOnboardingScenarios.ids(f, "secondary.ids"));
        Assert.assertFalse("Activation fixture must already have a Secondary", before.secondary().isEmpty());
        String added = f.required("added.id");
        Assert.assertEquals("Activation target is not unadded", "none",
                ((Map<?, ?>) before.raw().get("catalog_membership")).get(added));
        String title = ProgramOnboardingScenarios.title(ProgramSelectionApi.catalog(), added);
        IOSProgramCatalogPageObject catalog = new IOSProgramCatalogPageObject(driver);
        today.openTodayTab();
        catalog.openUnaddedFromExplore(title);
        long action = System.currentTimeMillis();
        catalog.addAndStart(title);
        Map<String, Object> exchange = ProgramSelectionApi.exchange(added + "/add_and_activate/", action);
        Assert.assertNull("The program ID must be in the activation URL, with no request body", exchange.get("request"));
        ProgramSelectionState after = ProgramSelectionApi.read();
        List<String> expected = new ArrayList<>(before.secondaryIds());
        expected.add(added);
        Map<String, Object> response = (Map<String, Object>) exchange.get("response");
        Assert.assertEquals("Activation response changed Main Journey", before.mainId(), response.get("main_journey_program_id"));
        Assert.assertEquals("Activation response has the wrong Active Program", added, response.get("active_program_id"));
        List<String> savedIds = (List<String>) response.get("secondary_program_ids");
        Assert.assertEquals("Activation response contains duplicate Secondary IDs", expected.size(), savedIds.size());
        Assert.assertEquals("Activation response lost or changed Secondary", new LinkedHashSet<>(expected), new LinkedHashSet<>(savedIds));
        after.assertRoles(before.mainId(), expected);
        after.assertActive(added);
        after.assertSameProgress(before);
        Assert.assertEquals("Activated catalog membership is wrong", "secondary",
                ((Map<?, ?>) after.raw().get("catalog_membership")).get(added));
        today.assertCurrentDailyContent(SimulatorProgramContent.readProgramSelection("COA-9797", added, action), added);
        today.openProgramSelector();
        today.assertMatchesState(after);
        today.closeProgramSelector();
    }

    @Test @Issue("COA-9797")
    public void allProgramsAddsTwoAtOnceAndReturnsToExpandedSelector() throws Exception {
        addSelectedFromSelector(false);
    }

    private void addSelectedFromSelector(boolean viaAddProgram) throws Exception {
        MobileCaseFixture f = prepare("COA-9797");
        ProgramSelectionState before = ProgramSelectionApi.read();
        before.assertRoles(f.required("main.id"), ProgramOnboardingScenarios.ids(f, "secondary.ids"));
        List<String> additions = viaAddProgram ? Collections.singletonList(f.required("added.id"))
                : ProgramOnboardingScenarios.ids(f, "added.ids");
        Assert.assertEquals("Fixture has the wrong number of unadded programs", viaAddProgram ? 1 : 2, additions.size());
        for (String id : additions) Assert.assertEquals("Target must be unadded", "none",
                ((Map<?, ?>) before.raw().get("catalog_membership")).get(id));
        List<Map<String, Object>> programs = ProgramSelectionApi.catalog();
        IOSProgramCatalogPageObject catalog = new IOSProgramCatalogPageObject(driver);
        today.openTodayTab();
        today.openProgramSelector();
        if (viaAddProgram) today.openCatalogFromAddProgram();
        else today.openCatalogFromSelector();
        catalog.assertMembershipAndOrder(programs, before);
        catalog.selectForAddition(ProgramOnboardingScenarios.titles(programs, additions), before);
        ProgramSelectionApi.read().assertExactlyEquals(before);
        long action = System.currentTimeMillis();
        catalog.addSelectedAndReturnToSelector(additions.size());
        List<String> expected = new ArrayList<>(before.secondaryIds());
        expected.addAll(additions);
        Map<String, Object> request = (Map<String, Object>) ProgramSelectionApi.exchange("secondary_programs/", action).get("request");
        List<String> sent = (List<String>) request.get("program_ids");
        Assert.assertEquals("Multiselect request contains duplicates", expected.size(), sent.size());
        Assert.assertEquals("Multiselect request lost saved programs", new LinkedHashSet<>(expected), new LinkedHashSet<>(sent));
        ProgramSelectionState after = ProgramSelectionApi.read();
        after.assertRoles(before.mainId(), expected);
        after.assertActive(before.active());
        after.assertSameProgress(before);
        Assert.assertEquals("Adding programs changed completed content", before.raw().get("achievements"), after.raw().get("achievements"));
        today.assertMatchesState(after);
        today.closeProgramSelector();
    }

    @Test @Issue("COA-9793")
    public void everySavedCounterUsesTheSameUnitAndPositionAsItsOwnDailyPlan() throws Exception {
        MobileCaseFixture f = prepare("COA-9793");
        ProgramSelectionState before = ProgramSelectionApi.read();
        before.assertRoles(f.required("main.id"), ProgramOnboardingScenarios.ids(f, "secondary.ids"));
        before.assertActive(before.mainId());
        today.openTodayTab();
        boolean firstSwitch = true;
        for (Map<String, Object> program : before.programs()) {
            String title = (String) program.get("title");
            if (!title.equals(today.getActiveProgramName())) {
                today.openProgramSelector();
                today.selectSavedProgram(title, firstSwitch && bool(f, "confirmation"));
                firstSwitch = false;
            }
            today.openProgramSelector();
            today.assertMatchesState(ProgramSelectionApi.read());
            today.closeProgramSelector();
        }
        if (!before.main().get("title").equals(today.getActiveProgramName())) {
            today.openProgramSelector();
            today.selectSavedProgram((String) before.main().get("title"), false);
        }
        ProgramSelectionApi.read().assertExactlyEquals(before);
    }

    @Test @Issue("COA-9802")
    public void selectorTutorialAndFirstSwitchFollowTheReviewedOnPolicy() throws Exception {
        MobileCaseFixture f = prepare("COA-9802");
        String trigger = f.required("tooltip.trigger");
        Assert.assertTrue("Review the ON trigger before running this case", Arrays.asList("first-entry", "after-first-item").contains(trigger));
        boolean confirmation = bool(f, "confirmation"), secondTooltip = bool(f, "tooltip.second");
        String firstItem = f.required("first.item.title");
        Map<String, String> firstItemAnswers = preparedItemAnswers(f, "first.item");
        String firstAnchor = f.selector("tooltip.first.anchor");
        String secondAnchor = secondTooltip ? f.selector("tooltip.second.anchor") : null;
        ProgramSelectionState before = ProgramSelectionApi.read();
        String mainId = f.required("main.id"), secondaryId = f.required("switch.id");
        before.assertRoles(mainId, Collections.singletonList(secondaryId));
        String main = (String) before.main().get("title"), secondary = (String) before.program(secondaryId).get("title");
        IOSProgramSelectionPageObject selection = new IOSProgramSelectionPageObject(driver);
        selection.start(main);
        selection.finishFirstVisitIntroduction();
        selection.waitForFirstDailyPlan(main);
        today.waitForElementVisible("id:Today", "First Daily Plan navigation did not appear", 30);
        ProgramSwitchFeaturePageObject tips = new ProgramSwitchFeaturePageObject(driver);
        if ("first-entry".equals(trigger)) {
            tips.waitForFirstTooltip();
            today.assertTooltipAtSelector("Tap to explore more programs", firstAnchor);
            tips.dismissFirstTooltip();
            if (today.isElementVisible("id:My programs")) today.closeProgramSelector();
            tips.completeDailyPlanItemWithAnswers(firstItem, firstItemAnswers);
            tips.assertFirstTooltipAbsent();
        } else {
            tips.assertFirstTooltipAbsent();
            tips.completeDailyPlanItemWithAnswers(firstItem, firstItemAnswers);
            tips.waitForFirstTooltip();
            today.assertTooltipAtSelector("Tap to explore more programs", firstAnchor);
            tips.dismissFirstTooltip();
            if (today.isElementVisible("id:My programs")) today.closeProgramSelector();
        }
        today.openTodayTab();
        tips.assertTooltipsAbsent();
        closeAndReopenCoachApplication();
        selection.closeOptionalStartupPrompts();
        today.openTodayTab();
        tips.assertTooltipsAbsent();
        if (confirmation) {
            ProgramSelectionState beforeCancel = ProgramSelectionApi.read();
            String dayBeforeCancel = today.getCurrentDayLabel();
            String contentBeforeCancel = today.getFirstDailyContentTitle();
            today.openProgramSelector();
            today.waitForElementAndClick("id:" + secondary, "Secondary is absent", 10);
            tips.waitForConfirmation();
            tips.cancelConfirmation();
            if (today.isElementVisible("id:My programs")) today.closeProgramSelector();
            Assert.assertEquals("Cancel changed Active Program", main, today.getActiveProgramName());
            Assert.assertEquals("Cancel changed the Main Daily Plan stage", dayBeforeCancel, today.getCurrentDayLabel());
            Assert.assertEquals("Cancel changed the Main Daily Plan content", contentBeforeCancel, today.getFirstDailyContentTitle());
            ProgramSelectionApi.read().assertExactlyEquals(beforeCancel);
        }
        today.openProgramSelector();
        today.chooseSavedProgram(secondary, confirmation);
        if (secondTooltip) {
            tips.waitForSecondTooltip();
            today.assertTooltipAtSelector("To switch back, tap here", secondAnchor);
            tips.dismissSecondTooltip();
            if (today.isElementVisible("id:My programs")) today.closeProgramSelector();
        } else tips.assertTooltipsAbsent();
        Assert.assertEquals("First switch opened the wrong Daily Plan", secondary, today.getActiveProgramName());
        ProgramSelectionApi.read().assertActive(secondaryId);
        for (String title : Arrays.asList(main, secondary)) {
            today.openProgramSelector();
            today.chooseSavedProgram(title, false);
            // Tutorial assertions must observe overlays, never consume them in
            // the normal switching helper before checking for a repeat.
            tips.assertConfirmationAbsent();
            tips.assertTooltipsAbsent();
            today.waitForElementVisible("id:" + title, "Repeated switch did not expose the target Daily Plan", 20);
            Assert.assertEquals("Repeated switch opened the wrong Daily Plan", title, today.getActiveProgramName());
            tips.assertConfirmationAbsent();
            tips.assertTooltipsAbsent();
        }
        ProgramSelectionApi.read().assertSameRoles(before);
    }

    private Map<String, String> preparedItemAnswers(MobileCaseFixture f, String prefix) {
        String question = f.optional(prefix + ".question");
        Assert.assertEquals("Prepare both the Daily Plan question and its answer",
                question != null, f.optional(prefix + ".answer") != null);
        return question == null ? Collections.emptyMap()
                : Collections.singletonMap(question, f.required(prefix + ".answer"));
    }

    private void assertDaily(MobileCaseFixture f, String prefix, String title) {
        Assert.assertEquals("Wrong Daily Plan", title, today.getActiveProgramName());
        Assert.assertEquals("Wrong saved Daily Plan day", f.required(prefix + ".day"), today.getCurrentDayLabel());
        Assert.assertEquals("Wrong Daily Plan content", f.required(prefix + ".content"), today.getFirstDailyContentTitle());
    }

    static boolean bool(MobileCaseFixture f, String key) {
        String value = f.required(key);
        Assert.assertTrue("Reviewed true/false is required for " + key, "true".equals(value) || "false".equals(value));
        return Boolean.parseBoolean(value);
    }

    private void assertProgress(ProgramSelectionState state, String id, MobileCaseFixture f, String prefix) {
        Map<String, Object> row = state.program(id);
        Assert.assertEquals("Wrong saved module", f.required(prefix + ".module"), row.get("current_module_name"));
        Assert.assertEquals("Wrong saved stage", Double.parseDouble(f.required(prefix + ".stage")), number(row.get("current_stage")), .00001);
        Assert.assertEquals("Wrong saved percent", Double.parseDouble(f.required(prefix + ".percent")), number(row.get("progress_percent")), .00001);
    }

    private double number(Object value) {
        Assert.assertTrue("Progress must be numeric", value instanceof Number);
        return ((Number) value).doubleValue();
    }
}
