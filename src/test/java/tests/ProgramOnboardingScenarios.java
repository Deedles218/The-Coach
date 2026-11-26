package tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import lib.CoreTestCase;
import lib.MobileCaseFixture;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.ProgramSelectionApi;
import lib.ProgramSelectionState;
import lib.SimulatorProgramContent;
import lib.ui.ProgramSwitchFeaturePageObject;
import lib.ui.ios.IOSMyProgramsPageObject;
import lib.ui.ios.IOSProgramSelectionPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Every method consumes its own reviewed, UID-bound preprod fixture. */
@Epic("COA-9583 Program activation")
@Feature("Recommendation and onboarding program roles")
public class ProgramOnboardingScenarios extends CoreTestCase {
    private IOSProgramSelectionPageObject selection;

    @Before @Override
    public void setUp() throws Exception {
        requireIOSPlatform();
        Assert.assertTrue("Use an installed preprod build with preserved data",
                Platform.getInstance().isTestFlightAttachConfigured()
                        && Platform.getInstance().getIOSBundleId().startsWith("com.vamapps.preprod."));
        PreparedMobileProfile.assertPreservationAllowed();
        super.setUp();
        selection = new IOSProgramSelectionPageObject(driver);
        selection.activatePreparedApplication();
    }

    @Override protected void cleanupSessionForNextTest() {
        // Preserve the consumed fixture and its final state; never complete or clear its draft in teardown.
    }

    private MobileCaseFixture prepare(String key) throws Exception {
        MobileCaseFixture fixture = MobileCaseFixture.load(key);
        ProgramSelectionApi.assertDedicatedProfile(key);
        return fixture;
    }

    @Test @Issue("COA-9788")
    public void recommendationMatchesPreparedAnswersAndSoundsGreatSavesItsId() throws Exception {
        MobileCaseFixture f = prepare("COA-9788");
        // Run on both independent answer-set fixtures. The expected ID is never inferred from the response.
        f.required("answers.dataset");
        ProgramSelectionApi.assertFreshOnboardingProfile();
        String id = f.required("main.id");
        Map<String, Object> recommended = ProgramSelectionApi.recommendation();
        selection.assertRecommendation(recommended, id);
        String main = (String) recommended.get("title");
        long action = System.currentTimeMillis();
        selection.acceptRecommendation(main);
        assertMainSaved(action, id);
        ProgramSelectionApi.read().assertRoles(id, Collections.emptyList());
        selection.assertSelection(main, Collections.emptyList());
        Assert.assertFalse("Sounds Great prematurely opened Summary", selection.isElementVisible("id:Your plan is ready"));
        Assert.assertFalse("Sounds Great prematurely opened Daily Plan", selection.isElementVisible("id:Today"));
    }

    @Test @Issue("COA-9789")
    public void changeProgramSavesOneAlternativeMainAndExcludesItFromSecondary() throws Exception {
        MobileCaseFixture f = prepare("COA-9789");
        ProgramSelectionApi.assertFreshOnboardingProfile();
        String original = f.required("recommended.id"), replacement = f.required("main.id");
        Assert.assertNotEquals("Alternative must differ from the recommendation", original, replacement);
        List<Map<String, Object>> catalog = ProgramSelectionApi.catalog();
        Assert.assertTrue("Change Main fixture requires at least three programs", catalog.size() >= 3);
        selection.assertRecommendation(ProgramSelectionApi.recommendation(), original);
        selection.openMainPicker();
        selection.assertMainCatalog(catalog, original);
        String main = title(catalog, replacement);
        long action = System.currentTimeMillis();
        selection.chooseMain(main);
        assertMainSaved(action, replacement);
        ProgramSelectionApi.read().assertRoles(replacement, Collections.emptyList());
        List<String> available = selection.secondaryTitles();
        Assert.assertFalse("Alternative Main is still offered as Secondary", available.contains(main));
        Assert.assertTrue("Original recommendation is unavailable as Secondary", available.contains(title(catalog, original)));
        selection.selectOnly(Collections.emptyList());
        selection.openSummary();
        selection.assertSummary(main, Collections.emptyList(), available);
    }

