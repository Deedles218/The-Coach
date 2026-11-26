package tests;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import lib.BookmarksEvidence;
import lib.CoreTestCase;
import lib.Platform;
import lib.RestoringTestAction;
import lib.TestData;
import lib.UpdateEvidence;
import lib.ui.BookmarksPageObject;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.ios.IOSTestFlightPageObject;
import lib.ui.ios.iOSCoachFlowPageObject;
import lib.ui.ios.iOSProgramSelectorPageObject;
import lib.ui.factories.BookmarksPageObjectFactory;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.Description;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Epic("Daily Plan")
@Feature("Bookmarks")
public class BookmarksTests extends CoreTestCase {
    private static final String BASELINE = "BASELINE";
    private static final String CONTENT = "CONTENT";
    private static final String TOOLTIP = "TOOLTIP";
    private static final String RECURRING = "RECURRING";
    private static final String SWITCH_A = "SWITCHA";
    private static final String SWITCH_B = "SWITCHB";
    protected static final String UPDATE = "UPDATE";
    protected static final String NO_SUBSCRIPTION = "NOSUBSCRIPTION";

    protected CoachFlowPageObject coach;
    protected BookmarksPageObject bookmarks;

    protected void requireSupportedPlatform() {
        Assert.assertTrue("Bookmarks Xray cases are currently scoped to iOS", Platform.getInstance().isIOS());
    }

    protected void prepare(String fixture) {
        requireSupportedPlatform();
        coach = CoachFlowPageObjectFactory.get(driver);
        bookmarks = BookmarksPageObjectFactory.get(driver);
        bookmarks.closeOpenedContentIfPresent();
        if (bookmarks.isScreenVisible()) bookmarks.backFromBookmarks();
        if (Boolean.getBoolean("coach.bookmarks.useCurrentAccount")) {
            coach.activateAppIfPossible();
            coach.closePdfGuideUpsellIfPresent();
            coach.closeNotificationPromptIfPresent();
            coach.closeConnectEmailPromptIfPresent();
            bookmarks.closeOpenedContentIfPresent();
            if (bookmarks.isScreenVisible()) bookmarks.backFromBookmarks();
            coach.assertAuthorizedDashboardIsDisplayed();
            coach.openToday();
            return;
        }
        login(TestData.bookmarksAccount(fixture));
    }

    protected void login(TestData.TestAccount account) {
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        coach.openProfile();
        ((iOSCoachFlowPageObject) coach).assertAuthenticatedProfileForEmail(account.getEmail());
        coach.openToday();
    }

    private void switchTo(String fixture) {
        Assert.assertFalse("Multi-account Bookmarks cases cannot use coach.bookmarks.useCurrentAccount",
                Boolean.getBoolean("coach.bookmarks.useCurrentAccount"));
        bookmarks.closeOpenedContentIfPresent();
        if (bookmarks.isScreenVisible()) bookmarks.backFromBookmarks();
        coach.openToday();
        coach.openProfile();
        coach.logOut();
        login(TestData.bookmarksAccount(fixture));
    }

    @Override
    protected void cleanupSessionForNextTest() {
        if (Boolean.getBoolean("coach.bookmarks.useCurrentAccount")) return;
        try {
            if (bookmarks != null) {
                bookmarks.closeOpenedContentIfPresent();
                if (bookmarks.isScreenVisible()) bookmarks.backFromBookmarks();
            }
        } finally {
            super.cleanupSessionForNextTest();
        }
    }

    @Test @Issue("COA-9647")
    public void bookmarksOpenFromTodayAndExplore() {
        String lesson = required("lessonTitle");
        String tooltip = required("tooltipText");
        prepare(CONTENT);
        bookmarks.openFromToday();
        bookmarks.assertTooltipAbsent(tooltip);
        bookmarks.assertCard(lesson);
        bookmarks.backFromBookmarksToToday();
        bookmarks.openFromExplore();
        bookmarks.assertTooltipAbsent(tooltip);
        bookmarks.assertCard(lesson);
        bookmarks.backFromBookmarksToExplore();
    }

    @Test @Issue("COA-9648")
    public void emptyListNeverShowsTooltip() {
        prepare(BASELINE);
        String tooltip = required("tooltipText");
        bookmarks.openFromToday();
        bookmarks.assertEmptyState();
        bookmarks.assertTooltipAbsent(tooltip);
        bookmarks.backFromBookmarks();
        bookmarks.openFromToday();
        bookmarks.assertEmptyState();
        bookmarks.assertTooltipAbsent(tooltip);
    }

    @Test @Issue("COA-9649")
    public void saveFromAnySlideSynchronizesWholeLessonAndPractice() throws Exception {
        prepare(CONTENT);
        verifyWholeContentBookmark(required("lessonTitle"), required("lessonFirstSlideText"),
                required("lessonMiddleSlideText"),
                required("lessonLastSlideText"), required("lessonSavedSlideText"));
        verifyWholeContentBookmark(required("practiceTitle"), required("practiceFirstSlideText"),
                required("practiceMiddleSlideText"),
                required("practiceLastSlideText"), required("practiceSavedSlideText"));
    }

    @Test @Issue("COA-9650")
    public void removeInsideContentAndSaveAgain() throws Exception {
        prepare(CONTENT);
        verifyRemoveInsideAndSaveAgain("lesson");
        verifyRemoveInsideAndSaveAgain("practice");
    }

    private void verifyRemoveInsideAndSaveAgain(String kind) throws Exception {
        String title = required(kind + "Title");
        String first = required(kind + "FirstSlideText");
        String middle = required(kind + "MiddleSlideText");
        String last = required(kind + "LastSlideText");
        runRestoringScenario(kind + "ContentToggle", () -> {
            openFixtureContentFromToday(title);
            bookmarks.ensureOpenedContentIsNotSaved();
            bookmarks.swipeRightAndAssertNotSaved(first);
            bookmarks.saveOpenedContent();
            bookmarks.swipeLeftAndAssertSaved(middle);
            bookmarks.removeOpenedContent();
            bookmarks.assertOpenedContentNotSaved();
            bookmarks.swipeLeftAndAssertNotSaved(last);
            bookmarks.swipeRightAndAssertNotSaved(first);
            bookmarks.closeOpenedContent();
            bookmarks.openFromToday();
            bookmarks.assertCardAbsent(title);
            bookmarks.backFromBookmarks();
            openFixtureContentFromToday(title);
            bookmarks.swipeRightAndAssertNotSaved(first);
            bookmarks.saveOpenedContent();
            bookmarks.swipeLeftAndAssertSaved(middle);
            bookmarks.swipeLeftAndAssertSaved(last);
            bookmarks.swipeRightAndAssertSaved(first);
            bookmarks.closeOpenedContent();
            bookmarks.openFromToday();
            bookmarks.assertSingleCard(title, first, middle, last);
        }, () -> prepareSavedContent(title, first, required(kind + "SavedSlideText")));
    }

    @Test @Issue("COA-9651")
    public void cardsAreGroupedAndContainExpectedFields() {
        prepare(CONTENT);
        String lesson = required("lessonTitle");
        String practice = required("practiceTitle");
        bookmarks.openFromToday();
        bookmarks.assertCardGroupAndCategory(lesson, required("programTitle"), "Lessons");
        bookmarks.assertCardComposition(lesson);
        String practiceGroup = optional("practiceGroupTitle");
        bookmarks.assertCardGroupAndCategory(practice,
                practiceGroup == null ? required("moduleTitle") : practiceGroup, "Practices");
        bookmarks.assertCardComposition(practice);

        // Both categories above are populated. Verify hiding on the separately
        // prepared lesson-only fixture, where Practices is actually empty.
        switchTo(UPDATE);
        bookmarks.openFromToday();
        bookmarks.assertCardGroupAndCategory(lesson, required("programTitle"), "Lessons");
        bookmarks.assertCardComposition(lesson);
        bookmarks.assertCategoryAbsent("Practices");
    }

