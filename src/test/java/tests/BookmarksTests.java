package tests;

import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import lib.BookmarksEvidence;
import lib.CoreTestCase;
import lib.Platform;
import lib.TestData;
import lib.ui.BookmarksPageObject;
import lib.ui.CoachFlowPageObject;
import lib.ui.DailyPlanPageObject;
import lib.ui.SubscriptionPageObject;
import lib.ui.factories.BookmarksPageObjectFactory;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.factories.DailyPlanPageObjectFactory;
import lib.ui.factories.SubscriptionPageObjectFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

@Epic("Daily Plan")
@Feature("Bookmarks")
public class BookmarksTests extends CoreTestCase {
    private static final String BASELINE = "BASELINE";
    private static final String CONTENT = "CONTENT";
    private static final String TOOLTIP = "TOOLTIP";
    private static final String RECURRING = "RECURRING";
    private static final String SWITCH_A = "SWITCHA";
    private static final String SWITCH_B = "SWITCHB";
    private static final String UPDATE = "UPDATE";
    private static final String NO_SUBSCRIPTION = "NOSUBSCRIPTION";

    private CoachFlowPageObject coach;
    private BookmarksPageObject bookmarks;

    private void prepare(String fixture) {
        Assert.assertTrue("Bookmarks Xray cases are currently scoped to iOS", Platform.getInstance().isIOS());
        coach = CoachFlowPageObjectFactory.get(driver);
        bookmarks = BookmarksPageObjectFactory.get(driver);
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

    private void login(TestData.TestAccount account) {
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
    }

    private void switchTo(String fixture) {
        Assert.assertFalse("Multi-account Bookmarks cases cannot use coach.bookmarks.useCurrentAccount",
                Boolean.getBoolean("coach.bookmarks.useCurrentAccount"));
        coach.openToday();
        coach.openProfile();
        coach.logOut();
        login(TestData.bookmarksAccount(fixture));
    }

    @Override
    protected void cleanupSessionForNextTest() {
        if (!Boolean.getBoolean("coach.bookmarks.useCurrentAccount")) super.cleanupSessionForNextTest();
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
    public void saveFromAnySlideSynchronizesWholeLessonAndPractice() {
        prepare(CONTENT);
        verifyWholeContentBookmark(required("lessonTitle"), required("lessonFirstSlideText"),
                required("lessonMiddleSlideText"),
                required("lessonLastSlideText"));
        verifyWholeContentBookmark(required("practiceTitle"), required("practiceFirstSlideText"),
                required("practiceMiddleSlideText"),
                required("practiceLastSlideText"));
    }

    @Test @Issue("COA-9650")
    public void removeInsideContentAndSaveAgain() {
        prepare(CONTENT);
        String lesson = required("lessonTitle");
        bookmarks.openContentFromToday(lesson);
        bookmarks.ensureOpenedContentIsNotSaved();
        bookmarks.saveOpenedContent();
        bookmarks.swipeLeftAndAssertSaved(required("lessonMiddleSlideText"));
        bookmarks.removeOpenedContent();
        bookmarks.closeOpenedContent();
        bookmarks.openFromToday();
        bookmarks.assertCardAbsent(lesson);
        bookmarks.backFromBookmarks();
        bookmarks.openContentFromToday(lesson);
        bookmarks.saveOpenedContent();
        bookmarks.closeOpenedContent();
        bookmarks.openFromToday();
        bookmarks.assertCard(lesson);
    }

    @Test @Issue("COA-9651")
    public void cardsAreGroupedAndContainExpectedFields() {
        prepare(CONTENT);
        String lesson = required("lessonTitle");
        String practice = required("practiceTitle");
        bookmarks.openFromToday();
        bookmarks.assertGroupAndCategory(required("programTitle"), "Lessons");
        bookmarks.assertCardComposition(lesson);
        bookmarks.assertGroupAndCategory(required("moduleTitle"), "Practices");
        bookmarks.assertCardComposition(practice);
        bookmarks.assertCategoryAbsent(required("emptyCategoryTitle"));
    }

    @Test @Issue("COA-9652")
    public void tooltipWaitsForFirstNonEmptyEntry() {
        prepare(BASELINE);
        String tooltip = required("tooltipText");
        bookmarks.openFromToday();
        bookmarks.assertEmptyState();
        bookmarks.assertTooltipAbsent(tooltip);
        bookmarks.backFromBookmarks();
        bookmarks.openContentFromToday(required("lessonTitle"));
        bookmarks.ensureOpenedContentIsNotSaved();
        bookmarks.saveOpenedContent();
        bookmarks.closeOpenedContent();
        bookmarks.openFromToday();
        bookmarks.assertTooltipVisible(tooltip);
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
        String second = required("secondLessonTitle");
        bookmarks.openFromToday();
        bookmarks.openRemovalDialog(first);
        bookmarks.cancelRemoval();
        bookmarks.assertCard(first);
        bookmarks.assertCard(second);
        bookmarks.openCard(first);
        bookmarks.assertOpenedContentSaved();
    }

    @Test @Issue("COA-9655")
    public void removeDeletesOnlySelectedBookmarkAndSyncsContent() {
        prepare(CONTENT);
        String first = required("lessonTitle");
        String second = required("secondLessonTitle");
        bookmarks.openFromToday();
        bookmarks.openRemovalDialog(first);
        bookmarks.confirmRemoval();
        bookmarks.assertCardAbsent(first);
        bookmarks.assertCard(second);
        bookmarks.backFromBookmarks();
        bookmarks.openContentFromToday(first);
        bookmarks.ensureOpenedContentIsNotSaved();
        bookmarks.saveOpenedContent();
        bookmarks.closeOpenedContent();
        bookmarks.openFromToday();
        bookmarks.assertCard(first);
    }

    @Test @Issue("COA-9656")
    public void deletingLastBookmarkShowsEmptyState() {
        String tooltip = required("tooltipText");
        String lesson = required("lessonTitle");
        prepare(UPDATE);
        bookmarks.openFromToday();
        bookmarks.openRemovalDialog(lesson);
        bookmarks.confirmRemoval();
        bookmarks.assertEmptyState();
        bookmarks.assertTooltipAbsent(tooltip);

        // Restore the shared update fixture so this case remains safe for the other update checks.
        bookmarks.backFromBookmarks();
        bookmarks.openContentFromToday(lesson);
        bookmarks.ensureOpenedContentIsNotSaved();
        bookmarks.saveOpenedContent();
        bookmarks.closeOpenedContent();
    }

    @Test @Issue("COA-9657")
    public void cardsOpenLessonAndPracticeAtSavedSlides() {
        prepare(CONTENT);
        bookmarks.openFromToday();
        String lesson = required("lessonTitle");
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
        bookmarks.openFromToday();
        bookmarks.openCard(required("lessonTitle"));
        bookmarks.finishFromBookmarks();
    }

    @Test @Issue("COA-9659")
    public void finishMarksLessonCompletedInDailyPlan() {
        prepare(CONTENT);
        String lesson = required("lessonTitle");
        bookmarks.openFromToday();
        bookmarks.openCard(lesson);
        bookmarks.finishFromBookmarks();
        bookmarks.assertDailyPlanItemCompleted(lesson);
    }

    @Test @Issue("COA-9660")
    public void removingBookmarkDoesNotResetCompletedLesson() {
        prepare(CONTENT);
        String lesson = required("lessonTitle");
        bookmarks.openFromToday();
        bookmarks.openCard(lesson);
        bookmarks.finishFromBookmarks();
        bookmarks.openRemovalDialog(lesson);
        bookmarks.confirmRemoval();
        bookmarks.assertDailyPlanItemCompleted(lesson);
    }

    @Test @Issue("COA-9661")
    public void kegelAndKegelSportCannotBeBookmarked() {
        prepare(CONTENT);
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
        bookmarks.openContentFromToday(lesson);
        bookmarks.ensureOpenedContentIsNotSaved();
        bookmarks.saveOpenedContent();
        bookmarks.removeOpenedContent();
        BookmarksEvidence.assertAppendedContains(analytics, checkpoint,
                "BookmarksOpened", "BookmarkSaved", "BookmarkDeleted",
                required("lessonId"), required("savedSlideNumber"));
    }

    @Test @Issue("COA-9663")
    public void iosSaveSendsOriginalProgramAndDay() {
        prepare(RECURRING);
        String network = TestData.bookmarksNetworkLogPath();
        long checkpoint = BookmarksEvidence.checkpoint(network);
        bookmarks.openContentFromToday(required("recurringPracticeTitle"));
        bookmarks.ensureOpenedContentIsNotSaved();
        bookmarks.swipeLeftToSlide(required("practiceSavedSlideText"));
        bookmarks.saveOpenedContent();
        BookmarksEvidence.assertAppendedContains(network, checkpoint,
                "POST", "/api/v1/coachprogram/lessons/" + required("lessonId") + "/bookmark",
                "slide_number", required("savedSlideNumber"),
                "program_id", required("programId"),
                "program_day", required("programDay"));
    }

    @Test @Issue("COA-9664")
    public void recurringPracticeFinishCompletesSavedDay() {
        prepare(RECURRING);
        String practice = required("recurringPracticeTitle");
        bookmarks.openFromToday();
        bookmarks.openCard(practice);
        bookmarks.finishFromBookmarks();
        DailyPlanPageObject dailyPlan = DailyPlanPageObjectFactory.get(driver);
        dailyPlan.assertCurrentDayMatches(required("savedDayTitle"));
        bookmarks.assertDailyPlanItemCompleted(practice);
        bookmarks.assertPreviousDayItemNotCompleted(practice, required("previousDayTitle"));
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
    public void bookmarksAndTooltipStateAcrossUpdateAndReinstall() {
        prepare(UPDATE);
        Assert.assertTrue("Update/reinstall case requires an Appium application-management driver",
                driver instanceof InteractsWithApps);
        String app = Platform.getInstance().getIOSUpdateAppPath();
        Assert.assertTrue("Configure ios.update.app or IOS_UPDATE_APP", app != null && !app.trim().isEmpty());
        String lesson = required("lessonTitle");
        String tooltip = required("tooltipText");
        bookmarks.openFromToday();
        bookmarks.assertCard(lesson);
        bookmarks.assertTooltipAbsent(tooltip);
        bookmarks.backFromBookmarks();

        InteractsWithApps apps = (InteractsWithApps) driver;
        apps.terminateApp(Platform.getInstance().getIOSBundleId());
        apps.installApp(app);
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
    public void newUserWithoutSubscriptionSeesPaywallWhenOpeningBookmarks() {
        prepare(NO_SUBSCRIPTION);
        bookmarks.tapEntryFromToday();

        SubscriptionPageObject paywall = SubscriptionPageObjectFactory.get(driver);
        Assert.assertNotNull("Subscription page object is unavailable for current platform", paywall);
        paywall.assertPaywallIsDisplayed();
        Assert.assertFalse("My Bookmarks opened behind the subscription paywall", bookmarks.isScreenVisible());
    }

    private void verifyWholeContentBookmark(String title, String firstSlide, String middleSlide, String lastSlide) {
        // Save on the first slide and verify the state on every slide.
        bookmarks.openContentFromToday(title);
        bookmarks.ensureOpenedContentIsNotSaved();
        bookmarks.saveOpenedContent();
        bookmarks.swipeLeftAndAssertSaved(middleSlide);
        bookmarks.swipeLeftAndAssertSaved(lastSlide);
        bookmarks.swipeRightAndAssertSaved(middleSlide);
        bookmarks.swipeRightAndAssertSaved(firstSlide);
        bookmarks.removeOpenedContent();
        bookmarks.closeOpenedContent();

        // Save on the middle slide and verify both directions.
        bookmarks.openContentFromToday(title);
        bookmarks.ensureOpenedContentIsNotSaved();
        bookmarks.swipeLeftToSlide(middleSlide);
        bookmarks.saveOpenedContent();
        bookmarks.swipeRightAndAssertSaved(firstSlide);
        bookmarks.swipeLeftAndAssertSaved(middleSlide);
        bookmarks.swipeLeftAndAssertSaved(lastSlide);
        bookmarks.removeOpenedContent();
        bookmarks.closeOpenedContent();

        // Save on the last slide and verify the earlier slides.
        bookmarks.openContentFromToday(title);
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
    }

    private static String required(String suffix) {
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
