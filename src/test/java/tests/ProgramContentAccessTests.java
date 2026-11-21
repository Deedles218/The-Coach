package tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import lib.CoreTestCase;
import lib.MobileCaseFixture;
import lib.Platform;
import lib.PreparedMobileProfile;
import lib.ProgramSelectionApi;
import lib.ProgramSelectionState;
import lib.ui.ios.IOSMyProgramsPageObject;
import lib.ui.ios.IOSProgramCatalogPageObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Epic("COA-9583 Program activation")
@Feature("Content authorization before program membership")
public class ProgramContentAccessTests extends CoreTestCase {
    @Before @Override public void setUp() throws Exception {
        requireIOSPlatform();
        Assert.assertTrue("Use preserved preprod", Platform.getInstance().isTestFlightAttachConfigured()
                && Platform.getInstance().getIOSBundleId().startsWith("com.vamapps.preprod."));
        PreparedMobileProfile.assertPreservationAllowed();
        super.setUp();
    }
    @Override protected void cleanupSessionForNextTest() { }

    @Test @Issue("COA-9796")
    public void unaddedLessonAndPracticeAreBlockedAndBecomeAccessibleAfterActivation() throws Exception {
        MobileCaseFixture f = MobileCaseFixture.load("COA-9796");
        ProgramSelectionApi.assertDedicatedProfile("COA-9796");
        String id = f.required("unadded.id");
        // Resolve both prepared links/routes before the first action. Never derive or guess a content URL.
        for (String kind : new String[]{"lesson", "exercise"}) {
            validateLink(f.required(kind + ".deeplink"));
            f.required(kind + ".content.route");
            f.selector(kind + ".blocked");
            f.selector(kind + ".player.ready");
            f.selector(kind + ".daily.open");
            f.selector(kind + ".player.close");
            Assert.assertEquals("Prepared content must belong to the unadded program", id, f.required(kind + ".program.id"));
        }
        ProgramSelectionState before = ProgramSelectionApi.read();
        Assert.assertEquals("Prepare a paid QA profile", true, before.raw().get("subscription_active"));
        Assert.assertNotNull("Complete onboarding before checking Explore access", before.main());
        Assert.assertEquals("Prepared program is already added", "none",
                ((Map<?, ?>) before.raw().get("catalog_membership")).get(id));
        String title = ProgramOnboardingScenarios.title(ProgramSelectionApi.catalog(), id);
        IOSMyProgramsPageObject today = new IOSMyProgramsPageObject(driver);
        IOSProgramCatalogPageObject page = new IOSProgramCatalogPageObject(driver);
        today.openTodayTab();
        page.openFromSelector();
        page.openUnaddedProgram(title);
        assertPlayersAbsent(page, f);
        // Preview is distinct from lesson/practice players. Exercise the supplied ordinary preview when present.
        if (f.optional("preview.open") != null) {
            page.waitForElementAndClick(f.selector("preview.open"), "Program preview is unavailable", 10);
            page.waitForElementVisible(f.selector("preview.ready"), "Program preview did not open", 15);
            page.waitForElementAndClick(f.selector("preview.close"), "Cannot close program preview", 10);
        }
        for (String kind : new String[]{"lesson", "exercise"}) {
            Map<String, Object> link = new HashMap<>();
            link.put("url", f.required(kind + ".deeplink"));
            link.put("bundleId", Platform.getInstance().getIOSBundleId());
            driver.executeScript("mobile: deepLink", link);
            Map<?, ?> active = (Map<?, ?>) driver.executeScript("mobile: activeAppInfo", Collections.emptyMap());
            Assert.assertEquals("The prepared deep link did not reach the test app", Platform.getInstance().getIOSBundleId(), active.get("bundleId"));
            page.waitForElementVisible(f.selector(kind + ".blocked"), "Unadded content was not blocked in the app", 20);
            assertPlayersAbsent(page, f);
            Assert.assertEquals("Unadded content API must reject access", 403, ProgramSelectionApi.contentStatus(f.required(kind + ".content.route")));
            ProgramSelectionApi.read().assertExactlyEquals(before);
            if (f.optional(kind + ".blocked.close") != null) {
                page.waitForElementAndClick(f.selector(kind + ".blocked.close"), "Cannot leave the blocked content view", 10);
            }
            today.openTodayTab();
        }
        page.openFromSelector();
        page.openUnaddedProgram(title);
        page.addAndStart(title);
        ProgramSelectionState added = ProgramSelectionApi.read();
        List<String> secondary = new ArrayList<>(before.secondaryIds());
        secondary.add(id);
        added.assertRoles(before.mainId(), secondary);
        added.assertActive(id);
        added.assertSameProgress(before);
        for (String kind : new String[]{"lesson", "exercise"}) {
            String card = f.selector(kind + ".daily.open");
            today.swipeUpToFindFirstVisibleElement(new String[]{card}, "Prepared content card is absent", 10);
            today.waitForElementAndClick(card, "Cannot open activated content", 10);
            today.waitForElementVisible(f.selector(kind + ".player.ready"), "Activated content player did not open", 20);
            Assert.assertEquals("Activated content API did not grant access", 200, ProgramSelectionApi.contentStatus(f.required(kind + ".content.route")));
            today.waitForElementAndClick(f.selector(kind + ".player.close"), "Cannot close the unfinished content player", 10);
            if (f.optional(kind + ".player.confirmClose") != null) {
                today.waitForElementAndClick(f.selector(kind + ".player.confirmClose"), "Cannot confirm leaving unfinished content", 10);
            }
            today.openTodayTab();
        }
        ProgramSelectionState after = ProgramSelectionApi.read();
        after.assertSameRoles(added);
        after.assertSameProgress(added);
        after.assertActive(id);
    }

    private void assertPlayersAbsent(IOSProgramCatalogPageObject page, MobileCaseFixture f) {
        for (String kind : new String[]{"lesson", "exercise"}) {
            Assert.assertFalse("An unadded program opened its " + kind + " player", page.isElementVisible(f.selector(kind + ".player.ready")));
        }
    }

    private void validateLink(String link) {
        URI value = URI.create(link);
        String host = value.getHost();
        Assert.assertTrue("Supply the reviewed Coach HTTPS content link", "https".equals(value.getScheme())
                && host != null && (host.endsWith(".the.coach") || host.endsWith(".app.link") || host.endsWith(".page.link"))
                && value.getUserInfo() == null && value.getFragment() == null);
    }
}
