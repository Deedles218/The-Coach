package lib.ui;

import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.android.AndroidDriver;
import lib.LocalizationFixture;
import lib.LocalizationDeviceState;
import lib.Platform;
import lib.ui.factories.OnboardingPageObjectFactory;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.*;

/** Localization-specific text checks reuse the shared explicit waits and native IDs. */
public class LocalizationPageObject extends MainPageObject {
    private final LocalizationFixture fixture;
    private final boolean android;
    private String language = "en";
    private String previousAppLanguage;

    public LocalizationPageObject(RemoteWebDriver driver, LocalizationFixture fixture) {
        super(driver);
        this.fixture = fixture;
        android = Platform.getInstance().isAndroid();
    }

    public void setSystemLanguage(String language) throws Exception {
        setSystemLanguage(language, language);
    }

    public void setSystemLanguage(String language, String expectedAppLanguage) throws Exception {
        String country = fixture.region(language);
        if (android) {
            LocalizationDeviceState.setAndroidLocale(language, country);
        } else {
            Map<String, Object> args = new HashMap<>();
            args.put("language", Collections.singletonMap("name", language));
            args.put("locale", Collections.singletonMap("name", language + "_" + country));
            driver.executeScript("mobile: configureLocalization", args);
        }
        this.language = expectedAppLanguage;
        restart();
    }

    public void restart() {
        InteractsWithApps apps = (InteractsWithApps) driver;
        String id = android ? Platform.getInstance().getAndroidAppPackage() : Platform.getInstance().getIOSBundleId();
        apps.terminateApp(id);
        apps.activateApp(id);
    }

    private String androidId(String suffix) {
        return "id:" + Platform.getInstance().getAndroidAppPackage() + ":id/" + suffix;
    }

    private String locator(String key, String androidSuffix, String iosType) {
        String configured = fixture.optional("locator." + key);
        if (configured != null) return configured;
        if (android) return androidId(androidSuffix);
        // Existing builds lack stable IDs for these strings. This fallback is explicit
        // and reports a missing translation rather than tapping by coordinates/index.
        return "xpath://" + iosType + "[@label=" + xpathLiteral(fixture.text(language, key)) + "]";
    }

    public static String xpathLiteral(String value) {
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        return "concat('" + value.replace("'", "',\"'\",'") + "')";
    }

    private void assertText(String key, String locator) {
        String expected = LocalizationFixture.normalize(fixture.text(language, key));
        String[] actual = {null};
        try {
            createWait(20).withMessage("Localized text did not become ready: " + language + "." + key)
                    .until(ignored -> {
                        try {
                            WebElement element = driver.findElement(getLocatorByString(locator));
                            if (!element.isDisplayed()) return false;
                            actual[0] = LocalizationFixture.normalize(android ? element.getText() : element.getAttribute("label"));
                            return expected.equals(actual[0]);
                        } catch (org.openqa.selenium.NoSuchElementException | StaleElementReferenceException transientState) {
                            return false;
                        }
                    });
        } catch (TimeoutException failure) {
            if (actual[0] != null) Assert.assertEquals(language + "." + key, expected, actual[0]);
            throw failure;
        }
    }

    public void assertWelcome() {
        assertText("welcome.start", locator("welcome.start", "btnSignInAnonymous", "XCUIElementTypeButton"));
        assertText("welcome.login", locator("welcome.login", "btnLogin", "XCUIElementTypeButton"));
    }

    public void openLoginAndAssertText() {
        waitForElementAndClick(locator("welcome.login", "btnLogin", "XCUIElementTypeButton"), "Cannot open localized login", 10);
        assertText("login.title", locator("login.title", "tvTitleText", "XCUIElementTypeStaticText"));
        assertText("login.continue", locator("login.continue", "btnContinue", "XCUIElementTypeButton"));
        assertElementDisabled(locator("login.continue", "btnContinue", "XCUIElementTypeButton"),
                "Continue must be disabled for empty email", 10);
    }

