package tests;

import lib.LocalizationFixture;
import lib.ui.LocalizationPageObject;
import org.junit.*;
import java.util.*;

/** Offline checks prevent incomplete/mismatched language coverage from looking green. */
public class LocalizationFixtureUnitTests {
    @Test public void buildMatricesMatchSupportedLanguages() {
        Assert.assertEquals(Arrays.asList("en", "fr", "de", "it", "es"), LocalizationFixture.supportedLocales("male"));
        Assert.assertEquals(Arrays.asList("en", "es", "fr"), LocalizationFixture.supportedLocales("female"));
        Assert.assertEquals(Collections.singletonList("en"), LocalizationFixture.supportedLocales("female-legacy"));
    }

    @Test public void explicitSubsetRetainsRequestedOrder() {
        Assert.assertEquals(Arrays.asList("es", "fr"), LocalizationFixture.selectLocales("female", "es,fr"));
    }

    @Test public void unsupportedEmptyAndDuplicateLocalesAreRejected() {
        for (String value : Arrays.asList("de", "en,en", "", "en,")) {
            rejects(() -> LocalizationFixture.selectLocales("female", value));
        }
        rejects(() -> LocalizationFixture.supportedLocales("unknown"));
    }

    @Test public void metadataMustMatchPlatformBuildVariantAndApp() {
        LocalizationFixture fixture = new LocalizationFixture(complete());
        for (String[] args : new String[][]{{"android", "male", "app"}, {"ios", "female", "app"}, {"ios", "male", "another"}}) {
            rejects(() -> fixture.validate(args[0], args[1], args[2], Collections.singletonList("en")));
        }
    }

    @Test public void missingTranslationCannotFallBackToEnglish() {
        Properties data = complete();
        data.remove("fr.profile.support");
        rejects(() -> new LocalizationFixture(data).validate("ios", "male", "app", Arrays.asList("en", "fr")));
    }

    @Test public void absentDashboardTranslationDoesNotBlockWelcomeOrAnotherLocale() {
        Properties data = complete(); data.remove("fr.tab.shop");
        data.setProperty("missing.fr.tab.shop", "APK resource shop_title has no fr translation");
        LocalizationFixture fixture = new LocalizationFixture(data);
        fixture.validate("ios", "male", "app", Collections.singletonList("fr"), LocalizationFixture.keysForScenario("welcome"));
        fixture.validate("ios", "male", "app", Collections.singletonList("en"), LocalizationFixture.keysForScenario("dashboard"));
        try {
            fixture.validate("ios", "male", "app", Collections.singletonList("fr"), LocalizationFixture.keysForScenario("dashboard"));
            Assert.fail("Missing translation passed");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("APK resource shop_title"));
        }
    }

    @Test public void blankTranslationAndMissingEnglishBootstrapFail() {
        Properties data = complete();
        data.setProperty("fr.login.title", " ");
        rejects(() -> new LocalizationFixture(data).validate("ios", "male", "app", Collections.singletonList("fr")));
        Properties missingEnglish = complete(); missingEnglish.remove("en.welcome.login");
        rejects(() -> new LocalizationFixture(missingEnglish).validate("ios", "male", "app", Collections.singletonList("fr")));
    }

    @Test public void whitespaceNormalizationPreservesMeaningCaseAndAccents() {
        Assert.assertEquals("Conditions & confidentialité", LocalizationFixture.normalize(" Conditions\u00a0&\nconfidentialité "));
        Assert.assertNotEquals(LocalizationFixture.normalize("Continue"), LocalizationFixture.normalize("CONTINUE"));
        Assert.assertNotEquals(LocalizationFixture.normalize("Francais"), LocalizationFixture.normalize("Français"));
    }

    @Test public void localizedApostrophesAndQuotesProduceValidXPathLiteral() {
        Assert.assertEquals("'Français'", LocalizationPageObject.xpathLiteral("Français"));
        Assert.assertEquals("\"I've purchased\"", LocalizationPageObject.xpathLiteral("I've purchased"));
        Assert.assertEquals("concat('L',\"'\",'app \"Coach\"')", LocalizationPageObject.xpathLiteral("L'app \"Coach\""));
    }

    private Properties complete() {
        Properties data = new Properties();
        data.setProperty("platform", "ios"); data.setProperty("variant", "male"); data.setProperty("appId", "app");
        data.setProperty("source.version", "test"); data.setProperty("source.origin", "unit test fixture");
        for (String locale : Arrays.asList("en", "fr")) for (String key : LocalizationFixture.TEXT_KEYS) {
            data.setProperty(locale + "." + key, locale + " " + key);
        }
        return data;
    }

    private void rejects(Runnable action) {
        try { action.run(); Assert.fail("Invalid localization configuration was accepted"); }
        catch (IllegalArgumentException expected) { Assert.assertFalse(expected.getMessage().isEmpty()); }
    }
}