    @Test @Issue("COA-9791")
    public void editSecondaryReplacesTheWholeSetAndClearsItPermanently() throws Exception {
        replaceSecondaryDraft(false);
    }

    @Test @Issue("COA-9791")
    public void clearingSecondaryDraftBeforeSummaryPersistsTheEmptySet() throws Exception {
        replaceSecondaryDraft(true);
    }

    private void replaceSecondaryDraft(boolean clear) throws Exception {
        MobileCaseFixture f = prepare("COA-9791");
        String mainId = f.required("main.id"), replacement = f.required("replacement.id");
        List<String> initial = ids(f, "secondary.ids");
        Assert.assertEquals("Edit fixture needs B and C", 2, initial.size());
        Assert.assertFalse("Replacement must be initially unselected", initial.contains(replacement));
        ProgramSelectionState before = ProgramSelectionApi.read();
        Assert.assertEquals("Draft has the wrong saved Main", mainId, before.mainId());
        List<Map<String, Object>> catalog = ProgramSelectionApi.catalog();
        String main = title(catalog, mainId);
        List<String> available = secondaryTitles(catalog, mainId);
        selection.assertSelection(main, titles(catalog, initial));
        List<String> changed = clear ? Collections.emptyList() : Arrays.asList(initial.get(1), replacement);
        selection.selectOnly(titles(catalog, changed));
        ProgramSelectionApi.read().assertExactlyEquals(before);
        long action = System.currentTimeMillis();
        selection.openSummary();
        assertSecondarySaved(action, changed);
        ProgramSelectionState saved = ProgramSelectionApi.read();
        saved.assertRoles(mainId, changed);
        Assert.assertEquals("Changing the draft changed completed content", before.raw().get("achievements"), saved.raw().get("achievements"));
        selection.assertSummary(main, titles(catalog, changed), available);
        // Summary has no edit action in v3. A separate fresh fixture covers clearing.
        closeAndReopenCoachApplication();
        ProgramSelectionApi.read().assertExactlyEquals(saved);
    }

    @Test @Issues({@Issue("COA-9792"), @Issue("COA-9787")})
    public void startOpensMainDailyPlanAndPersistsExactlyTheSummaryPrograms() throws Exception {
        MobileCaseFixture f = prepare("COA-9792");
        String mainId = f.required("main.id");
        List<String> secondary = ids(f, "secondary.ids");
        Assert.assertTrue("Run both the empty and two-Secondary fixtures", secondary.isEmpty() || secondary.size() == 2);
        List<Map<String, Object>> catalog = ProgramSelectionApi.catalog();
        String main = title(catalog, mainId);
        ProgramSelectionApi.read().assertRoles(mainId, secondary);
        selection.assertSummary(main, titles(catalog, secondary), secondaryTitles(catalog, mainId));
        long action = System.currentTimeMillis();
        selection.start(main);
        Map<String, Object> complete = (Map<String, Object>) ProgramSelectionApi.exchange("onboarding/complete/", action).get("response");
        Assert.assertEquals("Completion saved the wrong Main Journey", mainId, complete.get("main_journey_program_id"));
        Assert.assertEquals("Completion saved the wrong Secondary set", new LinkedHashSet<>(secondary),
                new LinkedHashSet<>((List<String>) complete.get("secondary_program_ids")));
        Assert.assertEquals("Completion activated the wrong program", mainId, complete.get("active_program_id"));
        Assert.assertEquals("Completion did not finish onboarding", true, complete.get("onboarding_program_selection_completed"));
        selection.finishFirstVisitIntroduction();
        selection.waitForFirstDailyPlan(main);
        new ProgramSwitchFeaturePageObject(driver).dismissProgramTooltipsIfPresent();
        IOSMyProgramsPageObject today = new IOSMyProgramsPageObject(driver);
        if (today.isElementVisible("id:My programs")) today.closeProgramSelector();
        today.openTodayTab();
        Assert.assertEquals("Start opened the wrong Daily Plan", main, today.getActiveProgramName());
        today.assertCurrentDailyContent(SimulatorProgramContent.readProgramSelection("COA-9792", mainId, action), mainId);
        ProgramSelectionState saved = ProgramSelectionApi.read();
        saved.assertRoles(mainId, secondary);
        saved.assertActive(mainId);
        today.openProgramSelector();
        today.assertMatchesState(saved);
        today.closeProgramSelector();
        closeAndReopenCoachApplication();
        selection.closeOptionalStartupPrompts();
        today.openTodayTab();
        selection.assertProgramSelectionAbsent();
        ProgramSelectionState reopened = ProgramSelectionApi.read();
        reopened.assertExactlyEquals(saved);
        today.openProgramSelector();
        today.assertMatchesState(reopened);
        today.closeProgramSelector();
    }

