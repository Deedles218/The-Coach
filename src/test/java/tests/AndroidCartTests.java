package tests;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import lib.CoreTestCase;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.RestoringTestAction;
import lib.TestData;
import lib.ui.CoachFlowPageObject;
import lib.ui.android.AndroidCartPageObject;
import lib.ui.android.AndroidCoachFlowPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;

@Epic("The Coach for Him")
@Feature("Store cart")
public class AndroidCartTests extends CoreTestCase {
    private static final String APP = "com.vamapps.thecoach";
    private AndroidCartPageObject cart;
    private CoachFlowPageObject coach;

    @Override
    protected boolean isPlatformSupported() {
        return Platform.getInstance().isAndroid();
    }

    @Before @Override
    public void setUp() throws Exception {
        if (isPlatformSupported()) {
            Assert.assertEquals("Use the reviewed For Him build", APP, Platform.getInstance().getAndroidAppPackage());
            PreparedMobileProfile.assertPreservationAllowed();
        }
        super.setUp();
        coach = CoachFlowPageObjectFactory.get(driver);
        PreparedMobileProfile.verifyFromToday(coach);
        cart = new AndroidCartPageObject(driver);
    }

    @Test @Issue("COA-9347")
    @DisplayName("COA-9347 Removing the last item clears the Shop cart indicator")
    public void testCoa9347RemovalClearsShopIndicator() throws Exception {
        withFixtureItem(() -> {
            cart.assertShopBadge(true);
            cart.removeFixtureItemIfPresent(fixtureProduct());
            cart.assertShopBadge(false);
            closeAndReopenCoachApplication();
            coach.waitForAuthorizedDashboard();
            PreparedMobileProfile.verifyFromToday(coach);
            coach.openShop();
            cart.assertShopBadge(false);
        });
    }

    @Test @Issue("COA-9351")
    @DisplayName("COA-9351 An empty cart shows neither a Shop badge nor an abandoned-cart banner")
    public void testCoa9351EmptyCartHasNoIndicatorOrBanner() {
        cart.assertEmptyCart();
        closeAndReopenCoachApplication();
        coach.waitForAuthorizedDashboard();
        PreparedMobileProfile.verifyFromToday(coach);
        coach.openShop();
        cart.assertShopBadge(false);
        coach.openToday();
        cart.assertBannerAbsent("daily");
        coach.openExplore();
        cart.assertBannerAbsent("explore");
    }

    @Test @Issue("COA-9365")
    @DisplayName("COA-9365 The same account shows its cart indicator after signing in on another device")
    public void testCoa9365CartIndicatorFollowsAccount() throws Exception {
        String udid = required("coach.cart.second.udid");
        Object primary = driver.getCapabilities().getCapability("udid");
        if (primary == null) primary = driver.getCapabilities().getCapability("appium:udid");
        Assert.assertNotNull("Set the primary Android UDID explicitly", primary);
        Assert.assertNotEquals("This case needs a different Android device", primary.toString(), udid);
        int systemPort = Integer.parseInt(required("coach.cart.second.systemPort"));
        Assert.assertTrue("Choose a separate UiAutomator2 system port above 8200", systemPort > 8200 && systemPort <= 65535);
        Object primaryPort = driver.getCapabilities().getCapability("appium:systemPort");
        if (primaryPort == null) primaryPort = driver.getCapabilities().getCapability("systemPort");
        Assert.assertNotEquals("The two sessions must not share a UiAutomator2 system port",
                primaryPort == null ? "8200" : primaryPort.toString(), String.valueOf(systemPort));
        TestData.TestAccount account = TestData.existingProgressAccount();
        UiAutomator2Options options = new UiAutomator2Options().setUdid(udid).setDeviceName(udid)
                .setAppPackage(APP).setAppActivity(Platform.getInstance().getAndroidAppActivity())
                .setNoReset(true).setFullReset(false).setAutoGrantPermissions(false).setSystemPort(systemPort);
        AndroidDriver other = new AndroidDriver(new URL(required("coach.cart.second.appiumUrl")), options);
        CoachFlowPageObject otherCoach = CoachFlowPageObjectFactory.get(other);
        withSecondDeviceSession(otherCoach::waitForStartScreen, () ->
            withFixtureItem(() -> {
                cart.assertShopBadge(true);
                otherCoach.loginWithEmailAndOtp(account.getEmail(), account.getOtp());
                verifySecondDeviceIdentity((AndroidCoachFlowPageObject) otherCoach, account.getEmail());
                otherCoach.openShop();
                new AndroidCartPageObject(other).assertShopBadge(true);
            }), otherCoach::ensureLoggedOutOnStartScreen, other::quit);
    }

    static void verifySecondDeviceIdentity(AndroidCoachFlowPageObject coach, String email) throws Exception {
        coach.openToday();
        RestoringTestAction.run(() -> {
            coach.openProfile();
            coach.assertAuthenticatedProfileForEmail(email);
        }, coach::returnToMainScreenFromProfile);
    }

    // Welcome must be observed before this test owns any authentication on the second device.
    static void withSecondDeviceSession(RestoringTestAction.CheckedAction requireWelcome,
                                        RestoringTestAction.CheckedAction scenario,
                                        RestoringTestAction.CheckedAction logout,
                                        RestoringTestAction.CheckedAction quit) throws Exception {
        RestoringTestAction.run(() -> {
            requireWelcome.run();
            RestoringTestAction.run(scenario, logout);
        }, quit);
    }

    @Test @Issue("COA-9368")
    @DisplayName("COA-9368 A nonempty cart keeps its Shop indicator after a cold restart")
    public void testCoa9368CartIndicatorSurvivesRestart() throws Exception {
        withFixtureItem(() -> {
            cart.assertShopBadge(true);
            closeAndReopenCoachApplication();
            coach.waitForAuthorizedDashboard();
            PreparedMobileProfile.verifyFromToday(coach);
            coach.openShop();
            cart.assertShopBadge(true);
        });
    }

    private void withFixtureItem(RestoringTestAction.CheckedAction scenario) throws Exception {
        Assert.assertTrue("Adding a product requires an explicitly dedicated QA account",
                Boolean.getBoolean("coach.cart.dedicatedAccount"));
        String product = fixtureProduct();
        cart.assertEmptyCart();
        RestoringTestAction.run(() -> {
            cart.addFixtureItem(product);
            coach.openToday();
            scenario.run();
        }, () -> cart.removeFixtureItemIfPresent(product));
    }

    private static String fixtureProduct() {
        return required("coach.cart.product");
    }

    private static String required(String property) {
        String value = System.getProperty(property, "").trim();
        Assert.assertFalse("Configure -D" + property + " for this Xray case", value.isEmpty());
        return value;
    }

    @Override
    protected void cleanupSessionForNextTest() {
        if (cart != null) coach.openToday();
    }
}
