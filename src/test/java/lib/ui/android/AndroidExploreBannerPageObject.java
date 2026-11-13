package lib.ui.android;

import io.qameta.allure.Step;
import lib.Platform;
import lib.ui.ExploreBannerPageObject;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;

public final class AndroidExploreBannerPageObject extends ExploreBannerPageObject {
    private static final String APP_PACKAGE = Platform.getInstance().getAndroidAppPackage();
    private static final String ID = APP_PACKAGE + ":id/";
    private static final String BANNER = "id:" + ID + "explore_banner";
    private static final String BANNER_IMAGE = "xpath://*[@resource-id='" + ID + "explore_banner']"
            + "//*[@resource-id='" + ID + "iv_background']";
    private static final String KATY_COACHING_WEB_VIEW = "xpath://android.webkit.WebView"
            + "//android.widget.TextView[@text='people coached by Katy']";
    private static final String KATY_BOOKING_LINK = "xpath://android.webkit.WebView"
            + "//*[@content-desc='Book 1:1 a private consultation']";

    public AndroidExploreBannerPageObject(RemoteWebDriver driver) { super(driver); }

    WebElement waitForBanner() {
        return waitForElementVisible(BANNER, "The configured Explore banner is absent", 15);
    }

    WebElement waitForBannerImage() {
        return waitForElementVisible(BANNER_IMAGE, "The banner image is absent", 10);
    }

    @Override @Step("Open the configured Katy coaching landing page from the Android Explore banner")
    public void openExternalCoachingPage() {
        waitForElementAndClick(BANNER, "The configured Explore banner is not tappable", 15);
        waitForElementVisible(KATY_COACHING_WEB_VIEW, "The expected coaching landing page did not render", 60);
        waitForElementPresent(KATY_BOOKING_LINK, "The expected coaching page booking link is missing", 10);
        assertCoachingPageIsInApp(APP_PACKAGE);
    }

    @Override @Step("Verify the Android Explore banner image fits between the adjacent sections")
    public void assertImageIsVisibleWithoutOverlap() throws IOException {
        Rectangle box = waitForBanner().getRect();
        Rectangle picture = waitForBannerImage().getRect();
        Rectangle heading = waitForElementVisible("xpath://*[@resource-id='" + ID + "tvLabel' and @text='EXPLORE']",
                "Explore heading is missing", 10).getRect();
        Rectangle next = waitForFirstElementPresent(new String[]{"id:" + ID + "tvActiveTitle", "id:" + ID + "rvActivePrograms"},
                "The section below the banner is missing", 10).getRect();
        Rectangle navigation = waitForElementVisible("id:" + ID + "nav_graph_explore",
                "Explore navigation is missing", 10).getRect();
        assertBannerLayoutAndImage(box, picture, heading, next, navigation);
    }
}