    @Test @Issue("COA-9792")
    public void completedOnboardingSurvivesColdLaunchWithoutRepeatingProgramSelection() throws Exception {
        MobileCaseFixture f = prepare("COA-9792");
        ProgramSelectionState saved = ProgramSelectionApi.read();
        saved.assertRoles(f.required("main.id"), ids(f, "secondary.ids"));
        saved.assertActive(saved.mainId());
        IOSMyProgramsPageObject today = new IOSMyProgramsPageObject(driver);
        selection.closeOptionalStartupPrompts();
        today.openTodayTab();
        today.openProgramSelector();
        today.assertMatchesState(saved);
        today.closeProgramSelector();
        closeAndReopenCoachApplication();
        selection.closeOptionalStartupPrompts();
        today.openTodayTab();
        selection.assertProgramSelectionAbsent();
        ProgramSelectionState reopened = ProgramSelectionApi.read();
        reopened.assertExactlyEquals(saved);
        today.openProgramSelector();
        today.assertMatchesState(reopened);
        today.closeProgramSelector();
    }

    @Test @Issue("COA-9803")
    public void secondaryPreviewsPreserveCheckmarksCounterAndSavedDraft() throws Exception {
        MobileCaseFixture f = prepare("COA-9803");
        String mainId = f.required("main.id"), selectedId = f.required("selected.id"), unselectedId = f.required("unselected.id");
        List<Map<String, Object>> catalog = ProgramSelectionApi.catalog();
        String main = title(catalog, mainId), selected = title(catalog, selectedId), unselected = title(catalog, unselectedId);
        selection.assertSelection(main, Collections.singletonList(selected));
        ProgramSelectionState before = ProgramSelectionApi.read();
        preview(f, unselected, "unselected", true);
        selection.assertSelection(main, Collections.singletonList(selected));
        ProgramSelectionApi.read().assertExactlyEquals(before);
        preview(f, selected, "selected", false);
        selection.assertSelection(main, Collections.singletonList(selected));
        ProgramSelectionApi.read().assertExactlyEquals(before);
        preview(f, selected, "selected", true);
        selection.assertSelection(main, Collections.singletonList(selected));
        preview(f, unselected, "unselected", false);
        selection.assertSelection(main, Collections.singletonList(selected));
        ProgramSelectionApi.read().assertExactlyEquals(before);
        List<String> available = selection.secondaryTitles();
        selection.openSummary();
        selection.assertSummary(main, Collections.singletonList(selected), available);
        ProgramSelectionApi.read().assertRoles(mainId, Collections.singletonList(selectedId));
    }

    @Test @Issue("COA-9803")
    public void changeMainPreviewsDoNotSaveAChoiceOrLoseTheRecommendation() throws Exception {
        MobileCaseFixture f = prepare("COA-9803");
        ProgramSelectionApi.assertFreshOnboardingProfile();
        String recommended = f.required("recommended.id"), previewId = f.required("preview.id");
        List<Map<String, Object>> catalog = ProgramSelectionApi.catalog();
        selection.openMainPicker();
        ProgramSelectionState before = ProgramSelectionApi.read();
        preview(f, title(catalog, previewId), "alternative", true);
        selection.assertMainCatalog(catalog, recommended);
        ProgramSelectionApi.read().assertExactlyEquals(before);
        preview(f, title(catalog, previewId), "alternative", false);
        selection.assertMainCatalog(catalog, recommended);
        ProgramSelectionApi.read().assertExactlyEquals(before);
        Assert.assertFalse("Preview saved Main and opened Secondary", selection.isElementVisible("id:What else would you like to work on?"));
        Assert.assertNotEquals("Go back fixture needs an alternative", recommended, previewId);
        selection.toggle(title(catalog, previewId));
        ProgramSelectionApi.read().assertExactlyEquals(before);
        selection.returnToRecommendation();
        selection.assertText(title(catalog, recommended));
        Assert.assertEquals("Go back changed the recommendation", recommended, ProgramSelectionApi.recommendation().get("program_id"));
        ProgramSelectionApi.read().assertExactlyEquals(before);
    }