    @Test @Issue("COA-9652")
    public void tooltipWaitsForFirstNonEmptyEntry() throws Exception {
        String tooltip = required("tooltipText");
        String lesson = required("lessonTitle");
        prepareFreshTooltipFixture(BASELINE);
        boolean[] saveAttempted = {false};
        runRestoringScenario("tooltipEmptyFirst", () -> {
            bookmarks.openFromToday();
            bookmarks.assertEmptyState();
            bookmarks.assertTooltipAbsent(tooltip);
            bookmarks.backFromBookmarks();
            openFixtureContentFromToday(lesson);
            bookmarks.assertOpenedContentNotSaved();
            saveAttempted[0] = true;
            bookmarks.saveOpenedContent();
            bookmarks.closeOpenedContent();
            bookmarks.assertFirstEntryTooltip(tooltip, Collections.singletonList(lesson));
        }, () -> {
            if (saveAttempted[0]) {
                bookmarks.dismissTooltipIfVisible(tooltip);
                bookmarks.closeOpenedContentIfPresent();
                openFixtureContentFromToday(lesson);
                bookmarks.assertOpenedContentTitle(lesson);
                bookmarks.ensureOpenedContentIsNotSaved();
                bookmarks.closeOpenedContent();
                bookmarks.openFromToday();
                bookmarks.assertEmptyState();
            }
        });
    }

    @Test @Issue("COA-9653")
    public void anyTapDismissesTooltipAndItDoesNotReturn() throws Exception {
        verifyTooltipDismissal("blank");
    }

    @Test @Issue("COA-9653")
    public void tappingCardDismissesTooltipAndItDoesNotReturn() throws Exception {
        verifyTooltipDismissal("card");
    }

    @Test @Issue("COA-9653")
    public void tappingBookmarkDismissesTooltipAndItDoesNotReturn() throws Exception {
        verifyTooltipDismissal("bookmark");
    }

    @Test @Issue("COA-9653")
    public void tappingNavigationDismissesTooltipAndItDoesNotReturn() throws Exception {
        verifyTooltipDismissal("navigation");
    }

    @Test @Issue("COA-9652")
    public void firstNonEmptyEntryShowsTooltip() throws Exception {
        String tooltip = required("tooltipText");
        List<String> titles = Arrays.asList(required("lessonTitle"), required("practiceTitle"));
        prepareFreshTooltipFixture(TOOLTIP);
        bookmarks.assertFirstEntryTooltip(tooltip, titles);
    }

    protected void verifyTooltipDismissal(String variant) throws Exception {
        String tooltip = required("tooltipText");
        List<String> titles = Arrays.asList(required("lessonTitle"), required("practiceTitle"));
        prepareFreshTooltipFixture(TOOLTIP);
        String first = bookmarks.openFromTodayAndInspectTooltip(tooltip, titles);
        if ("card".equals(variant)) bookmarks.dismissTooltipByCard(first, tooltip);
        else if ("bookmark".equals(variant)) bookmarks.dismissTooltipByBookmark(first, tooltip);
        else if ("navigation".equals(variant)) bookmarks.dismissTooltipByNavigation(tooltip);
        else bookmarks.dismissTooltipBesideCard(first, tooltip);
        bookmarks.reopenAndAssertTooltipAbsent(tooltip);
        bookmarks.assertExactCardTitles(titles, null);
    }

    private void prepareFreshTooltipFixture(String role) throws Exception {
        String release = optional("tooltipTestFlightBuild");
        if (release != null) {
            Assert.assertTrue("TestFlight tooltip preparation requires iOS", Platform.getInstance().isIOS());
            Assert.assertFalse("A fresh TestFlight installation requires verified fixture login",
                    Boolean.getBoolean("coach.bookmarks.useCurrentAccount"));
            TestData.bookmarksAccount(role);
            String bundle = Platform.getInstance().getIOSBundleId();
            IOSTestFlightPageObject testFlight = new IOSTestFlightPageObject(driver, bundle, release);
            Map<String, String> expected = testFlight.expectedBuild();
            Assert.assertEquals("Tooltip preparation requires the selected build to be installed",
                    expected, UpdateEvidence.readInstalledIOSBuild(driver));
            testFlight.verifyOfferedBuild();
            reinstallIOSBookmarksApp((InteractsWithApps) driver, bundle, () -> testFlight.install(false));
            Map<String, String> installed = UpdateEvidence.readInstalledIOSBuild(driver);
            Assert.assertEquals("Tooltip preparation installed an unexpected iOS build", expected, installed);
            UpdateEvidence.attachBuild("Fresh iOS tooltip installation", installed.get("version"), installed.get("build"));
            CoachFlowPageObjectFactory.get(driver).activateAppIfPossible();
        }
        prepare(role);
    }

    @Test @Issue("COA-9654")
    public void cancelKeepsSelectedBookmark() {
        String lesson = required("lessonTitle");
        String practice = required("practiceTitle");
        String lessonNeighbor = secondContentTitle();
        List<String> initialCards = configuredCardRoster("cancelInitialCards", lesson, practice, lessonNeighbor);
        List<String> lessonMarkers = contentSlideMarkers("lesson");
        List<String> practiceMarkers = contentSlideMarkers("practice");
        prepare(CONTENT);
        verifyCancelKeepsBookmark(lesson, lessonNeighbor, lessonMarkers, initialCards);
        verifyCancelKeepsBookmark(practice, lesson, practiceMarkers, initialCards);
    }

    private void verifyCancelKeepsBookmark(String first, String second, List<String> markers, List<String> initialCards) {
        assertFixtureContainsPair(initialCards, first, second);
        bookmarks.openFromToday();
        bookmarks.assertTooltipAbsent(required("tooltipText"));
        bookmarks.assertExactCardTitles(initialCards, null);
        bookmarks.openRemovalDialog(first);
        bookmarks.cancelRemoval();
        bookmarks.assertExactCardTitles(initialCards, null);
        bookmarks.openCard(first);
        bookmarks.assertOpenedContentTitle(first);
        bookmarks.assertBookmarkOnEverySlide(markers, true);
        bookmarks.closeOpenedContent();
        if (bookmarks.isScreenVisible()) bookmarks.backFromBookmarks();
        bookmarks.openFromToday();
        bookmarks.assertExactCardTitles(initialCards, null);
    }

    @Test @Issue("COA-9655")
    public void removeDeletesOnlySelectedBookmarkAndSyncsContent() throws Exception {
        List<String> initialCards = configuredCardRoster("removalInitialCards", required("lessonTitle"),
                required("lessonRemovalNeighborTitle"), required("practiceTitle"), required("practiceRemovalNeighborTitle"));
        prepare(CONTENT);
        verifyRemoveFromList("lesson", initialCards);
        verifyRemoveFromList("practice", initialCards);
    }

