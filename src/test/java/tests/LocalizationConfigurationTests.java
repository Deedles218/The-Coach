package tests;

import io.qameta.allure.Feature;
import io.qameta.allure.junit4.DisplayName;
import lib.AllureReportSupport;
import lib.CoreTestCase;
import lib.LocalizationFixture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.nio.file.Paths;

/** One offline gate for the complete matrix, before any locale opens a session. */
@Feature("Localization preconditions")
public class LocalizationConfigurationTests extends CoreTestCase {
    @Before @Override public void setUp() throws Exception {
        // Deliberately do not call CoreTestCase.setUp(): no driver/device access.
        requireIOSPlatform();
        String directory = System.getProperty("allure.results.directory");
        if (directory != null) {
            java.nio.file.Files.createDirectories(Paths.get(directory));
            AllureReportSupport.writeCategories(Paths.get(directory));
        }
    }

    @Test
    @DisplayName("Preflight: frozen localization fixture supports the entire selected matrix")
    public void testFrozenFixtureSupportsSelectedMatrix() throws Exception {
        for (Object[] row : LocalizationTests.matrix()) {
            String variant = (String) row[0], language = (String) row[1], scenario = (String) row[2];
            LocalizationFixture fixture = LocalizationFixture.load(language, scenario);
            if ("switch".equals(scenario)) {
                Assert.assertTrue("App-language priority requires two supported languages",
                        LocalizationFixture.supportedLocales(variant).size() > 1);
                fixture.required("language.open.steps");
                fixture.required("language.selected.locator");
                fixture.required("language.option." + language);
                if ("en".equals(language)) fixture.required("language.option.fr");
            }
        }
    }
}
