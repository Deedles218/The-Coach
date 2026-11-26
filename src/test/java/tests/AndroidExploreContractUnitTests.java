package tests;

import lib.ui.android.AndroidExplorePageObject;
import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import java.util.*;

/** Exercise reviewed catalog branches without Appium or an application session. */
public class AndroidExploreContractUnitTests {
    private String previous;
    @Before public void save() {previous=System.getProperty("coach.explore.quickTip.redirect");System.clearProperty("coach.explore.quickTip.redirect");}
    @After public void restore() {
        if(previous==null)System.clearProperty("coach.explore.quickTip.redirect");
        else System.setProperty("coach.explore.quickTip.redirect",previous);
    }
    private static class Driver extends RemoteWebDriver {
        @Override public List<WebElement> findElements(By by) {return Collections.emptyList();}
    }
    private static class Screen extends AndroidExplorePageObject {
        final List<String> required=new ArrayList<>(),clicked=new ArrayList<>();
        boolean quickTips=true,video;
        Screen() {super(new Driver());}
        final WebElement element=new RemoteWebElement() {
            @Override public Rectangle getRect() {return new Rectangle(0,0,40,100);}
        };
        @Override public boolean isElementVisible(String locator) {
            if(locator.contains("'quicktips'"))return quickTips;
            if(locator.endsWith("/viewPlayer"))return video;
            return true;
        }
        @Override public WebElement waitForElementPresent(String locator,String message,long timeout) {required.add(locator);return element;}
        @Override public WebElement waitForElementVisible(String locator,String message,long timeout) {required.add(locator);return element;}
        @Override public WebElement waitForElementEnabled(String locator,String message,long timeout) {required.add(locator);return element;}
        @Override public WebElement waitForElementAndClick(String locator,String message,long timeout) {clicked.add(locator);return element;}
        @Override public void assertVideoPlayerControlsAreDisplayed() {required.add("video-controls");}
    }
    @Test public void defaultVideoPathStillRequiresTheNativePlayer() {
        Screen screen=new Screen();screen.openQuickTipVideoAndVerifyPlayer();
        Assert.assertEquals(1,screen.clicked.size());
        Assert.assertTrue(screen.required.stream().anyMatch(s -> s.endsWith("/viewPlayer")));
        Assert.assertTrue(screen.required.contains("video-controls"));
    }
    @Test public void absentCatalogChecksSectionsAndTemplatesWithoutNavigation() {
        System.setProperty("coach.explore.quickTip.redirect","absent");
        Screen screen=new Screen();screen.quickTips=false;
        screen.openQuickTipVideoAndVerifyPlayer();screen.assertConfiguredCardTemplates();
        Assert.assertTrue(screen.clicked.isEmpty());
        Assert.assertTrue(screen.required.stream().anyMatch(s -> s.contains("masterclasses")));
        Assert.assertTrue(screen.required.stream().anyMatch(s -> s.contains("iv_avatar")));
        Assert.assertFalse(screen.required.contains("video-controls"));
    }
    @Test(expected=AssertionError.class) public void absentCatalogRejectsAnUnexpectedQuickTipsSection() {
        System.setProperty("coach.explore.quickTip.redirect","absent");new Screen().openConfiguredQuickTipAndVerifyDestination();
    }
    @Test public void articleContractRequiresTheReviewedArticleAndNotAVideo() {
        System.setProperty("coach.explore.quickTip.redirect","coach_lesson");
        Screen screen=new Screen();screen.openQuickTipVideoAndVerifyPlayer();
        Assert.assertTrue(screen.clicked.get(0).contains("Finish Too Soon"));
        Assert.assertTrue(screen.required.stream().anyMatch(s -> s.contains("tvTextContent")));
        Assert.assertTrue(screen.required.stream().anyMatch(s -> s.endsWith("/bContinueLesson")));
        Assert.assertFalse(screen.required.contains("video-controls"));
    }
    @Test(expected=AssertionError.class) public void articleContractRejectsAVideoDestination() {
        System.setProperty("coach.explore.quickTip.redirect","coach_lesson");
        Screen screen=new Screen();screen.video=true;screen.openConfiguredQuickTipAndVerifyDestination();
    }
    @Test public void explicitVideoContractStillRequiresVideoControls() {
        System.setProperty("coach.explore.quickTip.redirect","coach_video");
        Screen screen=new Screen();screen.openConfiguredQuickTipAndVerifyDestination();
        Assert.assertEquals(1,screen.clicked.size());Assert.assertTrue(screen.required.contains("video-controls"));
    }
    @Test(expected=AssertionError.class) public void unreviewedContractCannotSilentlyUseLegacyNavigation() {
        System.setProperty("coach.explore.quickTip.redirect","unknown");new Screen().openConfiguredQuickTipAndVerifyDestination();
    }
}