    private void verifyRemoveFromList(String kind, List<String> initialCards) throws Exception {
        String first = required(kind + "Title");
        String neighbor = required(kind + "RemovalNeighborTitle");
        String group = required(kind + "RemovalGroupTitle");
        String category = "lesson".equals(kind) ? "Lessons" : "Practices";
        List<String> markers = contentSlideMarkers(kind);
        String savedSlide = required(kind + "SavedSlideText");
        String neighborSlide = required(kind + "RemovalNeighborSavedSlideText");
        Assert.assertTrue("The original saved slide must be in the configured slide sequence", markers.contains(savedSlide));
        assertFixtureContainsPair(initialCards, first, neighbor);
        List<String> remaining = new ArrayList<>(initialCards);
        remaining.remove(first);
        bookmarks.openFromToday();
        bookmarks.assertTooltipAbsent(required("tooltipText"));
        bookmarks.assertExactCardTitles(initialCards, null);
        bookmarks.assertCardGroupAndCategory(first, group, category);
        bookmarks.assertCardGroupAndCategory(neighbor, group, category);
        assertSavedBookmarkAtSlide(first, savedSlide);
        assertSavedBookmarkAtSlide(neighbor, neighborSlide);

        boolean[] restoreSelectedBookmark = {false};
        runRestoringScenario(kind + "ListRemoval", () -> {
            bookmarks.openRemovalDialog(first);
            restoreSelectedBookmark[0] = true;
            bookmarks.confirmRemoval();
            bookmarks.assertExactCardTitles(remaining, null);
            bookmarks.assertCardGroupAndCategory(neighbor, group, category);
            assertSavedBookmarkAtSlide(neighbor, neighborSlide);
            bookmarks.backFromBookmarks();
            openFixtureContentFromToday(first);
            bookmarks.assertOpenedContentTitle(first);
            bookmarks.assertBookmarkOnEverySlide(markers, false);
            bookmarks.swipeRightAndAssertNotSaved(savedSlide);
            bookmarks.saveOpenedContent();
            bookmarks.closeOpenedContent();
            bookmarks.openFromToday();
            bookmarks.assertCard(first);
            restoreSelectedBookmark[0] = false;
            bookmarks.assertExactCardTitles(initialCards, null);
            bookmarks.assertCardGroupAndCategory(neighbor, group, category);
            assertSavedBookmarkAtSlide(neighbor, neighborSlide);
        }, () -> {
            if (restoreSelectedBookmark[0]) {
                prepareSavedContent(first, markers.get(0), savedSlide);
                bookmarks.openFromToday();
                bookmarks.assertExactCardTitles(initialCards, null);
                assertSavedBookmarkAtSlide(neighbor, neighborSlide);
            }
        });
    }

    private void assertSavedBookmarkAtSlide(String title, String marker) {
        assertSavedBookmarkAtSlide(title, marker, null);
    }

    private void assertSavedBookmarkAtSlide(String title, String marker, String acceptedFirstSlide) {
        bookmarks.openCard(title);
        if (acceptedFirstSlide == null) {
            bookmarks.assertOpenedAtSlide(title, marker);
        } else {
            bookmarks.assertOpenedAtFirstOrSavedSlide(title, acceptedFirstSlide, marker);
        }
        bookmarks.assertOpenedContentSaved();
        bookmarks.closeOpenedContent();
        bookmarks.openFromToday();
    }

    private static void assertFixtureContainsPair(List<String> roster, String first, String second) {
        Assert.assertNotEquals("The two saved cards must be distinct", first, second);
        Assert.assertTrue("The initial bookmark roster must include both fixture cards", roster.contains(first) && roster.contains(second));
    }

    private static List<String> configuredCardRoster(String field, String... defaults) {
        String configured = optional(field);
        return configured == null ? new ArrayList<>(new LinkedHashSet<>(Arrays.asList(defaults)))
                : parseDistinctFixtureValues(configured, field);
    }

    private static List<String> contentSlideMarkers(String kind) {
        String field = kind + "SlideMarkers";
        return parseDistinctFixtureValues(required(field), field);
    }

    private static List<String> parseDistinctFixtureValues(String value, String field) {
        List<String> result = new ArrayList<>();
        for (String piece : value.split("\\|", -1)) {
            String item = piece.trim();
            Assert.assertFalse(field + " requires nonempty entries", item.isEmpty());
            Assert.assertFalse(field + " requires distinct entries", result.contains(item));
            result.add(item);
        }
        return result;
    }

    @Test @Issue("COA-9656")
    public void deletingLastBookmarkShowsEmptyState() throws Exception {
        verifyDeletingLastBookmark("lesson", false);
    }

    @Test @Issue("COA-9656")
    public void deletingLastPracticeFromListShowsEmptyState() throws Exception {
        verifyDeletingLastBookmark("practice", false);
    }

    @Test @Issue("COA-9656")
    public void deletingLastLessonInsideContentShowsEmptyState() throws Exception {
        verifyDeletingLastBookmark("lesson", true);
    }

    @Test @Issue("COA-9656")
    public void deletingLastPracticeInsideContentShowsEmptyState() throws Exception {
        verifyDeletingLastBookmark("practice", true);
    }

    private void verifyDeletingLastBookmark(String kind, boolean insideContent) throws Exception {
        String tooltip = required("tooltipText");
        String title = required(kind + "Title");
        String firstSlide = required(kind + "FirstSlideText");
        prepare(BASELINE);
        assertEmptyBookmarksWithoutTooltip(tooltip);
        bookmarks.backFromBookmarks();

        boolean[] saveAttempted = {false};
        runRestoringScenario("deleteLastBookmark", () -> {
            openFixtureContentFromToday(title);
            bookmarks.assertOpenedContentTitle(title);
            bookmarks.swipeRightAndAssertNotSaved(firstSlide);
            saveAttempted[0] = true;
            bookmarks.saveOpenedContent();
            bookmarks.closeOpenedContent();
            bookmarks.openFromTodayWithTooltipDismissed(title, tooltip);
            bookmarks.assertTooltipAbsent(tooltip);
            bookmarks.assertExactCardTitles(Collections.singletonList(title), null);
            io.qameta.allure.Allure.addAttachment("Last-bookmark fixture",
                    "Initially empty BASELINE fixture; one " + kind + " bookmark created through UI; remove "
                            + (insideContent ? "inside content" : "from list"));
            if (insideContent) {
                bookmarks.openCard(title);
                bookmarks.assertOpenedContentTitle(title);
                bookmarks.removeOpenedContent();
                bookmarks.assertOpenedContentNotSaved();
                bookmarks.closeOpenedContent();
            } else {
                bookmarks.openRemovalDialog(title);
                bookmarks.confirmRemoval();
            }
            assertEmptyBookmarksWithoutTooltip(tooltip);
            bookmarks.backFromBookmarks();
            assertEmptyBookmarksWithoutTooltip(tooltip);
            saveAttempted[0] = false;
        }, () -> {
            if (saveAttempted[0]) {
                bookmarks.dismissTooltipIfVisible(tooltip);
                bookmarks.closeOpenedContentIfPresent();
                openFixtureContentFromToday(title);
                bookmarks.assertOpenedContentTitle(title);
                bookmarks.ensureOpenedContentIsNotSaved();
                bookmarks.closeOpenedContent();
                assertEmptyBookmarksWithoutTooltip(tooltip);
            }
        });
    }

    private void assertEmptyBookmarksWithoutTooltip(String tooltip) {
        bookmarks.openFromToday();
        bookmarks.assertEmptyState();
        bookmarks.assertTooltipAbsent(tooltip);
    }

    private void runRestoringScenario(String name, RestoringTestAction.CheckedAction action,
                                      RestoringTestAction.CheckedAction restore) throws Exception {
        RestoringTestAction.run(() -> {
            try {
                action.run();
            } catch (Exception | AssertionError failure) {
                captureFailureArtifacts(failure,
                        Description.createTestDescription(getClass(), name + "BeforeRestore"));
                throw failure;
            }
        }, restore);
    }

