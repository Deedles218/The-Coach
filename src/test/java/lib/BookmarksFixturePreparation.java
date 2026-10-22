package lib;

import lib.ui.BookmarksPageObject;
import lib.ui.CoachFlowPageObject;
import lib.ui.factories.BookmarksPageObjectFactory;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.Arrays;
import java.util.Locale;

/** Explicit UI preparation of missing bookmarks on dedicated account roles. Not a regression test. */
public final class BookmarksFixturePreparation {
    private BookmarksFixturePreparation() { }

    public static void main(String[] arguments) throws Exception {
        Assert.assertTrue("Enable the explicit fixture preparation with coach.bookmarks.prepareFixtures=true",
                Boolean.getBoolean("coach.bookmarks.prepareFixtures"));
        Assert.assertTrue("Supply role/title pairs", arguments.length > 0 && arguments.length % 2 == 0);
        Assert.assertTrue("Bookmarks fixture preparation requires iOS", Platform.getInstance().isIOS());
        for (int index = 0; index < arguments.length; index += 2) {
            String role = arguments[index].toUpperCase(Locale.ROOT);
            Assert.assertTrue("Only dedicated CONTENT/SWITCHA/SWITCHB roles may be prepared",
                    Arrays.asList("CONTENT", "SWITCHA", "SWITCHB").contains(role));
            Assert.assertFalse("The fixture content title is empty", arguments[index + 1].trim().isEmpty());
        }
        RemoteWebDriver driver = Platform.getInstance().getDriver();
        CoachFlowPageObject coach = CoachFlowPageObjectFactory.get(driver);
        BookmarksPageObject bookmarks = BookmarksPageObjectFactory.get(driver);
        try {
            for (int index = 0; index < arguments.length; index += 2) {
                String role = arguments[index].toUpperCase(Locale.ROOT);
                String title = arguments[index + 1];
                TestData.TestAccount account = TestData.bookmarksAccount(role);
                bookmarks.closeOpenedContentIfPresent();
                if (bookmarks.isScreenVisible()) bookmarks.backFromBookmarks();
                coach.ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
                coach.openProfile();
                ((iOSCoachFlowPageObject) coach).assertAuthenticatedProfileForEmail(account.getEmail());
                coach.openToday();
                bookmarks.openFromToday();
                boolean missing = !bookmarks.hasCard(title);
                if (missing) {
                    bookmarks.backFromBookmarks();
                    bookmarks.openContentFromToday(title);
                    bookmarks.ensureOpenedContentIsSaved();
                    bookmarks.closeOpenedContent();
                    bookmarks.openFromToday();
                }
                bookmarks.assertCard(title);
                bookmarks.backFromBookmarks();
                coach.cleanupForNextTest();
                System.out.println("Prepared " + role + ": " + (missing ? "added missing bookmark" : "already present"));
            }
        } finally {
            driver.quit();
        }
    }
}