    private void preview(MobileCaseFixture f, String title, String prefix, boolean trailer) {
        String kind = trailer ? "trailer" : "details";
        // Unique player/detail identity and its observed close control are build-specific reviewed selectors.
        String ready = f.selector(prefix + "." + kind + ".ready");
        String close = f.selector(prefix + "." + kind + ".close");
        if (trailer) selection.openCardTrailer(title); else selection.openCardDetails(title);
        selection.waitForElementVisible(ready, "Wrong program preview opened", 20);
        if (!trailer) selection.assertText(title);
        if (!trailer) {
            selection.assertInformationalDetails();
        }
        selection.waitForElementAndClick(close, "Cannot close program preview", 10);
        selection.waitForElementNotVisible(ready, "Preview remained open", 10);
    }

    private void assertMainSaved(long action, String id) throws Exception {
        Map<String, Object> exchange = ProgramSelectionApi.exchange("main_journey/", action);
        Assert.assertEquals("Main Journey request differs from the selected ID",
                Collections.singletonMap("program_id", id), exchange.get("request"));
        Map<String, Object> response = (Map<String, Object>) exchange.get("response");
        Map<String, Object> main = (Map<String, Object>) response.get("main_journey");
        Assert.assertEquals("Main Journey response differs from the selected ID", id, main.get("program_id"));
        Assert.assertEquals("Main Journey response has the wrong role", "main_journey", main.get("membership_status"));
    }

    private void assertSecondarySaved(long action, List<String> ids) throws Exception {
        Map<String, Object> exchange = ProgramSelectionApi.exchange("secondary_programs/", action);
        Map<String, Object> request = (Map<String, Object>) exchange.get("request");
        Assert.assertEquals("Secondary request is not a full replacement", new LinkedHashSet<>(ids),
                new LinkedHashSet<>((List<String>) request.get("program_ids")));
        Map<String, Object> response = (Map<String, Object>) exchange.get("response");
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("secondary_programs");
        List<String> actual = rows.stream().map(p -> (String) p.get("program_id")).collect(Collectors.toList());
        Assert.assertEquals("Secondary response contains duplicates", actual.size(), new LinkedHashSet<>(actual).size());
        Assert.assertEquals("Secondary response is not the saved set", new LinkedHashSet<>(ids), new LinkedHashSet<>(actual));
    }

    static List<String> ids(MobileCaseFixture f, String key) {
        String value = f.required(key);
        if ("[]".equals(value)) return Collections.emptyList();
        List<String> result = Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toList());
        Assert.assertTrue("Invalid program_id list", result.stream().allMatch(v -> v.matches("[a-z0-9_]{1,80}")));
        Assert.assertEquals("Duplicate fixture program IDs", result.size(), new LinkedHashSet<>(result).size());
        return result;
    }

    static String title(List<Map<String, Object>> catalog, String id) {
        List<Map<String, Object>> matching = catalog.stream().filter(p -> id.equals(p.get("program_id"))).collect(Collectors.toList());
        Assert.assertEquals("Expected catalog program is absent or duplicated: " + id, 1, matching.size());
        return (String) matching.get(0).get("title");
    }

    static List<String> titles(List<Map<String, Object>> catalog, List<String> ids) {
        return ids.stream().map(id -> title(catalog, id)).collect(Collectors.toList());
    }

    static List<String> secondaryTitles(List<Map<String, Object>> catalog, String main) {
        return titles(catalog, catalog.stream().map(p -> (String) p.get("program_id"))
                .filter(id -> !id.equals(main)).collect(Collectors.toList()));
    }
}