    @Test @Issue("COA-9657")
    public void cardsOpenLessonAndPracticeAtSavedSlides() {
        String lesson = required("lessonTitle");
        String practice = required("practiceTitle");
        String lessonSavedSlide = required("lessonSavedSlideText");
        String practiceSavedSlide = required("practiceSavedSlideText");
        List<String> lessonSlides = contentSlideMarkers("lesson");
        List<String> practiceSlides = contentSlideMarkers("practice");
        int lessonPosition = lessonSlides.indexOf(lessonSavedSlide);
        int practicePosition = practiceSlides.indexOf(practiceSavedSlide);
        Assert.assertNotEquals("COA-9657 needs distinct lesson and practice cards", lesson, practice);
        Assert.assertNotEquals("The saved slides must have distinguishable text", lessonSavedSlide, practiceSavedSlide);
        Assert.assertTrue("Save the lesson on a middle slide of the configured sequence",
                lessonPosition > 0 && lessonPosition < lessonSlides.size() - 1);
        Assert.assertTrue("Save the practice at a different position in its configured sequence",
                practicePosition >= 0 && practicePosition != lessonPosition);

        prepare(fixtureRole("savedSlideAccountRole", CONTENT));
        bookmarks.openFromToday();
        bookmarks.assertTooltipAbsent(required("tooltipText"));
        // COA-9620 also permits reopening Android content at its first slide.
        boolean allowFirstSlide = Platform.getInstance().isAndroid();
        assertSavedBookmarkAtSlide(lesson, lessonSavedSlide, allowFirstSlide ? lessonSlides.get(0) : null);
        assertSavedBookmarkAtSlide(practice, practiceSavedSlide, allowFirstSlide ? practiceSlides.get(0) : null);
    }

    @Test @Issue("COA-9658")
    public void finishReturnsToBookmarksWithoutFeedback() throws Exception {
        verifyFinishReturnsToBookmarks("lesson");
    }

    @Test @Issue("COA-9658")
    public void finishPracticeReturnsToBookmarksWithoutFeedback() throws Exception {
        verifyFinishReturnsToBookmarks("practice");
    }

    private void verifyFinishReturnsToBookmarks(String kind) throws Exception {
        String title = required(kind + "Title");
        runWithCompletionSnapshot("COA-9658", kind, title, () -> {
            openCompletionBookmark(title);
            bookmarks.finishFromBookmarks(optional(kind + "CompletionAnswer"));
        });
    }

    @Test @Issue("COA-9659")
    public void finishMarksLessonCompletedInDailyPlan() throws Exception {
        String completionLesson = optional("completionLessonTitle");
        String lesson = completionLesson == null ? required("lessonTitle") : completionLesson;
        String savedSlide = required(completionLesson == null
                ? "lessonSavedSlideText" : "completionLessonSavedSlideText");
        runWithCompletionSnapshot("COA-9659", "lesson", lesson, () -> {
            String program = optional("completionProgramTitle");
            selectFixtureProgram(program == null && completionLesson == null
                    ? optional("lessonProgramTitle") : program);
            selectFixtureContentDay(optional("fixtureContentDay"));
            bookmarks.assertDailyPlanItemNotCompleted(lesson);
            bookmarks.openContentFromToday(lesson);
            bookmarks.assertOpenedContentNotSaved();
            bookmarks.swipeLeftToSlide(savedSlide);
            bookmarks.saveOpenedContent();
            bookmarks.closeOpenedContent();
            openCompletionBookmark(lesson);
            String answer = optional("completionAnswer");
            if (answer == null && completionLesson == null) answer = optional("lessonCompletionAnswer");
            bookmarks.finishFromBookmarks(answer);
            bookmarks.assertDailyPlanItemCompleted(lesson);
        });
    }

    @Test @Issue("COA-9660")
    public void removingBookmarkDoesNotResetCompletedLesson() throws Exception {
        verifyCompletedLessonSurvivesRemoval(false);
    }

    @Test @Issue("COA-9660")
    public void removingBookmarkInsideContentDoesNotResetCompletedLesson() throws Exception {
        verifyCompletedLessonSurvivesRemoval(true);
    }

    private void verifyCompletedLessonSurvivesRemoval(boolean insideContent) throws Exception {
        String lesson = required("lessonTitle");
        runWithCompletionSnapshot("COA-9660", insideContent ? "reader" : "list", lesson, () -> {
            selectFixtureProgram(optional("lessonProgramTitle"));
            selectFixtureContentDay(optional("fixtureContentDay"));
            bookmarks.assertDailyPlanItemNotCompleted(lesson);
            openCompletionBookmark(lesson);
            bookmarks.finishFromBookmarks(optional("lessonCompletionAnswer"));
            bookmarks.assertDailyPlanItemCompleted(lesson);
            bookmarks.openFromToday();
            if (insideContent) {
                bookmarks.openCard(lesson);
                bookmarks.removeOpenedContent();
                bookmarks.assertOpenedContentNotSaved();
                bookmarks.closeOpenedContent();
                bookmarks.openFromToday();
            } else {
                bookmarks.openRemovalDialog(lesson);
                bookmarks.confirmRemoval();
            }
            bookmarks.assertCardAbsent(lesson);
            bookmarks.assertDailyPlanItemCompleted(lesson);
        });
    }

    private void openCompletionBookmark(String title) {
        bookmarks.openFromToday();
        bookmarks.assertTooltipAbsent(required("tooltipText"));
        bookmarks.openCard(title);
        bookmarks.assertOpenedContentTitle(title);
        bookmarks.assertOpenedContentSaved();
    }

    private void runWithCompletionSnapshot(String issue, String variant, String title,
                                           RestoringTestAction.CheckedAction scenario) throws Exception {
        requireSupportedPlatform();
        Assert.assertFalse(issue + " requires verified fixture login",
                Boolean.getBoolean("coach.bookmarks.useCurrentAccount"));
        Path control = completionControl("completionControl", issue);
        String role = fixtureRole("completionAccountRole", CONTENT);
        TestData.TestAccount account = TestData.bookmarksAccount(role);
        Platform platform = Platform.getInstance();
        String app = platform.isIOS() ? platform.getIOSBundleId() : platform.getAndroidAppPackage();
        List<String> scope = Arrays.asList(required("completionSnapshot"), app, issue, variant, title);
        String emailVariable = "COACH_BOOKMARKS_COMPLETION_EMAIL";
        runCompletionControl(control, "verify", scope, account.getEmail(), emailVariable, issue);
        runRestoringScenario(issue + "-" + variant, () -> {
            prepare(role);
            scenario.run();
        }, () -> {
            runCompletionControl(control, "restore", scope, account.getEmail(), emailVariable, issue);
            runCompletionControl(control, "verify", scope, account.getEmail(), emailVariable, issue);
        });
    }

