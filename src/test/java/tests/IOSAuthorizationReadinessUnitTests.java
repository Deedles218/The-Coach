package tests;

import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.lang.reflect.Proxy;
import java.util.*;

public class IOSAuthorizationReadinessUnitTests {
    private static final class Driver extends RemoteWebDriver {
        int polls, taps;
        boolean becomesHittable = true;
        Map<?, ?> lastTap;

        @Override public List<WebElement> findElements(By by) {
            WebElement element = (WebElement) Proxy.newProxyInstance(WebElement.class.getClassLoader(),
                    new Class<?>[]{WebElement.class}, (proxy, method, arguments) -> {
                        if (method.getName().equals("isDisplayed") || method.getName().equals("isEnabled")) return true;
                        if (method.getName().equals("getRect")) return new Rectangle(new Point(10, 20), new Dimension(100, 40));
                        if (method.getName().equals("getAttribute")) {
                            return arguments[0].equals("hittable")
                                    ? String.valueOf(++polls > 1 && becomesHittable) : "true";
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
            return Collections.singletonList(element);
        }

        @Override public Object executeScript(String script, Object... arguments) {
            Assert.assertEquals("mobile: tap", script);
            taps++; lastTap = (Map<?, ?>) arguments[0]; return null;
        }
    }

    private static final class Screen extends iOSCoachFlowPageObject {
        Set<String> visible = new HashSet<>();
        Screen(Driver driver) { super(driver); }
        @Override public boolean isElementVisible(String locator) { return visible.contains(locator); }
        void today() { visible.addAll(Arrays.asList("id:Today", "id:WomanProfileImage", "id:DailyPlanItem")); }
        void otp() { visible.add(OTP_SCREEN_TITLE); }
        void emailTitle() { visible.add(LOGIN_SCREEN_TITLE); }
        void emailInput() { visible.add(LOGIN_EMAIL_INPUT); }
        void profileHeader() { visible.add(PROFILE_SCREEN); }
        void profileBody() { visible.add(PROFILE_ACCOUNT_SETTINGS_BUTTON); }
        boolean dashboardReady() { return isAuthorizedDashboardVisible(); }
        boolean formReady() { return isLoginFormReady(); }
        boolean profileReady() { return isProfileContentReady(); }
        void tap(long timeout) { tapHittableControl(new String[]{"id:target"}, "Not hittable", timeout); }
    }

    @Test public void persistentTabsDoNotProveAuthorization() {
        Screen screen = new Screen(new Driver()); screen.visible.add("id:Today");
        Assert.assertFalse(screen.dashboardReady());
    }

    @Test public void renderedTodayAndProfileProveDashboardReadiness() {
        Screen screen = new Screen(new Driver()); screen.today(); Assert.assertTrue(screen.dashboardReady());
    }

    @Test public void otpOverlayBlocksDashboardReadiness() {
        Screen screen = new Screen(new Driver()); screen.today(); screen.otp(); Assert.assertFalse(screen.dashboardReady());
    }

    @Test public void emailOverlayBlocksDashboardReadiness() {
        Screen screen = new Screen(new Driver()); screen.today(); screen.emailTitle(); Assert.assertFalse(screen.dashboardReady());
    }

    @Test public void loginTitleNeedsItsEmailControl() {
        Screen screen = new Screen(new Driver()); screen.emailTitle(); Assert.assertFalse(screen.formReady());
        screen.emailInput(); Assert.assertTrue(screen.formReady());
    }

    @Test public void emptyProfileContainerIsNotReady() {
        Screen screen = new Screen(new Driver()); screen.profileHeader(); Assert.assertFalse(screen.profileReady());
    }

    @Test public void loadedProfileBodyIsReady() {
        Screen screen = new Screen(new Driver()); screen.profileHeader(); screen.profileBody(); Assert.assertTrue(screen.profileReady());
    }

    @Test public void visibleEnabledControlIsTappedOnceOnlyAfterItBecomesHittable() {
        Driver driver = new Driver(); new Screen(driver).tap(2);
        Assert.assertTrue(driver.polls >= 2); Assert.assertEquals(1, driver.taps);
        Assert.assertEquals(60, driver.lastTap.get("x")); Assert.assertEquals(40, driver.lastTap.get("y"));
    }

    @Test public void nonHittableControlIsNeverTapped() {
        Driver driver = new Driver(); driver.becomesHittable = false;
        try { new Screen(driver).tap(0); Assert.fail("A non-hittable control must time out"); }
        catch (TimeoutException expected) { Assert.assertEquals(0, driver.taps); }
    }
}
