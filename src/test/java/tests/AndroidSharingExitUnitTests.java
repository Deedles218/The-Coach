package lib.ui.android;

import lib.Platform;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Offline navigation model: the negative scenario must leave the lesson just like the positive one. */
public class AndroidSharingExitUnitTests {
    private enum Screen { PLAYER, PLAYER_CONFIRM, LESSON, LESSON_CONFIRM, TODAY, POPUP }

    private static final class Driver extends RemoteWebDriver {
        Screen screen = Screen.PLAYER;
        final boolean confirm;
        final Screen playerDestination;
        final Screen lessonDestination;
        final List<String> clicks = new ArrayList<>();

        Driver(boolean confirm, Screen playerDestination, Screen lessonDestination) {
            this.confirm = confirm;
            this.playerDestination = playerDestination;
            this.lessonDestination = lessonDestination;
        }

        @Override public List<WebElement> findElements(By by) {
            String locator = by.toString();
            String control = null;
            if (locator.endsWith(":id/viewPlayer") && screen == Screen.PLAYER) control = "player";
            if (locator.endsWith(":id/btnNavigateUp") && screen == Screen.PLAYER) control = "back";
            if (locator.endsWith(":id/btnYes") && (screen == Screen.PLAYER_CONFIRM || screen == Screen.LESSON_CONFIRM)) control = "confirm";
            if (locator.endsWith(":id/rvLessonDetail") && screen == Screen.LESSON) control = "lesson";
            if (locator.endsWith(":id/ivButtonClose") && screen == Screen.LESSON) control = "close";
            if (locator.endsWith(":id/rvQuestions") && screen == Screen.TODAY) control = "today";
            if (locator.endsWith(":id/btnSharePartner") && screen == Screen.POPUP) control = "partner";
            if (locator.endsWith(":id/btnShareFriend") && screen == Screen.POPUP) control = "friend";
            if (control == null) return Collections.emptyList();
            final String action = control;
            return Collections.singletonList(new RemoteWebElement() {
                @Override public boolean isDisplayed() { return true; }
                @Override public boolean isEnabled() { return true; }
                @Override public String getAttribute(String name) { return "true"; }
                @Override public void click() {
                    clicks.add(action);
                    if ("back".equals(action)) screen = confirm ? Screen.PLAYER_CONFIRM : playerDestination;
                    else if ("close".equals(action)) screen = confirm ? Screen.LESSON_CONFIRM : lessonDestination;
                    else if ("confirm".equals(action)) screen = screen == Screen.PLAYER_CONFIRM ? playerDestination : lessonDestination;
                    else Assert.fail("Unexpected click: " + action);
                }
            });
        }

        @Override public WebElement findElement(By by) {
            List<WebElement> elements = findElements(by);
            if (elements.isEmpty()) throw new NoSuchElementException("Offline screen has no such control");
            return elements.get(0);
        }
    }

    private void exit(Driver driver) {
        new AndroidVideoSharingPageObject(driver, null).exitVideoAndLesson();
    }

    @Test public void unconfiguredLessonIsClosedAfterPlayerExit() {
        Driver driver = new Driver(false, Screen.LESSON, Screen.TODAY);
        exit(driver);
        Assert.assertEquals(Arrays.asList("back", "close"), driver.clicks);
        Assert.assertEquals(Screen.TODAY, driver.screen);
    }

    @Test public void bothExitConfirmationsAreAcceptedBeforeReturningToToday() {
        Driver driver = new Driver(true, Screen.LESSON, Screen.TODAY);
        exit(driver);
        Assert.assertEquals(Arrays.asList("back", "confirm", "close", "confirm"), driver.clicks);
        Assert.assertEquals(Screen.TODAY, driver.screen);
    }

    @Test public void popupTriggeredByClosingLessonIsLeftVisibleForTheOracle() {
        Driver driver = new Driver(false, Screen.LESSON, Screen.POPUP);
        exit(driver);
        Assert.assertEquals(Arrays.asList("back", "close"), driver.clicks);
        Assert.assertEquals(Screen.POPUP, driver.screen);
    }

    @Test public void directTodayExitDoesNotAttemptToCloseAnUnrelatedLesson() {
        Driver driver = new Driver(false, Screen.TODAY, Screen.TODAY);
        exit(driver);
        Assert.assertEquals(Collections.singletonList("back"), driver.clicks);
    }

    @Test public void popupTriggeredByPlayerExitIsLeftVisibleForTheOracle() {
        Driver driver = new Driver(false, Screen.POPUP, Screen.TODAY);
        exit(driver);
        Assert.assertEquals(Collections.singletonList("back"), driver.clicks);
        Assert.assertEquals(Screen.POPUP, driver.screen);
    }
}