    @Test @Issue("COA-9661")
    public void kegelAndKegelSportCannotBeBookmarked() throws Exception {
        Assert.assertFalse("COA-9661 requires verified Kegel fixture login",
                Boolean.getBoolean("coach.bookmarks.useCurrentAccount"));
        BookmarksEvidence.KegelDailyPlanCard kegel = BookmarksEvidence.readKegelCard(
                required("kegelDailyPlanCapture"), required("kegelTitle"), "kegel");
        BookmarksEvidence.KegelDailyPlanCard sport = BookmarksEvidence.readKegelCard(
                required("kegelSportDailyPlanCapture"), required("kegelSportTitle"), "kegel_sport");
        Assert.assertNotEquals("The workout variants must be distinct", kegel.id, sport.id);

        prepare(fixtureRole("kegelAccountRole", CONTENT));
        DailyPlanPageObject today = DailyPlanPageObjectFactory.get(driver);
        today.openTodayTab();
        String originalProgram = today.getActiveProgramName();
        String originalDay = today.getCurrentDayLabel().trim();
        Assert.assertTrue("Unreadable Kegel fixture day: " + originalDay,
                originalDay.matches("(Day|Stage) [1-9][0-9]*( of [0-9]+)?"));
        runRestoringScenario("excludedKegelBookmarks", () -> {
            verifyKegelWorkoutScreens(today, kegel);
            verifyKegelWorkoutScreens(today, sport);
        }, () -> RestoringTestAction.run(today::closeKegelExerciseFlowIfPresent, () -> {
            today.openTodayTab();
            selectFixtureProgram(originalProgram);
            selectFixtureContentDay(originalDay.split(" ", 3)[1]);
            Assert.assertEquals("Original Daily Plan program was not restored", originalProgram, today.getActiveProgramName());
            Assert.assertEquals("Original Daily Plan day was not restored", originalDay, today.getCurrentDayLabel().trim());
        }));
    }

    private void verifyKegelWorkoutScreens(DailyPlanPageObject today, BookmarksEvidence.KegelDailyPlanCard card) throws Exception {
        long boundary = System.currentTimeMillis();
        today.openTodayTab();
        selectFixtureProgram(card.program);
        selectFixtureContentDay(Integer.toString(card.day));
        Assert.assertEquals("Kegel capture does not match the selected Today day", card.dayLabel, today.getCurrentDayLabel().trim());
        String progressBefore = today.getProgramProgressValue();
        bookmarks.assertDailyPlanItemNotCompleted(card.title);
        BookmarksEvidence.assertCurrentKegelCard(driver, card, boundary,
                TestData.bookmarksAccount(fixtureRole("kegelAccountRole", CONTENT)).getEmail());
        bookmarks.openContentWithoutBookmarkFromToday(card.title);
        bookmarks.assertNoPaywall();
        today.assertKegelStartScreenIsDisplayed();
        bookmarks.assertOpenedContentTitle(card.title);
        bookmarks.assertOpenedContentCannotBeSaved();
        today.openFirstKegelExerciseInstruction();
        bookmarks.assertOpenedContentCannotBeSaved();
        today.returnFromKegelExerciseInstruction();
        if (Platform.getInstance().isIOS()) {
            today.startKegelExercise(bookmarks::assertOpenedContentCannotBeSaved);
        } else {
            today.startKegelExercise();
        }
        today.closeKegelPlayerInstructionsIfPresent();
        today.assertKegelPlayerIsDisplayed();
        bookmarks.assertOpenedContentCannotBeSaved();
        today.pauseKegelExercise();
        bookmarks.assertOpenedContentCannotBeSaved();
        if (Platform.getInstance().isIOS()) {
            today.openKegelPlayerInfo(bookmarks::assertOpenedContentCannotBeSaved);
        }
        today.exitKegelExercisePlayer();
        today.openTodayTab();
        Assert.assertEquals("Quitting a workout changed program progress", progressBefore, today.getProgramProgressValue());
        bookmarks.assertDailyPlanItemNotCompleted(card.title);
    }

    @Test @Issue("COA-9662")
    public void openingSavingAndDeletingEmitAnalytics() {
        prepare(BASELINE);
        String analytics = TestData.bookmarksAnalyticsLogPath();
        long checkpoint = BookmarksEvidence.checkpoint(analytics);
        String lesson = required("lessonTitle");
        bookmarks.openFromToday();
        bookmarks.backFromBookmarks();
        openFixtureContentFromToday(lesson);
        bookmarks.ensureOpenedContentIsNotSaved();
        bookmarks.swipeLeftToSlide(required("lessonSavedSlideText"));
        bookmarks.saveOpenedContent();
        bookmarks.removeOpenedContent();
        BookmarksEvidence.assertBookmarkAnalytics(analytics, checkpoint,
                required("lessonId"), Integer.parseInt(required("savedSlideNumber")));
    }

    @Test @Issue("COA-9663")
    public void iosSaveSendsOriginalProgramAndDay() throws Exception {
        Assert.assertFalse("COA-9663 requires verified RECURRING fixture login",
                Boolean.getBoolean("coach.bookmarks.useCurrentAccount"));
        String title = required("recurringPracticeTitle");
        String program = recurring("programTitle");
        String lessonId = recurring("lessonId");
        String programId = recurring("programId");
        String slideText = recurring("practiceSavedSlideText");
        int previousDay = Integer.parseInt(required("recurringPreviousProgramDay"));
        int savedDay = Integer.parseInt(recurring("programDay"));
        int slide = Integer.parseInt(recurring("savedSlideNumber"));
        Assert.assertTrue("COA-9663 requires an earlier occurrence and a distinct later occurrence",
                previousDay > 0 && previousDay < savedDay);
        Assert.assertTrue("Fixture slide number must be positive", slide > 0);
        List<String> initialCards = recurringInitialCards(title);
        List<String> savedCards = new ArrayList<>(initialCards);
        savedCards.add(title);

        String network = TestData.bookmarksNetworkLogPath();
        BookmarksEvidence.checkpoint(network);
        prepare(RECURRING);
        DailyPlanPageObject today = DailyPlanPageObjectFactory.get(driver);
        String originalProgram = today.getActiveProgramName();
        String originalDayLabel = today.getCurrentDayLabel().trim();
        Assert.assertTrue("Unreadable recurring fixture day: " + originalDayLabel,
                originalDayLabel.matches("(Day|Stage) [1-9][0-9]*( of [0-9]+)?"));
        String originalDay = originalDayLabel.split(" ", 3)[1];
        boolean[] navigationAttempted = {false};
        boolean[] saveAttempted = {false};
        runRestoringScenario("originalBookmarkRequest", () -> {
            assertBookmarkRoster(initialCards);
            bookmarks.backFromBookmarks();
            navigationAttempted[0] = true;
            selectFixtureProgram(program);
            openUnbookmarkedRecurringOccurrence(title, previousDay, slideText);
            bookmarks.closeOpenedContent();
            openUnbookmarkedRecurringOccurrence(title, savedDay, slideText);
            long checkpoint = BookmarksEvidence.checkpoint(network);
            saveAttempted[0] = true;
            bookmarks.saveOpenedContent();
            BookmarksEvidence.assertBookmarkRequest(network, checkpoint, lessonId, programId, savedDay, slide);
            bookmarks.closeOpenedContent();
            assertBookmarkRoster(savedCards);
        }, () -> RestoringTestAction.run(() -> {
            if (saveAttempted[0]) {
                bookmarks.closeOpenedContentIfPresent();
                if (bookmarks.isScreenVisible()) bookmarks.backFromBookmarks();
                selectFixtureProgram(program);
                selectFixtureContentDay(Integer.toString(savedDay));
                bookmarks.openContentFromToday(title);
                bookmarks.assertOpenedContentTitle(title);
                bookmarks.ensureOpenedContentIsNotSaved();
                bookmarks.closeOpenedContent();
                assertBookmarkRoster(initialCards);
            }
        }, () -> {
            if (navigationAttempted[0]) {
                bookmarks.closeOpenedContentIfPresent();
                if (bookmarks.isScreenVisible()) bookmarks.backFromBookmarks();
                coach.openToday();
                selectFixtureProgram(originalProgram);
                selectFixtureContentDay(originalDay);
                Assert.assertEquals("Original Daily Plan program was not restored", originalProgram, today.getActiveProgramName());
                Assert.assertEquals("Original Daily Plan day was not restored", originalDayLabel, today.getCurrentDayLabel().trim());
            }
        }));
    }