    public void assertInvalidEmailValidation() {
        String input = android ? androidId("edtEmail") : "xpath://XCUIElementTypeTextField";
        waitForElementAndClear(input, "Email input absent", 10);
        waitForElementAndSendKeys(input, "not-an-email", "Cannot enter invalid email", 10);
        String button = locator("login.continue", "btnContinue", "XCUIElementTypeButton");
        assertElementDisabled(button, "Continue must be disabled for malformed email", 10);
        Assert.assertTrue("Invalid email left the login screen", isElementVisible(input));
    }

    public void waitForEntryScreen() {
        createWait(30).withMessage("No entry screen after localization bootstrap").until(ignored -> {
            closeLaunchOverlays();
            return isElementVisible(locator("welcome.login", "btnLogin", "XCUIElementTypeButton"))
                    || isElementVisible(locator("login.title", "tvTitleText", "XCUIElementTypeStaticText"))
                    || isElementVisible(android ? androidId("nav_graph_daily")
                            : locator("tab.today", null, "XCUIElementTypeButton"))
                    || isElementVisible("xpath://" + (android ? "android.widget.TextView[@text="
                            : "XCUIElementTypeStaticText[@label=")
                            + xpathLiteral(fixture.text(language, "profile.account")) + "]");
        });
    }

    public void assertTabs() {
        createWait(30).withMessage("Localized dashboard did not become ready: " + language).until(ignored -> {
            closeLaunchOverlays();
            return isElementVisible(android ? androidId("nav_graph_daily")
                    : locator("tab.today", null, "XCUIElementTypeButton"));
        });
        assertTab("today", "nav_graph_daily");
        assertTab("explore", "nav_graph_explore");
        assertTab("shop", "nav_graph_shop");
    }

    public void closeLaunchOverlays() {
        String promo = fixture.optional("launch.promo.title");
        if (!android && promo != null) {
            String title = "xpath://XCUIElementTypeStaticText[@label=" + xpathLiteral(promo) + "]";
            if (isElementVisible(title)) {
                // Same container-scoped unnamed-X pattern as the existing
                // iOS partner-promo helper, observed on this production build.
                waitForElementAndClick(title + "/../XCUIElementTypeButton[not(@name) and @visible='true']",
                        "Cannot close observed launch promo", 10);
                waitForElementNotVisible(title, "Launch promo remained visible", 10);
            }
        }
        if (!android && isElementVisible("xpath://XCUIElementTypeStaticText[@label='Allow notifications to stay on track']")) {
            CoachFlowPageObjectFactory.get(driver).closeNotificationPromptIfPresent();
        }
        OnboardingPageObjectFactory.get(driver).closePaywallsAndPopups();
        // Existing iOS fallback identifies RESTORE in English. Scope the localized
        // variant to the same observed paywall close icon plus its Restore button.
        String restore = fixture.optional(language + ".paywall.restore");
        if (android || restore == null) return;
        String marker = "xpath://XCUIElementTypeButton[@label=" + xpathLiteral(restore) + "]";
        for (int attempt = 0; attempt < 3 && isElementVisible("id:ic_outline_close") && isElementVisible(marker); attempt++) {
            String before = driver.getPageSource();
            waitForElementAndClick("id:ic_outline_close", "Cannot close localized launch paywall", 10);
            createWait(12).withMessage("Localized paywall close did not change the screen")
                    .until(ignored -> !before.equals(driver.getPageSource()));
        }
    }

    private void assertTab(String name, String suffix) {
        String key = "tab." + name;
        String textLocator = android ? "xpath://*[@resource-id=" + xpathLiteral(
                Platform.getInstance().getAndroidAppPackage() + ":id/" + suffix) + "]//android.widget.TextView"
                : locator(key, suffix, "XCUIElementTypeButton");
        if (fixture.optional("locator." + key) != null) textLocator = fixture.required("locator." + key);
        assertText(key, textLocator);
    }

    public void openProfile() {
        if (android && isElementVisible(androidId("rvSettings"))) return;
        String configured = fixture.optional("locator.profile.open");
        String[] candidates = configured == null ? (android ? new String[]{androidId("btnAction")}
                : new String[]{"id:profile_button", "id:UserProfileImage", "id:WomanProfileImage"}) : new String[]{configured};
        waitForFirstElementAndClick(candidates, "Cannot open localized Profile", 15);
    }

