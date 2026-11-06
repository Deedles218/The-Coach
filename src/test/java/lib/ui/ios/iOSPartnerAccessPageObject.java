package lib.ui.ios;

import io.qameta.allure.Step;
import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.ios.IOSDriver;
import lib.Platform;
import lib.ui.MainPageObject;
import org.junit.Assert;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Quotes;

import java.net.URI;
import java.io.InputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** COA-9324: observe the offer before any generic popup cleanup can dismiss it. */
public class iOSPartnerAccessPageObject extends MainPageObject {
    public static final String RECIPIENT_BUNDLE = "com.vamapps.The-Coach-for-her";
    private static final Properties BUILD_LABELS = loadBuildLabels();
    private static final String STORE_SIGN_IN =
            "xpath://XCUIElementTypeButton[@name='Sign In' and @visible='true']";
    private static final String STORE_PASSWORD =
            "xpath://XCUIElementTypeSecureTextField[@visible='true']";
    private final iOSCoachFlowPageObject coach;
    private String previousStoreApplication;
    private boolean storeAuthenticationSubmitted;

    public iOSPartnerAccessPageObject(RemoteWebDriver driver) {
        super(driver);
        coach = new iOSCoachFlowPageObject(driver);
    }

    @Step("Complete a fresh buyer's onboarding without dismissing the partner offer")
    public void completeBuyerOnboarding(String goal) {
        iOSOnboardingPageObject onboarding = new iOSOnboardingPageObject(driver);
        onboarding.assertFreshStartIsDisplayed();
        onboarding.openStartFlow();
        onboarding.selectGoal(goal);
        onboarding.completeQuestionnaire();
        onboarding.closePaywallsWithoutConsumingSlides(new String[]{
                "Get an access for your partner", "More pleasure for both of you"
        });
        waitForLaunchSurface();
    }

    public void waitForLaunchSurface() {
        createWait(40).withMessage("Neither Today content nor the partner offer appeared").until(ignored -> {
            // These helpers only dismiss their own named offers, never the tested promo.
            if (isElementVisible(iOSCoachFlowPageObject.PARTNER_PROMO_TITLE)) return true;
            iOSOnboardingPageObject onboarding = new iOSOnboardingPageObject(driver);
            if (onboarding.isPaywallVisible()) {
                onboarding.closePaywallsWithoutConsumingSlides(new String[]{
                        "Get an access for your partner", "More pleasure for both of you"
                });
            }
            String liveCoach = text("Your Personal Guide Along the Way");
            if (isElementVisible(liveCoach)) {
                waitForElementAndClick(liveCoach + "/../XCUIElementTypeButton[not(@name) and @visible='true']",
                        "Cannot close the observed For Her live-coach introduction", 10);
                waitForElementNotVisible(liveCoach, "Live-coach introduction remained above Today", 10);
            }
            if (isElementVisible(iOSCoachFlowPageObject.MASTERCLASS_PROMO_TITLE)) coach.closeMasterclassPromoIfPresent();
            if (isElementVisible("id:PDFGuideUpsellCloseImage")) coach.closePdfGuideUpsellIfPresent();
            if (coach.isNotificationPromptDisplayed()) coach.closeNotificationPromptIfPresent();
            if (isElementVisible(text("Connect you email to save the progress."))
                    || isElementVisible(text("Connect your email to save the progress."))) {
                coach.closeConnectEmailPromptIfPresent();
            }
            return isElementVisible(iOSCoachFlowPageObject.PARTNER_PROMO_TITLE)
                    || isElementVisible("id:DailyPlanItem");
        });
    }

    @Step("Verify partner offer on app opening {opening}: expected={expected}")
    public void assertOfferOnOpening(int opening, boolean expected) {
        waitForLaunchSurface();
        if (expected) {
            waitForElementVisible(iOSCoachFlowPageObject.PARTNER_PROMO_TITLE,
                    "Partner offer is missing on opening " + opening, 20);
            return;
        }
        // A visible Today behind a late modal is not evidence of absence.
        // Observe the full delivery window using the project's explicit wait.
        assertAbsentForDeliveryWindow(iOSCoachFlowPageObject.PARTNER_PROMO_TITLE,
                "Unexpected partner offer on opening " + opening);
    }