    private static List<String> recurringInitialCards(String practice) {
        String configured = optional("recurringInitialCards");
        List<String> titles = new ArrayList<>();
        if (configured != null) {
            for (String card : configured.split("\\|", -1)) {
                String title = card.trim();
                Assert.assertFalse("Initial recurring fixture card titles must be nonempty", title.isEmpty());
                Assert.assertNotEquals("The recurring practice must not be saved in the initial fixture", practice, title);
                Assert.assertFalse("Duplicate card in the initial recurring fixture", titles.contains(title));
                titles.add(title);
            }
        }
        return titles;
    }

    private void openUnbookmarkedRecurringOccurrence(String title, int day, String slideText) {
        selectFixtureContentDay(Integer.toString(day));
        bookmarks.openContentFromToday(title);
        bookmarks.assertOpenedContentTitle(title);
        bookmarks.assertNoPaywall();
        bookmarks.assertOpenedContentNotSaved();
        bookmarks.swipeLeftToSlide(slideText);
        bookmarks.assertOpenedAtSlide(title, slideText);
        bookmarks.assertOpenedContentNotSaved();
    }

    private void assertBookmarkRoster(List<String> titles) {
        bookmarks.openFromToday();
        if (titles.isEmpty()) bookmarks.assertEmptyState();
        else bookmarks.assertExactCardTitles(titles, null);
    }

    @Test @Issue("COA-9664")
    public void recurringPracticeFinishCompletesSavedDay() throws Exception {
        Assert.assertTrue("COA-9664 requires an iOS session", Platform.getInstance().isIOS());
        Assert.assertFalse("COA-9664 requires verified RECURRING fixture login",
                Boolean.getBoolean("coach.bookmarks.useCurrentAccount"));
        Path control = completionControl("recurringCompletionControl", "COA-9664");
        TestData.TestAccount account = TestData.bookmarksAccount(RECURRING);
        String practice = required("recurringPracticeTitle");
        String program = recurring("programTitle");
        String savedDay = required("savedDayTitle");
        String previousDay = required("previousDayTitle");
        String tooltip = required("tooltipText");
        int previousProgramDay = Integer.parseInt(required("recurringPreviousProgramDay"));
        int savedProgramDay = Integer.parseInt(recurring("programDay"));
        Assert.assertTrue("COA-9664 needs distinct earlier and saved occurrences",
                previousProgramDay > 0 && previousProgramDay < savedProgramDay && !previousDay.equals(savedDay));
        List<String> scope = Arrays.asList(required("recurringCompletionSnapshot"), Platform.getInstance().getIOSBundleId(),
                recurring("programId"), recurring("lessonId"), Integer.toString(previousProgramDay), Integer.toString(savedProgramDay));

        String emailVariable = "COACH_BOOKMARKS_RECURRING_EMAIL";
        runCompletionControl(control, "verify", scope, account.getEmail(), emailVariable, "COA-9664");
        prepare(RECURRING);
        iOSProgramSelectorPageObject today = new iOSProgramSelectorPageObject(driver);
        runRestoringScenario("recurringCompletion", () -> {
            selectFixtureProgram(program);
            today.selectObservedDay(savedDay);
            bookmarks.assertDailyPlanItemNotCompleted(practice);
            // Leave Today on D1 so completing the currently selected day would fail the test.
            bookmarks.assertPreviousDayItemNotCompleted(practice, previousDay);
            coach.openToday();
            Assert.assertEquals("Today must be on the earlier occurrence before opening the bookmark",
                    previousDay, today.getCurrentDayLabel().trim());
            bookmarks.openFromToday();
            bookmarks.assertTooltipAbsent(tooltip);
            bookmarks.openCard(practice);
            bookmarks.assertOpenedContentTitle(practice);
            bookmarks.assertOpenedContentSaved();
            bookmarks.finishFromBookmarks(optional("recurringCompletionAnswer"));
            bookmarks.backFromBookmarks();

            coach.openToday();
            Assert.assertEquals("The recurring program must still be selected", program, today.getActiveProgramName());
            today.selectObservedDay(savedDay);
            bookmarks.assertDailyPlanItemCompleted(practice);
            bookmarks.assertPreviousDayItemNotCompleted(practice, previousDay);
        }, () -> {
            runCompletionControl(control, "restore", scope, account.getEmail(), emailVariable, "COA-9664");
            runCompletionControl(control, "verify", scope, account.getEmail(), emailVariable, "COA-9664");
        });
    }

    private static Path completionControl(String setting, String issue) {
        Path control = Paths.get(required(setting)).toAbsolutePath().normalize();
        Assert.assertTrue(issue + " needs an executable snapshot control for the approved QA fixture",
                Files.isRegularFile(control) && Files.isExecutable(control));
        String uid = System.getenv("COACH_EXPECTED_ACCOUNT_UID");
        Assert.assertTrue(issue + " requires the approved fixture UID",
                uid != null && uid.matches("[A-Za-z0-9_-]{20,128}"));
        return control;
    }

    private static void runCompletionControl(Path executable, String action, List<String> scope,
                                             String email, String emailVariable, String issue) throws Exception {
        List<String> command = new ArrayList<>(Arrays.asList(executable.toString(), action));
        command.addAll(scope);
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(new File("/dev/null"));
        builder.environment().keySet().removeIf(key -> key.startsWith("COACH_") && key.endsWith("_OTP"));
        builder.environment().put(emailVariable, email);
        Process process = builder.start();
        try {
            Assert.assertTrue(issue + " snapshot " + action + " timed out", process.waitFor(30, TimeUnit.SECONDS));
            Assert.assertEquals(issue + " snapshot " + action + " failed", 0, process.exitValue());
        } finally {
            if (process.isAlive()) process.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
        }
    }

    @Test @Issue("COA-9665")
    public void accountSwitchDoesNotLeakBookmarks() {
        Assert.assertFalse("Account-switch test requires fixture login",
                Boolean.getBoolean("coach.bookmarks.useCurrentAccount"));
        String first = required("lessonTitle");
        String second = required("secondLessonTitle");
        Assert.assertNotEquals("The two fixture lessons must be distinct", first, second);
        TestData.TestAccount firstAccount = TestData.bookmarksAccount(SWITCH_A);
        TestData.TestAccount secondAccount = TestData.bookmarksAccount(SWITCH_B);
        Assert.assertFalse("COA-9665 requires two distinct fixture accounts",
                firstAccount.getEmail().equalsIgnoreCase(secondAccount.getEmail()));

        prepare(SWITCH_A);
        assertIsolatedBookmarkList(first, second);

        switchTo(SWITCH_B);
        assertIsolatedBookmarkList(second, first);

        switchTo(SWITCH_A);
        assertIsolatedBookmarkList(first, second);
    }

    private void assertIsolatedBookmarkList(String expected, String foreignLesson) {
        String tooltip = optional("tooltipText");
        bookmarks.openFromTodayAllowingTooltip(tooltip);
        assertForeignBookmarkNotVisible(foreignLesson);
        if (tooltip != null) {
            bookmarks.dismissTooltipIfVisible(tooltip);
            bookmarks.assertScreenVisible();
            assertForeignBookmarkNotVisible(foreignLesson);
        }
        bookmarks.assertCard(expected);
        bookmarks.assertExactCardTitles(Collections.singletonList(expected), null);
    }

