package tests;

import lib.AppearanceSnapshot;
import lib.MobileCaseFixture;
import lib.TestData;
import lib.ui.ios.iOSCoachFlowPageObject;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Properties;

/** Offline negative checks for fixtures, profile identity and rendered appearance. */
public class MobileNextCasesUnitTests {
    @Test public void aWebPaidIosIdentityCanStillNeedEmailConfirmation() {
        new IosProfileState(true, false).assertProfileForEmail("fixture@example.invalid", false);
    }

    @Test(expected = AssertionError.class) public void ordinaryIosLoginStillRequiresConfirmedEmail() {
        new IosProfileState(true, false).assertAuthenticatedProfileForEmail("fixture@example.invalid");
    }

    @Test(expected = AssertionError.class) public void anAnonymousIosProfileCannotBeAPaidIdentity() {
        new IosProfileState(false, true).assertProfileForEmail("fixture@example.invalid", false);
    }

    @Test public void buildContractCannotBeUsedForAnotherPlatformOrApplication() {
        MobileCaseFixture fixture = fixture();
        fixture.validate("ios", "com.vamapps.preprod.The-Coach");
        rejects(() -> fixture.validate("android", "com.vamapps.preprod.The-Coach"), "another platform/application");
        rejects(() -> fixture.validate("ios", "com.vamapps.The-Coach-for-her"), "another platform/application");
    }

    @Test public void missingUnknownAndPlaceholderDataFailClosed() {
        for (String value : new String[]{"", "unknown", "<observed marker>", "TODO"}) {
            Properties data = data(); data.setProperty("COA-7960.legacy.screen", value);
            rejects(() -> new MobileCaseFixture(data, "COA-7960").selector("legacy.screen"), "fixture value missing");
        }
    }

    @Test public void anotherCasesDataCannotSatisfyTheCurrentCase() {
        Properties data = data(); data.setProperty("COA-7963.legacy.screen", "id:observed-marker");
        rejects(() -> new MobileCaseFixture(data, "COA-7960").selector("legacy.screen"), "COA-7960.legacy.screen");
    }

    @Test public void cardChildSelectorsCannotSearchTheWholeScreen() {
        Properties data = data();
        for (String selector : new String[]{"xpath://*[@name='Completed']", "xpath:/hierarchy/node",
                "xpath:.//node/ancestor::*", "xpath:.//node | //unrelated-card", "xpath:.//node/../other"}) {
            data.setProperty("COA-7960.status", selector);
            rejects(() -> new MobileCaseFixture(data, "COA-7960").relativeSelector("status"), "must be relative");
        }
        data.setProperty("COA-7960.status", "xpath:.//*[@name='Completed']");
        Assert.assertEquals("xpath:.//*[@name='Completed']", new MobileCaseFixture(data, "COA-7960").relativeSelector("status"));
    }

    @Test public void malformedCountsAndSelectorsAreRejected() {
        Properties data = data();
        for (String count : new String[]{"0", "-1", "2.5", "not-a-count"}) {
            data.setProperty("COA-7960.count", count);
            rejects(() -> new MobileCaseFixture(data, "COA-7960").positiveInt("count"), "positive integer");
        }
        data.setProperty("COA-7960.marker", "id:");
        rejects(() -> new MobileCaseFixture(data, "COA-7960").selector("marker"), "observed id:/xpath:");
    }

    @Test public void unchangedReadableRegionsMatch() {
        Assert.assertTrue(AppearanceSnapshot.sameRendering(region(false), region(false)));
    }

    @Test public void identicalBlankRegionsCannotPassTheAppearanceCheck() {
        BufferedImage blank = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        Assert.assertFalse(AppearanceSnapshot.hasVisibleContrast(blank));
        Assert.assertFalse(AppearanceSnapshot.sameRendering(blank, blank));
    }

    @Test public void changedLayoutAndLostTextFailTheAppearanceCheck() {
        Assert.assertFalse(AppearanceSnapshot.sameRendering(region(false), region(true)));
        Assert.assertFalse(AppearanceSnapshot.sameRendering(region(false), new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB)));
    }

    private static final class IosProfileState extends iOSCoachFlowPageObject {
        private final boolean confirm, connect;
        private IosProfileState(boolean confirm, boolean connect) { super(null); this.confirm = confirm; this.connect = connect; }
        @Override public WebElement waitForElementVisible(String locator, String message, long timeout) { return null; }
        @Override public boolean isElementVisible(String locator) {
            return ("id:CONFIRM".equals(locator) && confirm) || ("id:CONNECT".equals(locator) && connect);
        }
    }

    private static MobileCaseFixture fixture() { return new MobileCaseFixture(data(), "COA-7960"); }

    private static Properties data() {
        Properties data = new Properties();
        data.setProperty("platform", "ios"); data.setProperty("app.id", "com.vamapps.preprod.The-Coach");
        data.setProperty("source.version", "offline-fixture-only"); data.setProperty("environment", "preprod");
        data.setProperty("COA-7960.program", "Last Longer"); data.setProperty("COA-7960.day", "Day 2");
        return data;
    }

    private static BufferedImage region(boolean moved) {
        BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 40, 20);
            graphics.setColor(Color.BLACK); graphics.fillRect(moved ? 22 : 3, 5, 10, 8);
        } finally { graphics.dispose(); }
        return image;
    }

    private interface Action { void run() throws Exception; }
    private static void rejects(Action action, String message) {
        try { action.run(); Assert.fail("Invalid fixture/evidence was accepted"); }
        catch (IllegalArgumentException | IllegalStateException expected) { Assert.assertTrue(expected.getMessage(), expected.getMessage().contains(message)); }
        catch (Exception unexpected) { throw new AssertionError(unexpected); }
    }
}