    public void assertProfile() {
        for (String key : Arrays.asList("profile.account", "profile.support", "profile.terms", "profile.logout")) {
            String configured = fixture.optional("locator." + key);
            String target = configured == null ? "xpath://" + (android ? "android.widget.TextView[@text=" : "XCUIElementTypeStaticText[@label=")
                    + xpathLiteral(fixture.text(language, key)) + "]" : configured;
            for (int attempt = 0; attempt < 6 && !isElementVisible(target); attempt++) scrollUp();
            assertText(key, target);
        }
    }

    private void scrollUp() {
        if (!android) {
            driver.executeScript("mobile: swipe", Collections.singletonMap("direction", "up"));
        } else {
            Dimension size = driver.manage().window().getSize();
            Map<String, Object> args = new HashMap<>();
            args.put("left", size.width / 10); args.put("top", size.height / 4);
            args.put("width", size.width * 8 / 10); args.put("height", size.height / 2);
            args.put("direction", "down"); args.put("percent", 0.75);
            driver.executeScript("mobile: scrollGesture", args);
        }
    }

    /** Actual selectors are recorded from each build; no invented language-switch IDs. */
    public void chooseAppLanguage(String target) {
        openLanguageSettings();
        if (previousAppLanguage == null) {
            String current = getElementAccessibleName(waitForElementVisible(fixture.required("language.selected.locator"),
                    "Current language selection is unavailable", 10));
            Assert.assertFalse("Selected language must expose a restorable label", current.trim().isEmpty());
            previousAppLanguage = current;
        }
        clickLanguage(fixture.required("language.option." + target));
        language = target;
        restart();
    }

    public void restoreAppLanguage() {
        // iOS host-side recovery restores the exact previous override, including absence.
        if (!android || previousAppLanguage == null) return;
        openLanguageSettings();
        // Restore the observed option, including System/Default when offered by the app.
        clickLanguage("xpath://android.widget.TextView[@text=" + xpathLiteral(previousAppLanguage) + "]");
        restart();
    }

    private void openLanguageSettings() {
        String selected = fixture.required("language.selected.locator");
        if (android) {
            if (isElementVisible(selected)) return;
            openProfile();
            // Profile assertions may leave its list at Logout. Reset the list
            // before opening language controls during restoration.
            for (int attempt = 0; attempt < 6; attempt++) {
                WebElement list = waitForElementVisible(androidId("rvSettings"), "Profile settings list is absent", 15);
                Map<String, Object> args = new HashMap<>();
                args.put("elementId", ((RemoteWebElement) list).getId());
                args.put("direction", "up"); args.put("percent", 1.0);
                if (Boolean.FALSE.equals(driver.executeScript("mobile: scrollGesture", args))) break;
            }
        }
        else {
            InteractsWithApps apps = (InteractsWithApps) driver;
            apps.terminateApp("com.apple.Preferences");
            apps.activateApp("com.apple.Preferences");
        }
        String steps = fixture.optional("language.open.steps." + language);
        if (steps == null) steps = fixture.required("language.open.steps");
        for (String locator : steps.split("\\|\\|", -1)) {
            if (isElementVisible(selected)) break;
            if (!android) swipeUpToFindFirstVisibleElement(new String[]{locator.trim()},
                    "Cannot reveal app language settings", 6);
            else for (int attempt = 0; attempt < 6 && !isElementVisible(locator.trim()); attempt++) scrollUp();
            waitForElementAndClick(locator.trim(), "Cannot open app language settings", 15);
        }
    }

    private void clickLanguage(String locator) {
        waitForElementAndClick(locator, "Cannot choose language", 10);
        String confirm = fixture.optional("language.confirm.locator");
        if (confirm != null) waitForElementAndClick(confirm, "Cannot confirm language change", 10);
        if (android) ((AndroidDriver) driver).navigate().back();
    }
}
