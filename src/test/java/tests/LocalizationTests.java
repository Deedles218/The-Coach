package tests;

import io.qameta.allure.*;
import lib.*;
import lib.ui.LocalizationPageObject;
import lib.ui.factories.CoachFlowPageObjectFactory;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runner.Description;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.*;

@RunWith(Parameterized.class)
@Epic("Localization")
@Feature("Language selection and localized UI smoke")
public class LocalizationTests extends CoreTestCase {
    private final String variant;
    private final String language;
    private final String scenario;
    private LocalizationFixture fixture;
    private LocalizationDeviceState originalDevice;
    private LocalizationPageObject page;

    @Parameterized.Parameters(name = "{0}/{1}/{2}")
    public static Collection<Object[]> matrix() {
        List<Object[]> rows = new ArrayList<>();
        List<String> scenarios = Arrays.asList(System.getProperty("localization.scenarios", "welcome,dashboard,switch").split(",", -1));
        if (new HashSet<>(scenarios).size() != scenarios.size()
                || !Arrays.asList("welcome", "dashboard", "switch").containsAll(scenarios)) {
            throw new IllegalArgumentException("localization.scenarios must contain unique welcome,dashboard,switch entries");
        }
        for (String language : LocalizationFixture.locales()) for (String scenario : scenarios) {
            rows.add(new Object[]{LocalizationFixture.variant(), language, scenario});
        }
        return rows;
    }

    public LocalizationTests(String variant, String language, String scenario) {
        this.variant = variant; this.language = language; this.scenario = scenario;
    }

    @Before @Override public void setUp() throws Exception {
        requireMobilePlatform();
        fixture = LocalizationFixture.load(language, scenario);
        if (!"welcome".equals(scenario)) TestData.existingProgressAccount();
        if ("switch".equals(scenario)) {
            Assert.assertTrue("App-language priority requires two supported languages; use welcome,dashboard for a single-language build",
                    LocalizationFixture.supportedLocales(variant).size() > 1);
            fixture.required("language.open.steps");
            fixture.required("language.selected.locator");
            fixture.required("language.option." + language);
            if ("en".equals(language)) fixture.required("language.option.fr");
        }
        Assert.assertEquals("Localization requires preinstalled app isolation; do not reinstall between locales",
                "logout", TestData.testIsolationMode());
        String prefix = Platform.getInstance().getPlatformVar();
        String envPrefix = prefix.toUpperCase(Locale.ROOT);
        Assert.assertFalse("Localization requires fullReset=false", Boolean.parseBoolean(System.getProperty(
                prefix + ".fullReset", System.getenv(envPrefix + "_FULL_RESET"))));
        String noReset = System.getProperty(prefix + ".noReset", System.getenv(envPrefix + "_NO_RESET"));
        Assert.assertTrue("Localization requires noReset=true", noReset == null || Boolean.parseBoolean(noReset));
        originalDevice = LocalizationDeviceState.capture();
        originalDevice.prepare();
        super.setUp();
        page = new LocalizationPageObject(driver, fixture);
        page.setSystemLanguage("en");
        page.waitForEntryScreen();
        Allure.addAttachment("Localization matrix", "platform=" + Platform.getInstance().getPlatformVar()
                + "\nvariant=" + variant + "\nlocale=" + language + "\nscenario=" + scenario
                + "\nbaseline.version=" + fixture.required("source.version")
                + "\nbaseline.origin=" + fixture.required("source.origin")
                + "\nbaseline.status=provisional build resources; linguistic review pending");
    }

    @Override protected RemoteWebDriver createDriver() throws Exception {
        // XCUITest language/locale capabilities pin -AppleLanguages/-AppleLocale
        // on every launch and would mask subsequent system-language changes.
        return Platform.getInstance().isIOS() ? Platform.getInstance().getDriver()
                : Platform.getInstance().getDriver("en", "US");
    }

    @Test @Severity(SeverityLevel.CRITICAL)
    public void testLocalizedScreensAndLanguagePersistence() throws Exception {
        RestoringTestAction.run(() -> {
            try {
                if ("welcome".equals(scenario)) {
                    CoachFlowPageObjectFactory.get(driver).ensureLoggedOutOnStartScreen();
                    page.setSystemLanguage(language);
                    page.assertWelcome();
                    page.openLoginAndAssertText();
                    page.assertInvalidEmailValidation();
                } else {
                    TestData.TestAccount account = TestData.existingProgressAccount();
                    CoachFlowPageObjectFactory.get(driver, page::assertTabs)
                            .ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
                    if ("switch".equals(scenario)) {
                        if ("en".equals(language) && Platform.getInstance().isIOS()) page.setSystemLanguage("fr");
                        originalDevice.prepareLanguageMenu();
                        // Android's Save flow needs a real changed selection;
                        // iOS stores English explicitly when the device is French.
                        if ("en".equals(language) && Platform.getInstance().isAndroid()) page.chooseAppLanguage("fr");
                        page.chooseAppLanguage(language);
                        if ("en".equals(language) && Platform.getInstance().isIOS()) page.setSystemLanguage("en", language);
                        // A different device language must not override an explicit app choice.
                        page.setSystemLanguage("en".equals(language) ? "fr" : "en", language);
                    } else page.setSystemLanguage(language);
                    page.assertTabs();
                    page.openProfile();
                    page.assertProfile();
                    page.restart();
                    page.assertTabs();
                    page.openProfile();
                    page.assertProfile();
                }
            } catch (Exception | AssertionError failure) {
                captureFailureArtifacts(failure, Description.createTestDescription(getClass(),
                        Platform.getInstance().getPlatformVar() + "-" + variant + "-" + language + "-" + scenario + "-before-restore"));
                throw failure;
            }
        }, () -> {
            if ("switch".equals(scenario)) page.restoreAppLanguage();
        });
    }

    @Override protected void cleanupSessionForNextTest() {
        // Restore native language preferences even when session creation failed.
        // Locale-independent app settings are kept by the existing noReset policy.
    }

    @Override public void tearDown() {
        try {
            if (Platform.getInstance().isAndroid()) {
                // UiAutomator2 resets its API policy on quit. Locale recovery
                // needs the still-active Settings process before that reset.
                RestoringTestAction.run(() -> {
                    if (originalDevice != null) originalDevice.close();
                }, () -> super.tearDown());
            } else {
                RestoringTestAction.run(() -> super.tearDown(), () -> {
                    if (originalDevice != null) originalDevice.close();
                });
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Could not finish localization session/restoration", failure);
        }
    }
}
