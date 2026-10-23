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
import lib.ui.ios.iOSCoachFlowPageObject;
import lib.ui.ios.iOSProgramSelectorPageObject;
import lib.ui.factories.BookmarksPageObjectFactory;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;
import java.util.Map;

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
        prepare(CONTENT);
        String lesson = required("lessonTitle");
        bookmarks.openFromToday();
        bookmarks.assertCard(lesson);
        bookmarks.backFromBookmarks();
        bookmarks.openFromExplore();
        bookmarks.assertCard(lesson);
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
        String lesson = required("lessonTitle");
        RestoringTestAction.run(() -> {
            openFixtureContentFromToday(lesson);
            bookmarks.ensureOpenedContentIsNotSaved();
            bookmarks.saveOpenedContent();
            bookmarks.swipeLeftAndAssertSaved(required("lessonMiddleSlideText"));
            bookmarks.removeOpenedContent();
            bookmarks.closeOpenedContent();
            bookmarks.openFromToday();
            bookmarks.assertCardAbsent(lesson);
            bookmarks.backFromBookmarks();
            openFixtureContentFromToday(lesson);
            bookmarks.saveOpenedContent();
            bookmarks.closeOpenedContent();
            bookmarks.openFromToday();
            bookmarks.assertCard(lesson);
        }, () -> prepareSavedContent(lesson, required("lessonSavedSlideText")));
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
        prepare(BASELINE);
        String tooltip = required("tooltipText");
        String lesson = required("lessonTitle");
        boolean[] createdByTest = {false};
        RestoringTestAction.run(() -> {
            bookmarks.openFromToday();
            bookmarks.assertEmptyState();
            bookmarks.assertTooltipAbsent(tooltip);
            bookmarks.backFromBookmarks();
            openFixtureContentFromToday(required("lessonTitle"));
            bookmarks.ensureOpenedContentIsNotSaved();
            bookmarks.saveOpenedContent();
            createdByTest[0] = true;
            bookmarks.closeOpenedContent();
            bookmarks.openFromToday();
            bookmarks.assertTooltipVisible(tooltip);
        }, () -> {
            if (createdByTest[0]) {
                bookmarks.closeOpenedContentIfPresent();
                openFixtureContentFromToday(lesson);
                bookmarks.ensureOpenedContentIsNotSaved();
                bookmarks.closeOpenedContent();
            }
        });
    }

    @Test @Issue("COA-9653")
    public void anyTapDismissesTooltipAndItDoesNotReturn() {
        String tooltip = required("tooltipText");

        prepare(TOOLTIP);
        bookmarks.openFromToday();
        bookmarks.assertTooltipVisible(tooltip);
        bookmarks.dismissTooltipByBlankArea(tooltip);
        bookmarks.reopenAndAssertTooltipAbsent(tooltip);
    }

    @Test @Issue("COA-9654")
    public void cancelKeepsSelectedBookmark() {
        prepare(CONTENT);
        String first = required("lessonTitle");
        String second = secondContentTitle();
        bookmarks.openFromToday();
        bookmarks.openRemovalDialog(first);
        bookmarks.cancelRemoval();
        bookmarks.assertCard(first);
        bookmarks.assertCard(second);
        bookmarks.openCard(first);
        bookmarks.assertOpenedContentSaved();
    }

    @Test @Issue("COA-9655")
    public void removeDeletesOnlySelectedBookmarkAndSyncsContent() throws Exception {
        prepare(CONTENT);
        String first = required("lessonTitle");
        String second = secondContentTitle();
        RestoringTestAction.run(() -> {
            bookmarks.openFromToday();
            bookmarks.openRemovalDialog(first);
            bookmarks.confirmRemoval();
            bookmarks.assertCardAbsent(first);
            bookmarks.assertCard(second);
            bookmarks.backFromBookmarks();
            openFixtureContentFromToday(first);
            bookmarks.ensureOpenedContentIsNotSaved();
            bookmarks.saveOpenedContent();
            bookmarks.closeOpenedContent();
            bookmarks.openFromToday();
            bookmarks.assertCard(first);
        }, () -> prepareSavedContent(first, required("lessonSavedSlideText")));
    }

    @Test @Issue("COA-9656")
    public void deletingLastBookmarkShowsEmptyState() throws Exception {
        String tooltip = required("tooltipText");
        String lesson = required("lessonTitle");
        prepare(UPDATE);
        RestoringTestAction.run(() -> {
            bookmarks.openFromToday();
            bookmarks.openRemovalDialog(lesson);
            bookmarks.confirmRemoval();
            bookmarks.assertEmptyState();
            bookmarks.assertTooltipAbsent(tooltip);
        }, () -> prepareSavedContent(lesson, required("lessonFirstSlideText")));
    }

    @Test @Issue("COA-9657")
    public void cardsOpenLessonAndPracticeAtSavedSlides() {
        prepare(CONTENT);
        String lesson = required("lessonTitle");
        prepareSavedContent(lesson, required("lessonSavedSlideText"));
        prepareSavedContent(required("practiceTitle"), required("practiceSavedSlideText"));
        bookmarks.openFromToday();
        bookmarks.openCard(lesson);
        bookmarks.assertOpenedAtSlide(lesson, required("lessonSavedSlideText"));
        bookmarks.closeOpenedContent();
        bookmarks.openFromToday();
        String practice = required("practiceTitle");
        bookmarks.openCard(practice);
        bookmarks.assertOpenedAtSlide(practice, required("practiceSavedSlideText"));
    }

    @Test @Issue("COA-9658")
    public void finishReturnsToBookmarksWithoutFeedback() {
        prepare(CONTENT);
        prepareSavedContent(required("lessonTitle"), required("lessonSavedSlideText"));
        bookmarks.openFromToday();
        bookmarks.openCard(required("lessonTitle"));
        bookmarks.finishFromBookmarks(optional("lessonCompletionAnswer"));
    }

    @Test @Issue("COA-9659")
    public void finishMarksLessonCompletedInDailyPlan() {
        String completionLesson = optional("completionLessonTitle");
        String lesson = completionLesson == null ? required("lessonTitle") : completionLesson;
        String savedSlide = required(completionLesson == null
                ? "lessonSavedSlideText" : "completionLessonSavedSlideText");
        String answer = optional("completionAnswer");
        if (answer == null && completionLesson == null) answer = optional("lessonCompletionAnswer");
        prepare(fixtureRole("completionAccountRole", CONTENT));
        String program = optional("completionProgramTitle");
        selectFixtureProgram(program);
        bookmarks.assertDailyPlanItemNotCompleted(lesson);
        prepareSavedContent(lesson, savedSlide);
        bookmarks.openFromToday();
        bookmarks.openCard(lesson);
        bookmarks.finishFromBookmarks(answer);
        bookmarks.assertDailyPlanItemCompleted(lesson);
    }

    @Test @Issue("COA-9660")
    public void removingBookmarkDoesNotResetCompletedLesson() throws Exception {
        prepare(CONTENT);
        String lesson = required("lessonTitle");
        RestoringTestAction.run(() -> {
            prepareSavedContent(lesson, required("lessonSavedSlideText"));
            bookmarks.openFromToday();
            bookmarks.openCard(lesson);
            bookmarks.finishFromBookmarks(optional("lessonCompletionAnswer"));
            bookmarks.openRemovalDialog(lesson);
            bookmarks.confirmRemoval();
            bookmarks.assertDailyPlanItemCompleted(lesson);
        }, () -> prepareSavedContent(lesson, required("lessonSavedSlideText")));
    }

    @Test @Issue("COA-9661")
    public void kegelAndKegelSportCannotBeBookmarked() {
        prepare(fixtureRole("kegelAccountRole", CONTENT));
        selectFixtureProgram(optional("kegelProgramTitle"));
        bookmarks.openContentWithoutBookmarkFromToday(required("kegelTitle"));
        bookmarks.assertOpenedContentCannotBeSaved();
        bookmarks.closeOpenedContent();
        bookmarks.openContentWithoutBookmarkFromToday(required("kegelSportTitle"));
        bookmarks.assertOpenedContentCannotBeSaved();
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
    public void iosSaveSendsOriginalProgramAndDay() {
        prepare(RECURRING);
        String network = TestData.bookmarksNetworkLogPath();
        long checkpoint = BookmarksEvidence.checkpoint(network);
        selectFixtureProgram(optional("recurringProgramTitle"));
        bookmarks.openContentFromToday(required("recurringPracticeTitle"));
        bookmarks.ensureOpenedContentIsNotSaved();
        bookmarks.swipeLeftToSlide(recurring("practiceSavedSlideText"));
        bookmarks.saveOpenedContent();
        BookmarksEvidence.assertBookmarkRequest(network, checkpoint, recurring("lessonId"),
                recurring("programId"), Integer.parseInt(recurring("programDay")),
                Integer.parseInt(recurring("savedSlideNumber")));
    }

    @Test @Issue("COA-9664")
    public void recurringPracticeFinishCompletesSavedDay() throws Exception {
        prepare(RECURRING);
        String practice = required("recurringPracticeTitle");
        String savedDay = required("savedDayTitle");
        String previousDay = required("previousDayTitle");
        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        dailyPlan.assertCurrentDayMatches(savedDay);
        bookmarks.assertRecurringPreviousDayFixture(practice, savedDay, previousDay);
        bookmarks.openFromToday();
        bookmarks.openCard(practice);
        bookmarks.finishFromBookmarks(optional("recurringCompletionAnswer"));
        dailyPlan.assertCurrentDayMatches(savedDay);
        bookmarks.assertDailyPlanItemCompleted(practice);
        bookmarks.assertPreviousDayItemNotCompleted(practice, previousDay);
    }

    @Test @Issue("COA-9665")
    public void accountSwitchDoesNotLeakBookmarks() {
        prepare(SWITCH_A);
        String first = required("lessonTitle");
        String second = required("secondLessonTitle");
        bookmarks.openFromToday();
        bookmarks.assertCard(first);
        bookmarks.assertCardAbsent(second);

        switchTo(SWITCH_B);
        bookmarks.openFromToday();
        bookmarks.assertCard(second);
        bookmarks.assertCardAbsent(first);
    }

    @Test @Issue("COA-9666")
    public void bookmarksAndTooltipStateAcrossUpdateAndReinstall() throws Exception {
        prepare(UPDATE);
        Assert.assertTrue("Update/reinstall case requires an Appium application-management driver",
                driver instanceof InteractsWithApps);
        String app = Platform.getInstance().getIOSUpdateAppPath();
        Assert.assertTrue("Configure ios.update.app or IOS_UPDATE_APP", app != null && !app.trim().isEmpty());
        Map<String, Object> expectedBuild = null;
        if (app.endsWith(".app")) {
            Map<String, Object> installedBuild = UpdateEvidence.installedIOSBuild();
            expectedBuild = UpdateEvidence.iosBuild(app);
            Assert.assertNotEquals("Bookmarks update requires two different installed build numbers",
                    installedBuild.get("CFBundleVersion"), expectedBuild.get("CFBundleVersion"));
        }
        String lesson = required("lessonTitle");
        String tooltip = required("tooltipText");
        bookmarks.openFromToday();
        bookmarks.assertCard(lesson);
        bookmarks.assertTooltipAbsent(tooltip);
        bookmarks.backFromBookmarks();

        InteractsWithApps apps = (InteractsWithApps) driver;
        apps.terminateApp(Platform.getInstance().getIOSBundleId());
        apps.installApp(app);
        if (expectedBuild != null) {
            Assert.assertEquals("Bookmarks update did not install the requested build",
                    expectedBuild.get("CFBundleVersion"), UpdateEvidence.installedIOSBuild().get("CFBundleVersion"));
        }
        coach.activateAppIfPossible();
        coach.assertAuthorizedDashboardIsDisplayed();
        bookmarks.openFromToday();
        bookmarks.assertCard(lesson);
        bookmarks.assertTooltipAbsent(tooltip);

        apps.terminateApp(Platform.getInstance().getIOSBundleId());
        Assert.assertTrue("Cannot remove iOS app before reinstall",
                apps.removeApp(Platform.getInstance().getIOSBundleId()));
        apps.installApp(app);
        coach.activateAppIfPossible();
        login(TestData.bookmarksAccount(UPDATE));
        bookmarks.openFromToday();
        bookmarks.assertCard(lesson);
        bookmarks.assertTooltipVisible(tooltip);
    }

    @Test @Issue("COA-9667")
    public void availableLessonCanBeBookmarkedWithoutSubscription() throws Exception {
        prepare(NO_SUBSCRIPTION);
        String lesson = required("lessonTitle");
        boolean[] createdByTest = {false};
        RestoringTestAction.run(() -> {
            bookmarks.openFromToday();
            bookmarks.assertEmptyState();
            bookmarks.backFromBookmarks();
            openFixtureContentFromToday(lesson);
            bookmarks.assertNoPaywall();
            bookmarks.ensureOpenedContentIsNotSaved();
            bookmarks.swipeLeftToSlide(required("lessonSavedSlideText"));
            bookmarks.saveOpenedContent();
            createdByTest[0] = true;
            bookmarks.closeOpenedContent();
            bookmarks.openFromToday();
            bookmarks.assertCard(lesson);
            bookmarks.openCard(lesson);
            bookmarks.assertNoPaywall();
            bookmarks.assertOpenedAtSlide(lesson, required("lessonSavedSlideText"));
            bookmarks.assertOpenedContentSaved();
        }, () -> {
            if (createdByTest[0]) {
                bookmarks.closeOpenedContentIfPresent();
                bookmarks.openFromToday();
                bookmarks.openRemovalDialog(lesson);
                bookmarks.confirmRemoval();
                bookmarks.assertEmptyState();
            }
        });
    }

    private void verifyWholeContentBookmark(String title, String firstSlide, String middleSlide,
                                            String lastSlide, String savedSlide) throws Exception {
        RestoringTestAction.run(() -> {
            // Save on the first slide and verify the state on every slide.
            openFixtureContentFromToday(title);
            bookmarks.ensureOpenedContentIsNotSaved();
            bookmarks.saveOpenedContent();
            bookmarks.swipeLeftAndAssertSaved(middleSlide);
            bookmarks.swipeLeftAndAssertSaved(lastSlide);
            bookmarks.swipeRightAndAssertSaved(middleSlide);
            bookmarks.swipeRightAndAssertSaved(firstSlide);
            bookmarks.removeOpenedContent();
            bookmarks.closeOpenedContent();

            // Save on the middle slide and verify both directions.
            openFixtureContentFromToday(title);
            bookmarks.ensureOpenedContentIsNotSaved();
            bookmarks.swipeLeftToSlide(middleSlide);
            bookmarks.saveOpenedContent();
            bookmarks.swipeRightAndAssertSaved(firstSlide);
            bookmarks.swipeLeftAndAssertSaved(middleSlide);
            bookmarks.swipeLeftAndAssertSaved(lastSlide);
            bookmarks.removeOpenedContent();
            bookmarks.closeOpenedContent();

            // Save on the last slide and verify the earlier slides.
            openFixtureContentFromToday(title);
            bookmarks.ensureOpenedContentIsNotSaved();
            bookmarks.swipeLeftToSlide(middleSlide);
            bookmarks.swipeLeftToSlide(lastSlide);
            bookmarks.saveOpenedContent();
            bookmarks.swipeRightAndAssertSaved(middleSlide);
            bookmarks.swipeRightAndAssertSaved(firstSlide);
            bookmarks.removeOpenedContent();
            bookmarks.closeOpenedContent();

            bookmarks.openFromToday();
            bookmarks.assertCardAbsent(title);
            bookmarks.backFromBookmarks();
        }, () -> prepareSavedContent(title, savedSlide));
    }

    private void prepareSavedContent(String title, String slideMarker) {
        bookmarks.closeOpenedContentIfPresent();
        openFixtureContentFromToday(title);
        bookmarks.ensureOpenedContentIsNotSaved();
        bookmarks.swipeLeftToSlide(slideMarker);
        bookmarks.saveOpenedContent();
        bookmarks.closeOpenedContent();
    }

    private void openFixtureContentFromToday(String title) {
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
        bookmarks.openContentFromToday(title);
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