    private void assertForeignBookmarkNotVisible(String title) {
        Assert.assertFalse("A bookmark from the other account was displayed on opening My Bookmarks",
                bookmarks.hasCard(title));
    }

    @Test @Issue("COA-9666")
    public void bookmarksAndTooltipStateAcrossUpdateAndReinstall() throws Exception {
        InstallVariant variant = bookmarkInstallVariant();
        Assert.assertTrue("Update/reinstall case requires an Appium application-management driver",
                driver instanceof InteractsWithApps);
        InteractsWithApps apps = (InteractsWithApps) driver;
        String bundle = Platform.getInstance().getIOSBundleId();
        String app = Platform.getInstance().getIOSUpdateAppPath();
        String release = optional("testFlightBuild");
        IOSTestFlightPageObject testFlight = release == null ? null : new IOSTestFlightPageObject(driver, bundle, release);
        Assert.assertTrue("Configure an iOS install artifact or the exact TestFlight build",
                testFlight != null || (app != null && !app.trim().isEmpty()));
        Map<String, String> before = UpdateEvidence.readInstalledIOSBuild(driver);
        String udid = System.getProperty("ios.udid", System.getenv("IOS_UDID"));
        Map<String, String> expectedBuild = testFlight != null ? testFlight.expectedBuild()
                : UpdateEvidence.iosInstallArtifact(app, udid.matches("[A-Fa-f0-9-]{36}"));
        assertBookmarksInstallBaseline(variant, before, expectedBuild);
        String lesson = required("lessonTitle");
        String tooltip = required("tooltipText");
        prepare(UPDATE);
        assertInstalledBookmarks(lesson, tooltip, false);
        bookmarks.backFromBookmarks();

        TestData.TestAccount account = TestData.bookmarksAccount(UPDATE);
        if (variant.updates()) {
            runRestoringScenario("iosBookmarksUpdate", () -> {
                apps.terminateApp(bundle);
                if (testFlight == null) apps.installApp(app);
                else testFlight.install(true);
                Assert.assertEquals("iOS update installed an unexpected build", expectedBuild, UpdateEvidence.readInstalledIOSBuild(driver));
                coach.activateAppIfPossible();
                iOSCoachFlowPageObject iosCoach = (iOSCoachFlowPageObject) coach;
                iosCoach.openProfileInExistingSession();
                iosCoach.assertAuthenticatedProfileForEmail(account.getEmail());
                coach.openToday();
                assertInstalledBookmarks(lesson, tooltip, false);
            }, () -> {
                boolean targetInstalled = false;
                try {
                    targetInstalled = apps.isAppInstalled(bundle)
                            && expectedBuild.equals(UpdateEvidence.readInstalledIOSBuild(driver));
                } catch (Exception | AssertionError unreadableBuild) {
                    io.qameta.allure.Allure.addAttachment("Bookmarks update restoration",
                            "Installed build could not be verified; attempting one target-build restoration.");
                }
                if (!targetInstalled) {
                    if (testFlight == null) apps.installApp(app);
                    else testFlight.install(apps.isAppInstalled(bundle));
                    Assert.assertEquals("Could not restore the target Bookmarks build after update",
                            expectedBuild, UpdateEvidence.readInstalledIOSBuild(driver));
                }
            });
        }

        if (variant.reinstalls()) {
            if (testFlight != null) testFlight.verifyOfferedBuild();
            RestoringTestAction.CheckedAction install = testFlight == null
                    ? () -> apps.installApp(app) : () -> testFlight.install(false);
            reinstallIOSBookmarksApp(apps, bundle, install);
            Assert.assertEquals("iOS reinstall installed an unexpected build", expectedBuild, UpdateEvidence.readInstalledIOSBuild(driver));
            coach.activateAppIfPossible();
            coach.ensureLoggedOutOnStartScreen();
            login(account);
            assertInstalledBookmarks(lesson, tooltip, true);
        }
    }

    private void reinstallIOSBookmarksApp(InteractsWithApps apps, String bundle,
                                          RestoringTestAction.CheckedAction install) throws Exception {
        apps.terminateApp(bundle);
        try {
            Assert.assertTrue("Cannot remove iOS app before reinstall", apps.removeApp(bundle));
            Assert.assertFalse("iOS app remained installed before reinstall", apps.isAppInstalled(bundle));
            install.run();
        } catch (Exception | AssertionError failure) {
            captureFailureArtifacts(failure, Description.createTestDescription(getClass(), "iosBookmarksReinstallBeforeRecovery"));
            try { if (!apps.isAppInstalled(bundle)) install.run(); }
            catch (Exception | AssertionError recovery) { failure.addSuppressed(recovery); }
            throw failure;
        }
    }

    protected enum InstallVariant {
        UPDATE, REINSTALL, BOTH;

        protected boolean updates() { return this != REINSTALL; }
        protected boolean reinstalls() { return this != UPDATE; }
    }

    protected InstallVariant bookmarkInstallVariant() {
        Assert.assertFalse("Update/reinstall needs verified fixture login", Boolean.getBoolean("coach.bookmarks.useCurrentAccount"));
        String value = optional("installVariant");
        if (value == null) return InstallVariant.BOTH;
        Assert.assertTrue("Use update or reinstall for coach.bookmarks.installVariant",
                "update".equals(value) || "reinstall".equals(value));
        return InstallVariant.valueOf(value.toUpperCase(Locale.ROOT));
    }

    protected void assertBookmarksInstallBaseline(InstallVariant variant, Map<String, String> before, Map<String, String> intended) {
        if (variant.updates()) UpdateEvidence.assertNewerBuild(before.get("build"), intended.get("build"));
        else Assert.assertEquals("Reinstall needs the prepared installation of the requested build", intended, before);
        UpdateEvidence.attachBuild(variant.name() + " baseline", before.get("version"), before.get("build"));
        UpdateEvidence.attachBuild(variant.name() + " requested", intended.get("version"), intended.get("build"));
    }

    protected void assertInstalledBookmarks(String lesson, String tooltip, boolean tooltipShouldReappear) {
        if (tooltipShouldReappear) {
            bookmarks.tapEntryFromToday();
            bookmarks.assertTooltipVisible(tooltip);
            bookmarks.dismissTooltipIfVisible(tooltip);
            bookmarks.closeOpenedContentIfPresent();
        }
        bookmarks.openFromToday();
        bookmarks.assertTooltipAbsent(tooltip);
        bookmarks.assertCard(lesson);
    }

