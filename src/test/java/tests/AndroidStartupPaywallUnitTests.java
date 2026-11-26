package tests;

import lib.ui.android.AndroidCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Cold-start offers must be dismissed before choosing login/logout navigation. */
public class AndroidStartupPaywallUnitTests {
    private static class Screen extends AndroidCoachFlowPageObject {
        String state;
        boolean refuseClose;
        final List<String> actions=new ArrayList<>();
        Screen(String state) { super(null);this.state=state; }
        @Override public void activateAppIfPossible() { }
        @Override public WebElement waitForFirstElementPresent(String[] locators,String message,long timeout) {
            actions.add("ready");return new RemoteWebElement();
        }
        @Override public boolean isElementPresent(String locator) {
            return ("paywall".equals(state) && locator.endsWith("/clPaywallContainer"))
                    || ("today".equals(state) && locator.endsWith("/nav_graph_daily"))
                    || ("welcome".equals(state) && START_SCREEN_TITLE.equals(locator));
        }
        @Override public boolean isElementVisible(String locator) { return isElementPresent(locator); }
        @Override protected void closeStartupPaywalls() {
            Assert.assertTrue("Cold-start wait was bypassed",actions.contains("ready"));
            actions.add("close-offer");
            if(refuseClose)throw new TimeoutException("offer remained");
            state="today";
        }
        @Override public void openToday() { actions.add("today"); }
        @Override public void openProfile() { actions.add("profile"); }
        @Override public void logOut() { actions.add("logout");state="welcome"; }
        @Override public void waitForStartScreen() {
            Assert.assertEquals("Unknown state cannot become Welcome","welcome",state);actions.add("welcome");
        }
    }
    @Test public void coldStartPaywallIsClosedBeforeLogout() {
        Screen screen=new Screen("paywall");screen.ensureLoggedOutOnStartScreen();
        Assert.assertEquals(Arrays.asList("ready","close-offer","today","profile","logout"),screen.actions);
    }
    @Test public void welcomeDoesNotDismissAnOffer() {
        Screen screen=new Screen("welcome");screen.ensureLoggedOutOnStartScreen();
        Assert.assertEquals(Arrays.asList("ready","welcome"),screen.actions);
    }
    @Test public void unsuccessfulDismissalCannotBecomeSuccessfulLogout() {
        Screen screen=new Screen("paywall");screen.refuseClose=true;
        try {screen.ensureLoggedOutOnStartScreen();Assert.fail("Offer failure was ignored");}
        catch(TimeoutException expected) {Assert.assertTrue(expected.getMessage().contains("offer remained"));}
        Assert.assertFalse(screen.actions.contains("logout"));
    }
    @Test(expected=AssertionError.class) public void anUnknownQuestionnaireCannotBeAcceptedAsWelcome() {
        new Screen("questionnaire").ensureLoggedOutOnStartScreen();
    }
}