    @Step("Dismiss the partner offer and verify Today remains available")
    public void dismissOffer() {
        coach.closePartnerPromoIfPresent();
        waitForElementVisible("id:DailyPlanItem", "Today did not return after dismissing the offer", 15);
    }

    public static String required(String name) {
        String value = System.getProperty("partner." + name, BUILD_LABELS.getProperty(name));
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Configure -Dpartner." + name + " from this build's UI/configuration");
        }
        return value.trim();
    }

    private static Properties loadBuildLabels() {
        Properties values = new Properties();
        try (InputStream input = iOSPartnerAccessPageObject.class.getResourceAsStream("/partner/ios-prod-3582.properties")) {
            if (input == null) throw new IllegalStateException("Partner build labels are missing");
            values.load(input);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read partner build labels", failure);
        }
        return values;
    }

    private static String text(String value) {
        String literal = Quotes.escape(value.trim().replaceAll("\\s+", " "));
        return "xpath://*[@visible='true' and (normalize-space(@name)=" + literal
                + " or normalize-space(@label)=" + literal + ")]";
    }

    @Step("Open the partner paywall from its offer")
    public void openPartnerPaywall() {
        String action = iOSCoachFlowPageObject.PARTNER_PROMO_TITLE
                + "/../XCUIElementTypeButton[@name='GIFT HER ACCESS' and @visible='true']";
        waitForElementAndClick(action, "Partner offer has no accessible Gift Her Access action", 15);
        waitForElementNotVisible(iOSCoachFlowPageObject.PARTNER_PROMO_TITLE,
                "Partner offer remained above the paywall", 15);
        assertPartnerPaywall();
    }

    @Step("Verify the configured partner paywall")
    public void assertPartnerPaywall() {
        waitForElementVisible(text(required("paywallTitle")), "The partner paywall did not open", 20);
        waitForElementVisible(text(required("paywallDisclosure")),
                "Lifetime partner paywall must state that there are no recurring charges", 15);
        waitForElementEnabled("id:CONTINUE", "Partner paywall Continue is not available", 15);
    }

    @Step("Open the partner purchase in the native TestFlight sheet")
    public void startPartnerStorePurchase() {
        Assert.assertNull("Finish the previous native store operation first", previousStoreApplication);
        Assert.assertTrue("Partner purchase must use the TestFlight/StoreKit sandbox",
                Platform.getInstance().isStoreKitSandboxEnabled());
        assertPartnerPaywall();
        waitForElementAndClick("id:CONTINUE", "Cannot start the partner purchase", 15);
        IOSDriver ios = (IOSDriver) driver;
        Object previous = ios.getSettings().get("defaultActiveApplication");
        previousStoreApplication = previous == null ? "auto" : previous.toString();
        storeAuthenticationSubmitted = false;
        // This iOS 18 TestFlight sheet belongs to SpringBoard, outside the AUT tree.
        ios.setSetting("defaultActiveApplication", "com.apple.springboard");
        assertPartnerStoreProduct();
    }

    @Step("Verify the native sheet offers the partner product as a one-time purchase")
    public void assertPartnerStoreProduct() {
        waitForElementVisible(text(required("storeProduct")),
                "StoreKit did not offer the expected partner product", 20);
        waitForElementVisible(text(required("storeOneTimeCharge")),
                "Partner product is not presented as a one-time charge", 15);
        waitForElementVisible(text(required("storeTestNotice")),
                "The native sheet is not explicitly marked as a test purchase", 15);
        rejectReusedPurchase();
    }

    private void rejectReusedPurchase() {
        Assert.assertFalse("The sandbox account has an old purchase; this is not a new transaction",
                isElementVisible("xpath://XCUIElementTypeStaticText[@visible='true' and ("
                        + "contains(@name,\"already purchased\") or contains(@name,\"Already Purchased\"))]"));
    }

    @Step("Cancel the native partner purchase and return to the same paywall")
    public void cancelStorePurchase() {
        Assert.assertNotNull("The native store context was not entered", previousStoreApplication);
        assertPartnerStoreProduct();
        String cancel = "xpath://XCUIElementTypeButton[@name='dismiss' and @label='close' and @visible='true']";
        waitForElementAndClick(cancel, "StoreKit Cancel is not accessible", 15);
        waitForElementNotVisible(cancel, "StoreKit sheet did not close", 15);
        restoreStoreApplication();
        assertPartnerPaywall();
    }

    @Step("Confirm the guarded TestFlight partner purchase")
    public void confirmPartnerStorePurchase() {
        Assert.assertTrue("Enable test-store confirmation with -Dpurchase.allow=true",
                Platform.getInstance().isTestPurchaseAllowed());
        Assert.assertNotNull("The native store context was not entered", previousStoreApplication);
        assertPartnerStoreProduct();
        waitForElementAndClick("xpath://XCUIElementTypeButton[@name='footer' and @label='Purchase' and @visible='true']",
                "Native TestFlight Purchase is not accessible", 15);
        createWait(45).withMessage("StoreKit confirmation did not finish; check system authentication/side-button requirements")
                .until(ignored -> {
                    rejectReusedPurchase();
                    authenticateStoreIfRequired();
                    return acknowledgeStoreSuccessIfPresent();
                });
        restoreStoreApplication();
    }

    /** Native context is entered by startPartnerStorePurchase; no payment is initiated here. */
    public boolean acknowledgeStoreSuccessIfPresent() {
        String success = text("Your purchase was successful.");
        if (!isElementVisible(success)) return false;
        waitForElementVisible(text("You’re all set"), "Unexpected native purchase-result dialog", 10);
        waitForElementAndClick("xpath://XCUIElementTypeButton[@name='OK' and @visible='true']",
                "Cannot acknowledge successful native test purchase", 10);
        waitForElementNotVisible(success, "Native purchase success notice remained open", 15);
        return true;
    }

    private void authenticateStoreIfRequired() {
        if (!isElementVisible(STORE_SIGN_IN) && !isElementVisible(STORE_PASSWORD)) return;
        Assert.assertFalse("Apple requested Sign In again; automatic credential retries are disabled",
                storeAuthenticationSubmitted);
        String account = System.getenv("PARTNER_STORE_ACCOUNT");
        String password = System.getenv("PARTNER_STORE_PASSWORD");
        Assert.assertTrue("Apple Account authentication is required; supply PARTNER_STORE_ACCOUNT "
                        + "and PARTNER_STORE_PASSWORD through the child environment",
                account != null && !account.trim().isEmpty() && password != null && !password.isEmpty());
        String prompt = waitForElementVisible("xpath://XCUIElementTypeStaticText[@visible='true' "
                        + "and starts-with(@name,'Enter the password for ')]",
                "The native Apple Account password prompt is not recognized", 10).getAttribute("name");
        assertStoreAccountMatches(prompt, account);
        storeAuthenticationSubmitted = true;
        try {
            waitForElementVisible(STORE_PASSWORD, "Native Apple password field is missing", 10).sendKeys(password);
            waitForElementAndClick(STORE_SIGN_IN, "Native Apple Sign In is unavailable", 10);
        } catch (WebDriverException failure) {
            // Do not retain a Selenium command/exception that might include input.
            throw new AssertionError("Native Apple Account credential submission failed");
        }
        createWait(30).withMessage("Apple Account authentication did not finish; check the device")
                .until(ignored -> !isElementVisible(STORE_SIGN_IN) && !isElementVisible(STORE_PASSWORD));
    }

    public static void assertStoreAccountMatches(String nativePrompt, String expectedAccount) {
        Assert.assertTrue("Configure the expected Apple Account for native authentication",
                expectedAccount != null && !expectedAccount.trim().isEmpty());
        Matcher accounts = Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")
                .matcher(nativePrompt == null ? "" : nativePrompt);
        Assert.assertTrue("Native Apple Account differs from the configured test credential",
                accounts.find() && expectedAccount.trim().equalsIgnoreCase(accounts.group()));
        Assert.assertFalse("Native Apple Account prompt is ambiguous", accounts.find());
    }

    private void restoreStoreApplication() {
        Assert.assertNotNull("The native store context was not entered", previousStoreApplication);
        ((IOSDriver) driver).setSetting("defaultActiveApplication", previousStoreApplication);
        previousStoreApplication = null;
    }

    @Step("Close the partner paywall without buying")
    public void closePartnerPaywall() {
        new iOSOnboardingPageObject(driver).closePaywallsAndPopups();
        waitForLaunchSurface();
    }

    @Step("Verify the purchased invitation banner is visible on Today")
    public void assertInviteAvailable() {
        createWait(45).withMessage("Successful partner purchase did not expose the invitation banner")
                .until(ignored -> {
                    rejectReusedPurchase();
                    return isElementVisible(text(required("inviteTitle")))
                            && isElementVisible(text(required("inviteButton")));
                });
        waitForElementEnabled(text(required("inviteButton")), "Invitation action is disabled", 10);
    }

    @Step("Verify no invitation is offered")
    public void assertNoInvite() {
        assertAbsentForDeliveryWindow(text(required("inviteTitle")),
                "An invitation is still offered when no pending partner license is expected");
    }

    @Step("Verify Share hides the invitation reminder for the current app opening")
    public void assertInviteHiddenAfterShare() {
        assertAbsentForDeliveryWindow(text(required("inviteTitle")),
                "Sharing the invitation did not hide its reminder for the current opening");
    }

    private void assertAbsentForDeliveryWindow(String locator, String message) {
        final long started = System.nanoTime();
        createWait(15).withMessage("Could not observe the full absence window").until(ignored -> {
            Assert.assertFalse(message, isElementVisible(locator));
            return System.nanoTime() - started >= java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        });
        // Driver errors propagate; a failed element query cannot prove absence.
    }

    @Step("Wait for the buyer's invitation banner to disappear after activation")
    public void waitForInviteRemoval() {
        waitForElementNotVisible(text(required("inviteTitle")),
                "Pending invitation banner remained after recipient activation", 30);
    }

    @Step("Copy the invitation from the native share sheet")
    public String copySharedInvite() {
        // A fresh marker prevents an old clipboard link from satisfying this case.
        String marker = "partner-e2e-" + UUID.randomUUID();
        withClipboardApp(() -> { ((IOSDriver) driver).setClipboardText(marker); return ""; }, true);
        waitForLaunchSurface();
        dismissOffer();
        assertInviteAvailable();
        waitForElementAndClick(text(required("inviteButton")), "Cannot share the partner invite", 15);
        String copy = "xpath://XCUIElementTypeCell[@visible='true' and (@name='Copy' or @name='Копировать')]";
        waitForElementAndClick(copy,
                "The native share sheet did not expose Copy", 15);
        waitForElementNotVisible(copy, "Native Copy did not close the share sheet", 15);
        waitForElementVisible("id:DailyPlanItem", "Today did not return after sharing", 15);
        // Check this before foregrounding WDA: returning from clipboard access
        // can itself deliver a new app-foreground event and show the reminder.
        assertInviteHiddenAfterShare();
        String sharedText = withClipboardApp(() -> ((IOSDriver) driver).getClipboardText(), false);
        Assert.assertFalse("Share sheet did not replace the fresh clipboard marker", marker.equals(sharedText));
        return inviteUrlFromSharedText(sharedText);
    }

    private String withClipboardApp(java.util.function.Supplier<String> operation, boolean returnToBuyer) {
        // iOS requires WDA in foreground for real-device clipboard access.
        String wda = System.getProperty("partner.wdaBundleId", "com.facebook.WebDriverAgentRunner.xctrunner");
        InteractsWithApps apps = (InteractsWithApps) driver;
        apps.activateApp(wda);
        try {
            return operation.get();
        } finally {
            // After Copy, the test performs the first buyer reopening explicitly.
            if (returnToBuyer) apps.activateApp(Platform.getInstance().getIOSBundleId());
        }
    }

    public static String inviteUrlFromSharedText(String sharedText) {
        Matcher links = Pattern.compile("https://[^\\s<>\\\"']+").matcher(sharedText == null ? "" : sharedText);
        Assert.assertTrue("Native share text contains no HTTPS invite URL", links.find());
        String url = links.group();
        Assert.assertFalse("Native share text contains more than one HTTPS URL", links.find());
        try {
            URI uri = new URI(url);
            Assert.assertTrue("Invite URL has no valid host or path",
                    uri.getHost() != null && uri.getRawPath() != null && uri.getRawPath().length() > 1
                            && uri.getRawUserInfo() == null);
        } catch (java.net.URISyntaxException invalid) {
            throw new AssertionError("Native share text contains an invalid invite URL");
        }
        return url;
    }

    @Step("Prepare the unpaid recipient and verify the selected lesson is locked")
    public void prepareUnpaidRecipient() {
        InteractsWithApps apps = (InteractsWithApps) driver;
        Assert.assertTrue("Install the matching production For Her TestFlight build", apps.isAppInstalled(RECIPIENT_BUNDLE));
        String buyer = Platform.getInstance().getIOSBundleId();
        apps.terminateApp(buyer);
        apps.activateApp(RECIPIENT_BUNDLE);
        withRecipientVariant(this::prepareRecipientJourney);
        apps.terminateApp(RECIPIENT_BUNDLE);
        apps.activateApp(buyer);
    }

    @Step("Create a second unpaid For Her recipient through UI logout and new onboarding")
    public void prepareSecondUnpaidRecipient() {
        InteractsWithApps apps = (InteractsWithApps) driver;
        apps.terminateApp(Platform.getInstance().getIOSBundleId());
        apps.activateApp(RECIPIENT_BUNDLE);
        withRecipientVariant(() -> {
            waitForLaunchSurface();
            coach.openToday();
            coach.openProfile();
            coach.logOut();
            prepareRecipientJourney();
        });
    }

    private void withRecipientVariant(Runnable action) {
        String previousBundle = System.getProperty("ios.bundleId");
        // Existing onboarding helpers select variant-specific answers from Platform.
        // This dedicated device suite runs sequentially, with the property restored on failure.
        System.setProperty("ios.bundleId", RECIPIENT_BUNDLE);
        try {
            action.run();
        } finally {
            if (previousBundle == null) System.clearProperty("ios.bundleId");
            else System.setProperty("ios.bundleId", previousBundle);
        }
    }

    private void prepareRecipientJourney() {
        iOSOnboardingPageObject onboarding = new iOSPartnerRecipientOnboardingPageObject(driver);
        // For Her 2.1.0 enters directly at Quiz / Goals on a fresh install.
        onboarding.createWait(30).withMessage("Recipient did not reach Today, Start or the first Quiz / Goals screen")
                .until(ignored -> onboarding.isGoalSelectionDisplayed()
                        || onboarding.isElementVisible(iOSCoachFlowPageObject.WELCOME_START_BUTTON)
                        || onboarding.isElementVisible("id:DailyPlanItem")
                        || onboarding.isPaywallVisible());
        if (onboarding.isPaywallVisible()) {
            onboarding.closePaywallsAndPopups();
        } else if (!onboarding.isElementVisible("id:DailyPlanItem")) {
            if (!onboarding.isGoalSelectionDisplayed()) onboarding.assertFreshStartIsDisplayed();
            onboarding.openStartFlow();
            onboarding.selectGoal(required("recipientGoal"));
            onboarding.completeQuestionnaire();
            onboarding.closePaywallsAndPopups();
        }
        waitForLaunchSurface();
        onboarding.waitForToday();
        assertRecipientLessonLocked();
    }

    @Step("Verify the recipient lesson is still locked behind a loaded paywall")
    public void assertRecipientLessonLocked() {
        iOSOnboardingPageObject onboarding = new iOSOnboardingPageObject(driver);
        iOSDailyPlanPageObject daily = openRecipientLessonCard();
        createWait(20).withMessage("Recipient baseline is not proven unpaid: selected lesson did not open a paywall")
                .until(ignored -> onboarding.isPaywallVisible());
        waitForElementVisible("id:RESTORE", "Recipient baseline has no loaded subscription paywall", 15);
        waitForElementEnabled("id:CONTINUE", "Recipient baseline paywall has no available plan", 15);
        Assert.assertFalse("A subscription loading error cannot prove an unpaid recipient baseline",
                isElementVisible("id:subscription_error_illustration"));
        onboarding.closePaywallsAndPopups();
        if (!isElementVisible("id:Today") && isElementVisible(text(required("recipientLesson")))) {
            daily.returnFromDailyLessonToDailyPlan();
        }
    }

    // No @Step/URL argument in Allure: the URL contains the redeemable invite token.
    public void followInviteAfterRestart(String url) {
        // A new app/Branch session makes reused-link checks meaningful after UI logout.
        ((InteractsWithApps) driver).terminateApp(RECIPIENT_BUNDLE);
        followInvite(url);
    }

    public void followInvite(String url) {
        try {
            // Let iOS/Branch resolve the actual universal link; never force For Her.
            driver.executeScript("mobile: deepLink", Collections.singletonMap("url", url));
        } catch (WebDriverException failure) {
            throw new AssertionError("iOS could not resolve the partner universal link");
        }
        createWait(30).withMessage("The invite did not resolve to the installed production For Her app")
                .until(ignored -> RECIPIENT_BUNDLE.equals(((Map<?, ?>) driver.executeScript("mobile: activeAppInfo")).get("bundleId")));
        withRecipientVariant(this::waitForLaunchSurface);
        waitForElementVisible("id:DailyPlanItem", "Recipient did not reach Today after resolving the invite", 15);
    }

    @Step("Verify the previously locked recipient lesson now shows its content")
    public void assertRecipientAccess() {
        iOSDailyPlanPageObject daily = openRecipientLessonCard();
        daily.assertDailyLessonScreenIsDisplayed();
        Assert.assertFalse("The recipient lesson still requires a subscription after activation",
                new iOSOnboardingPageObject(driver).isPaywallVisible());
        String title = Quotes.escape(required("recipientLesson"));
        waitForElementVisible("xpath://XCUIElementTypeStaticText[@visible='true' "
                        + "and string-length(normalize-space(@name))>80 and not(@name=" + title + ") "
                        + "and not(@label=" + title + ")]",
                "Previously locked lesson exposes only a heading, not its body content", 20);
        daily.returnFromDailyLessonToDailyPlan();
    }

    private iOSDailyPlanPageObject openRecipientLessonCard() {
        iOSDailyPlanPageObject daily = new iOSDailyPlanPageObject(driver);
        String day = System.getProperty("partner.recipientDay");
        if (day != null && !day.trim().isEmpty()) daily.selectObservedDay(day);
        daily.openDailyLessonCard(required("recipientLesson"));
        return daily;
    }

    @Step("Cold restart production For Her and verify recipient access persists")
    public void restartRecipientAndAssertAccess() {
        InteractsWithApps apps = (InteractsWithApps) driver;
        apps.terminateApp(RECIPIENT_BUNDLE);
        apps.activateApp(RECIPIENT_BUNDLE);
        withRecipientVariant(this::waitForLaunchSurface);
        waitForElementVisible("id:DailyPlanItem", "Recipient Today did not restore after restart", 40);
        assertRecipientAccess();
    }
}
