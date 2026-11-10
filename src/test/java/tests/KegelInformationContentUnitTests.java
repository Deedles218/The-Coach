package tests;

import lib.Platform;
import lib.ui.KegelCasePageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class KegelInformationContentUnitTests {
    private void check(String content, boolean semanticPattern) throws Exception {
        Properties data = new Properties();
        data.setProperty("platform", Platform.getInstance().getPlatformVar());
        data.setProperty("app.version", "unit"); data.setProperty("environment", "unit");
        data.setProperty("info.expectedContent.FLICKER", "pelvic floor");
        if (semanticPattern) data.setProperty("info.expectedPattern.FLICKER",
                "(?=.*(?:contract|tighten))(?=.*(?:relax|release))(?=.*(?:quick|rapid|pulsat)).*");
        Path file = Files.createTempFile("coach-info-content-", ".properties");
        String original = System.getProperty("coach.kegelUi.fixture");
        try {
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) { data.store(writer, null); }
            System.setProperty("coach.kegelUi.fixture", file.toString());
            new KegelCasePageObject(new RemoteWebDriver() { }).assertInformationContent("FLICKER", content);
        } finally {
            if (original == null) System.clearProperty("coach.kegelUi.fixture");
            else System.setProperty("coach.kegelUi.fixture", original);
            Files.deleteIfExists(file);
        }
    }

    @Test public void reviewedFlickerInstructionPasses() throws Exception {
        check("Contract and relax your pelvic floor muscles as quickly as possible, creating a pulsating sensation", true);
    }
    @Test public void equivalentRapidReleaseWordingPasses() throws Exception {
        check("Rapidly tighten the pelvic floor muscles and release them", true);
    }
    @Test(expected = AssertionError.class) public void sustainedSqueezeCannotPassAsFlicker() throws Exception {
        check("Contract your pelvic floor muscle to 70-80% strength, hold for 15 seconds, then relax", true);
    }
    @Test(expected = AssertionError.class) public void genericMuscleTextCannotPassAsInstruction() throws Exception {
        check("Learn about your pelvic floor muscles", true);
    }
    @Test public void legacySubstringContractRemainsAvailable() throws Exception {
        check("Learn about your pelvic floor muscles", false);
    }
}