    @Test @Issue("COA-9667")
    @io.qameta.allure.Description("Save an available lesson from Today. The first lesson is accessible from Today on the base fixture. Opening its saved card shows the subscription paywall. An active subscription or trial opens the saved lesson at its saved slide.")
    public void availableLessonCanBeBookmarkedWithoutSubscription() throws Exception {
        String accessVariant = optional("accessVariant");
        String defaultRole = NO_SUBSCRIPTION;
        if ("active-subscription".equals(accessVariant)) {
            defaultRole = "ACTIVESUBSCRIPTION";
        } else if ("active-trial".equals(accessVariant)) {
            defaultRole = "ACTIVETRIAL";
        } else {
            Assert.assertNull("Use active-subscription or active-trial for coach.bookmarks.accessVariant", accessVariant);
        }
        prepare(fixtureRole("availableAccountRole", defaultRole));
        if (accessVariant != null) {
            coach.openProfile();
            new lib.ui.ios.iOSSubscriptionPageObject(driver).assertPremiumAccessIsDisplayed();
            coach.openToday();
        }
        io.qameta.allure.Allure.addAttachment("Bookmarks available-content access variant",
                accessVariant == null ? "base/no-subscription fixture; content must be available before Save" : accessVariant);
        String lesson = required("lessonTitle");
        String savedSlide = required("lessonSavedSlideText");
        String tooltip = required("tooltipText");
        List<String> initialCards = configuredCardRoster("availableInitialCards");
        Assert.assertFalse("The selected lesson must not be bookmarked in the starting fixture", initialCards.contains(lesson));
        List<String> savedCards = new ArrayList<>(initialCards);
        savedCards.add(lesson);
        boolean[] saveAttempted = {false};
        runRestoringScenario("availableLessonBookmark", () -> {
            bookmarks.openFromToday();
            bookmarks.assertTooltipAbsent(tooltip);
            assertBookmarkRoster(initialCards);
            bookmarks.backFromBookmarks();
            openFixtureContentFromToday(lesson);
            bookmarks.assertOpenedContentTitle(lesson);
            bookmarks.assertNoPaywall();
            bookmarks.assertOpenedContentNotSaved();
            bookmarks.swipeLeftToSlide(savedSlide);
            bookmarks.assertOpenedContentNotSaved();
            saveAttempted[0] = true;
            bookmarks.saveOpenedContent();
            bookmarks.closeOpenedContent();
            assertBookmarkRoster(savedCards);
            if (accessVariant == null) {
                bookmarks.openCardExpectingPaywall(lesson);
            } else {
                bookmarks.openCard(lesson);
                bookmarks.assertNoPaywall();
                bookmarks.assertOpenedAtSlide(lesson, savedSlide);
                bookmarks.assertOpenedContentSaved();
            }
        }, () -> {
            if (saveAttempted[0]) {
                coach.activateAppIfPossible();
                bookmarks.closeOpenedContentIfPresent();
                openFixtureContentFromToday(lesson);
                bookmarks.assertOpenedContentTitle(lesson);
                bookmarks.ensureOpenedContentIsNotSaved();
                bookmarks.closeOpenedContentIfPresent();
                assertBookmarkRoster(initialCards);
            }
        });
    }

    private void verifyWholeContentBookmark(String title, String firstSlide, String middleSlide,
                                            String lastSlide, String savedSlide) throws Exception {
        runRestoringScenario("wholeContentBookmark", () -> {
            for (String saveAt : new String[]{firstSlide, middleSlide, lastSlide}) {
                openFixtureContentFromToday(title);
                bookmarks.ensureOpenedContentIsNotSaved(); // B0 preparation, before the assertions.
                bookmarks.swipeRightAndAssertNotSaved(firstSlide);
                bookmarks.swipeLeftAndAssertNotSaved(lastSlide);
                bookmarks.swipeRightAndAssertNotSaved(firstSlide);
                bookmarks.swipeLeftToSlide(saveAt);
                bookmarks.saveOpenedContent();
                bookmarks.swipeRightAndAssertSaved(firstSlide);
                bookmarks.swipeLeftAndAssertSaved(middleSlide);
                bookmarks.swipeLeftAndAssertSaved(lastSlide);
                bookmarks.swipeRightAndAssertSaved(firstSlide);
                bookmarks.closeOpenedContent();
                bookmarks.openFromToday();
                bookmarks.assertSingleCard(title, firstSlide, middleSlide, lastSlide);

                // A fresh Daily Plan reader must show the state persisted on all slides.
                bookmarks.backFromBookmarks();
                openFixtureContentFromToday(title);
                bookmarks.swipeRightAndAssertSaved(firstSlide);
                bookmarks.swipeLeftAndAssertSaved(lastSlide);
                bookmarks.swipeRightAndAssertSaved(firstSlide);
                bookmarks.removeOpenedContent();
                bookmarks.closeOpenedContent();
                bookmarks.openFromToday();
                bookmarks.assertCardAbsent(title);
                bookmarks.backFromBookmarks();
            }
        }, () -> prepareSavedContent(title, firstSlide, savedSlide));
    }

    private void prepareSavedContent(String title, String firstSlideMarker, String slideMarker) {
        bookmarks.closeOpenedContentIfPresent();
        openFixtureContentFromToday(title);
        bookmarks.ensureOpenedContentIsNotSaved();
        bookmarks.swipeRightAndAssertNotSaved(firstSlideMarker);
        bookmarks.swipeLeftToSlide(slideMarker);
        bookmarks.saveOpenedContent();
        bookmarks.closeOpenedContent();
    }

    protected void openFixtureContentFromToday(String title) {
        if (bookmarks.isScreenVisible()) bookmarks.backFromBookmarks();
        coach.openToday();
        String program;
        if (title.equals(optional("completionLessonTitle"))) {
            program = optional("completionProgramTitle");
        } else if (title.equals(optional("practiceTitle"))) {
            program = optional("practiceProgramTitle");
        } else {
            program = optional("lessonProgramTitle");
        }
        selectFixtureProgram(program);
        selectFixtureContentDay(optional("fixtureContentDay"));
        bookmarks.openContentFromToday(title);
    }

    protected void selectFixtureContentDay(String day) {
        if (day != null) {
            Assert.assertTrue("Fixture content day must be a positive number", day.matches("[1-9][0-9]*"));
            iOSProgramSelectorPageObject today = new iOSProgramSelectorPageObject(driver);
            String current = today.getCurrentDayLabel().trim();
            Assert.assertTrue("Unreadable fixture day: " + current, current.matches("(Day|Stage) [0-9]+( of [0-9]+)?"));
            today.selectObservedDay(current.replaceFirst("[0-9]+", day));
        }
    }

    protected void selectFixtureProgram(String program) {
        if (program != null) new iOSProgramSelectorPageObject(driver).ensureProgramSelected(program);
    }

    private static String secondContentTitle() {
        String title = System.getProperty("coach.bookmarks.secondContentTitle");
        if (title == null || title.trim().isEmpty()) title = System.getenv("COACH_BOOKMARKS_SECOND_CONTENT_TITLE");
        return title == null || title.trim().isEmpty() ? required("secondLessonTitle") : title.trim();
    }

    private static String fixtureRole(String field, String fallback) {
        String role = optional(field);
        return role == null ? fallback : role;
    }

    private static String recurring(String suffix) {
        String field = "recurring" + Character.toUpperCase(suffix.charAt(0)) + suffix.substring(1);
        String env = "COACH_BOOKMARKS_" + field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        String value = System.getProperty("coach.bookmarks." + field);
        if (value == null || value.trim().isEmpty()) value = System.getenv(env);
        return value == null || value.trim().isEmpty() ? required(suffix) : value.trim();
    }

    private static String optional(String suffix) {
        String env = "COACH_BOOKMARKS_" + suffix.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        String value = System.getProperty("coach.bookmarks." + suffix);
        if (value == null || value.trim().isEmpty()) value = System.getenv(env);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    protected static String required(String suffix) {
        String property = "coach.bookmarks." + suffix;
        String env = "COACH_BOOKMARKS_" + suffix.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toUpperCase(Locale.ROOT);
        String value = System.getProperty(property);
        if (value == null || value.trim().isEmpty()) value = System.getenv(env);
        Assert.assertTrue("Configure -D" + property + " or " + env,
                value != null && !value.trim().isEmpty());
        return value.trim();
    }
}
