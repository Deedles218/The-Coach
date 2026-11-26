package tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import lib.CoreTestCase;
import lib.Platform;
import lib.RestoringTestAction;
import lib.TestData;
import lib.UpdateEvidence;
import lib.ui.BookmarksPageObject;
import lib.ui.factories.BookmarksPageObjectFactory;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Epic("Daily Plan")
@Feature("Bookmarks")
public class BookmarksExpiryTests extends CoreTestCase {
    private Path control;
    private String device;
    private String bundle;
    private TestData.TestAccount account;

    @Override
    @Before
    public void setUp() throws Exception {
        Platform platform = Platform.getInstance();
        bundle = platform.getIOSBundleId();
        Assert.assertTrue("COA-9668 requires an iOS Him build with account evidence support",
                platform.isIOS() && ("com.vamapps.The-Coach".equals(bundle)
                        || "com.vamapps.preprod.The-Coach".equals(bundle)));
        device = System.getProperty("ios.udid", System.getenv("IOS_UDID"));
        Assert.assertTrue("COA-9668 needs an explicitly selected iOS test device",
                device != null && device.matches("(?i)(?:[0-9a-f-]{36}|[0-9a-f]{8}-[0-9a-f]{16})"));
        String uid = System.getenv("COACH_EXPECTED_ACCOUNT_UID");
        Assert.assertTrue("Set the approved COA-9668 fixture UID", uid != null && uid.matches("[A-Za-z0-9_-]{20,128}"));
        String executable = System.getenv("COACH_BOOKMARKS_EXPIRY_CONTROL");
        Assert.assertTrue("Set COACH_BOOKMARKS_EXPIRY_CONTROL to the approved fixture executable",
                executable != null && !executable.trim().isEmpty());
        control = Paths.get(executable).toAbsolutePath();
        Assert.assertTrue("COA-9668 fixture control is not executable", Files.isRegularFile(control) && Files.isExecutable(control));
        account = TestData.existingProgressAccount();
        super.setUp();
    }

    @Test
    @Issue("COA-9668")
    public void bookmarkSurvivesExpiryAndOpeningLessonShowsPaywall() throws Exception {
        String lesson = BookmarksTests.required("lessonTitle");
        String tooltip = BookmarksTests.required("tooltipText");
        iOSCoachFlowPageObject coach = (iOSCoachFlowPageObject) CoachFlowPageObjectFactory.get(driver);
        BookmarksPageObject bookmarks = BookmarksPageObjectFactory.get(driver);
        coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        coach.openProfile();
        coach.assertAuthenticatedProfileForEmail(account.getEmail());
        coach.openToday();
        UpdateEvidence.attachInstalledBuild(driver);
        assertSubscription(true, "Before Save");
        bookmarks.openFromToday();
        bookmarks.assertEmptyState();
        bookmarks.backFromBookmarks();
        bookmarks.openContentFromToday(lesson);
        bookmarks.assertOpenedContentTitle(lesson);
        bookmarks.assertNoPaywall();
        bookmarks.assertOpenedContentNotSaved();

        boolean[] expiryAttempted = {false};
        RestoringTestAction.run(() -> {
            try {
                bookmarks.saveOpenedContent();
                bookmarks.closeOpenedContent();
                bookmarks.openFromToday();
                bookmarks.assertTooltipAbsent(tooltip);
                bookmarks.assertCard(lesson);
                bookmarks.backFromBookmarks();

                expiryAttempted[0] = true;
                control("expire");
                assertSubscription(false, "After expiry");
                assertExpiredBookmarkAccess(bookmarks, lesson);
                assertSubscription(false, "After opening the saved lesson paywall");
            } catch (Exception | AssertionError failure) {
                captureFailureArtifacts(failure, Description.createTestDescription(getClass(), "expiryBeforeRestore"));
                throw failure;
            }
        }, () -> RestoringTestAction.run(() -> {
            if (expiryAttempted[0]) {
                control("restore");
                assertSubscription(true, "After fixture restore");
            }
        }, () -> {
            bookmarks.closeOpenedContentIfPresent();
            bookmarks.openFromToday();
            if (bookmarks.hasCard(lesson)) {
                bookmarks.openRemovalDialog(lesson);
                bookmarks.confirmRemoval();
            }
            bookmarks.assertEmptyState();
            bookmarks.backFromBookmarks();
        }));
    }

    static void assertExpiredBookmarkAccess(BookmarksPageObject bookmarks, String lesson) {
        bookmarks.openFromToday();
        bookmarks.assertCard(lesson);
        bookmarks.assertNoPaywall();
        bookmarks.openCardExpectingPaywall(lesson);
    }

    private void assertSubscription(boolean active, String phase) throws Exception {
        Object actual = UpdateEvidence.readAccountState(driver).get("subscriptionActive");
        Allure.addAttachment("COA-9668 " + phase, "Fixture UID verified, subscriptionActive=" + actual);
        Assert.assertEquals("Unexpected entitlement for the verified COA-9668 account during " + phase, active, actual);
    }

    private void control(String action) throws Exception {
        Process command = new ProcessBuilder(control.toString(), action, device, bundle)
                .redirectErrorStream(true).redirectOutput(new File("/dev/null")).start();
        try {
            Assert.assertTrue("COA-9668 fixture " + action + " timed out", command.waitFor(30, TimeUnit.SECONDS));
            Assert.assertEquals("COA-9668 fixture " + action + " failed", 0, command.exitValue());
        } finally {
            if (command.isAlive()) command.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
        }
    }
}
