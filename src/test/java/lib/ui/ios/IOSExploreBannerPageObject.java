package lib.ui.ios;

import io.qameta.allure.Step;
import lib.Platform;
import lib.ui.ExploreBannerPageObject;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.util.List;

/** The native Explore banner uses one image that includes its copy. */
public final class IOSExploreBannerPageObject extends ExploreBannerPageObject {
    private static final String BANNER = "xpath://XCUIElementTypeOther[@name='CoachBannerView' and @visible='true']";
    private static final String KATY_COACHING_WEB_VIEW = "xpath://XCUIElementTypeWebView[@visible='true']"
            + "//XCUIElementTypeStaticText[@label='people coached by Katy' and @visible='true']";
    private static final String KATY_BOOKING_LINK = "xpath://XCUIElementTypeWebView"
            + "//XCUIElementTypeLink[@label='Book 1:1 a private consultation' and @visible='true']";

    public IOSExploreBannerPageObject(RemoteWebDriver driver) { super(driver); }

    @Override @Step("Open the configured Katy coaching landing page from the Explore banner")
    public void openExternalCoachingPage() {
        waitForElementAndClick(BANNER, "The configured Explore banner is not tappable", 15);
        waitForElementVisible(KATY_COACHING_WEB_VIEW, "The expected coaching landing page did not render", 30);
        waitForElementPresent(KATY_BOOKING_LINK, "The expected coaching page booking link is missing", 10);
        assertCoachingPageIsInApp(Platform.getInstance().getIOSBundleId());
    }

    @Override @Step("Verify the Explore banner image renders and fits between the adjacent sections")
    public void assertImageIsVisibleWithoutOverlap() throws IOException {
        WebElement banner = waitForElementVisible(BANNER, "The configured Explore banner is absent", 15);
        List<WebElement> images = banner.findElements(By.xpath("./XCUIElementTypeImage[@visible='true']"));
        Assert.assertEquals("Explore banner must expose one visible image", 1, images.size());
        Rectangle heading = waitForElementVisible("xpath://XCUIElementTypeStaticText[@name='Explore' and @visible='true']",
                "Explore heading is missing", 10).getRect();
        Rectangle next = waitForElementVisible("id:Browse Programs", "The section below the banner is missing", 10).getRect();
        Rectangle navigation = waitForElementVisible("xpath://XCUIElementTypeButton[@name='Explore' and @value='1']",
                "Explore navigation is missing", 10).getRect();
        assertBannerLayoutAndImage(banner.getRect(), images.get(0).getRect(), heading, next, navigation);
    }
}
