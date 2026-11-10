package lib.ui;

import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.appmanagement.ApplicationState;
import lib.AppearanceSnapshot;
import lib.DailyPlanCompletionMarker;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.awt.image.BufferedImage;
import java.io.IOException;

public abstract class ExploreBannerPageObject extends MainPageObject {
    protected ExploreBannerPageObject(RemoteWebDriver driver) { super(driver); }

    public abstract void openExternalCoachingPage();
    public abstract void assertImageIsVisibleWithoutOverlap() throws IOException;

    protected void assertCoachingPageIsInApp(String appId) {
        Assert.assertEquals("The coaching page opened outside The Coach", ApplicationState.RUNNING_IN_FOREGROUND,
                ((InteractsWithApps) driver).queryAppState(appId));
    }

    protected void assertBannerLayoutAndImage(Rectangle box, Rectangle picture, Rectangle heading,
                                              Rectangle next, Rectangle navigation) throws IOException {
        Dimension screen = driver.manage().window().getSize();
        Assert.assertTrue("The banner image is clipped by its container or the viewport",
                picture.width > 0 && picture.height > 0
                && picture.x >= Math.max(0, box.x) && picture.y >= Math.max(0, box.y)
                && picture.x + picture.width <= Math.min(screen.width, box.x + box.width)
                && picture.y + picture.height <= Math.min(screen.height, box.y + box.height));
        Assert.assertTrue("Explore banner covers the heading, next section or navigation",
                box.y >= heading.y + heading.height && box.y + box.height <= Math.min(next.y, navigation.y));

        BufferedImage render = DailyPlanCompletionMarker.crop(driver.getScreenshotAs(OutputType.BYTES), picture, screen.width);
        Assert.assertTrue("The banner is blank or did not render", AppearanceSnapshot.hasVisibleContrast(render));
        // The headline and description occupy the upper left of the reviewed male banner.
        BufferedImage copy = render.getSubimage(render.getWidth() / 12, render.getHeight() / 4,
                render.getWidth() / 2, render.getHeight() / 3);
        Assert.assertTrue("The banner copy area has no visible contrast", AppearanceSnapshot.hasVisibleContrast(copy));
    }
}
