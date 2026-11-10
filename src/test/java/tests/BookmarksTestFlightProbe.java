package tests;

import io.qameta.allure.Allure;
import lib.TestData;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;

/** Targeted physical-device companion. The owning runner holds the device lock throughout. */
public class BookmarksTestFlightProbe extends BookmarksTests {
    @Override
    protected void cleanupSessionForNextTest() {
        // ensure-rc may inspect TestFlight's Automatic Updates settings. Parent
        // Bookmarks cleanup must query the actual Coach screen before logout.
        if (driver != null) CoachFlowPageObjectFactory.get(driver).activateAppIfPossible();
        super.cleanupSessionForNextTest();
    }

    @Override
    protected boolean isPlatformSupported() {
        return lib.Platform.getInstance().isIOS()
                && System.getProperty("coach.bookmarks.testFlightHandoff") != null;
    }

    @Test
    public void bookmarksAcrossTestFlightUpdateAndReinstall() throws Exception {
        lib.RestoringTestAction.run(() -> {
            handoff("install-previous");
            coach = CoachFlowPageObjectFactory.get(driver);
            coach.activateAppIfPossible();
            prepare(UPDATE);
            String lesson = required("lessonTitle");
            String tooltip = required("tooltipText");
            bookmarks.openFromToday();
            bookmarks.assertCard(lesson);
            // The update case starts with an already dismissed tooltip. The
            // previous build may show it on this fixture's first local entry.
            bookmarks.dismissTooltipIfVisible(tooltip);
            bookmarks.reopenAndAssertTooltipAbsent(tooltip);
            bookmarks.assertTooltipAbsent(tooltip);
            bookmarks.backFromBookmarks();

            handoff("install-rc");
            coach.activateAppIfPossible();
            coach.closePdfGuideUpsellIfPresent();
            coach.closeNotificationPromptIfPresent();
            coach.closeConnectEmailPromptIfPresent();
            coach.waitForAuthorizedDashboard();
            coach.assertAuthorizedDashboardIsDisplayed();
            coach.openProfile();
            ((iOSCoachFlowPageObject) coach).assertAuthenticatedProfileForEmail(TestData.bookmarksAccount(UPDATE).getEmail());
            coach.openToday();
            bookmarks.openFromToday();
            bookmarks.assertCard(lesson);
            bookmarks.assertTooltipAbsent(tooltip);
            bookmarks.backFromBookmarks();

            handoff("reinstall-rc");
            coach.activateAppIfPossible();
            prepare(UPDATE);
            bookmarks.openFromToday();
            bookmarks.assertCard(lesson);
            bookmarks.assertTooltipVisible(tooltip);
        }, () -> handoff("ensure-rc"));
    }

    @Test
    public void tooltipOnFreshInstallation() throws Exception {
        String variant = System.getProperty("coach.bookmarks.tooltipVariant");
        boolean[] createdPractice = {Boolean.getBoolean("coach.bookmarks.cleanupSeededPractice")};
        String practice = required("practiceTitle");
        lib.RestoringTestAction.run(() -> {
            lib.RestoringTestAction.run(() -> {
                if (!"empty-first".equals(variant)) {
                    prepare("TOOLTIP");
                    lib.ExploreStateRestorer original = lib.ExploreStateRestorer.capture(driver);
                    lib.RestoringTestAction.run(() -> {
                        openFixtureContentFromToday(practice);
                        if (!bookmarks.hasAnyBookmarkCard()) {
                            bookmarks.saveOpenedContent();
                            createdPractice[0] = true;
                        }
                        bookmarks.closeOpenedContent();
                    }, original::restore);
                }
                handoff("reinstall-rc");
                coach = CoachFlowPageObjectFactory.get(driver);
                coach.activateAppIfPossible();
                if ("empty-first".equals(variant)) {
                    tooltipWaitsForFirstNonEmptyEntry();
                    return;
                }
                prepare("TOOLTIP");
                String tooltip = required("tooltipText");
                String lesson = required("lessonTitle");
                bookmarks.openFromToday();
                Assert.assertEquals("Tooltip fixture must contain two saved cards", 2,
                        driver.findElements(org.openqa.selenium.By.xpath("//XCUIElementTypeScrollView[@visible='true']"
                                + "//XCUIElementTypeButton[@name='filled_bookmark']")).size());
                bookmarks.assertCard(lesson);
                bookmarks.assertTooltipVisible(tooltip);
                Allure.addAttachment("Fresh tooltip before dismissal", "image/png",
                        new java.io.ByteArrayInputStream(driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES)), "png");
                if ("blank".equals(variant)) bookmarks.dismissTooltipByBlankArea(tooltip);
                else if ("card".equals(variant)) bookmarks.dismissTooltipByCard(lesson, tooltip);
                else if ("bookmark".equals(variant)) bookmarks.dismissTooltipByBookmark(lesson, tooltip);
                else if ("navigation".equals(variant)) bookmarks.dismissTooltipByNavigation(tooltip);
                else Assert.fail("Unknown tooltip dismissal variant");
                bookmarks.closeOpenedContentIfPresent();
                bookmarks.reopenAndAssertTooltipAbsent(tooltip);
            }, () -> {
                if (createdPractice[0]) {
                    bookmarks.closeOpenedContentIfPresent();
                    bookmarks.openFromToday();
                    bookmarks.openRemovalDialog(practice);
                    bookmarks.confirmRemoval();
                    bookmarks.assertCardAbsent(practice);
                }
            });
        }, () -> handoff("ensure-rc"));
    }

    private void handoff(String phase) throws Exception {
        lib.TestFlightHandoff.perform(driver, phase);
    }
}
