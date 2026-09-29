package tests;

import io.qameta.allure.*;
import lib.*;
import lib.ui.factories.CoachFlowPageObjectFactory;
import lib.ui.ModulesPageObject;
import lib.ui.factories.ModulesPageObjectFactory;
import org.junit.*;
import org.junit.runner.Description;
import java.util.Map;

@Epic("Today")
@Feature("Modules")
public class ModulesTests extends CoreTestCase {
    private ModulesPageObject modules;
    private ModuleStage original;
    private String program;

    @Override protected boolean isPlatformSupported() {
        return (Platform.getInstance().isIOS() || Platform.getInstance().isAndroid())
                && Boolean.getBoolean("coach.modules.enabled");
    }

    @Override protected String unsupportedPlatformMessage() {
        return "Modules require iOS Simulator or Android test emulator and -Dcoach.modules.enabled=true; see docs/modules-automation.md";
    }

    @Before @Override public void setUp() throws Exception {
        // Fail configuration before opening an Appium session in an explicit run.
        if (isPlatformSupported()) {
            TestData.testModelAccount("COA-9044");
            Assert.assertNotNull("Dedicated modules UID is required", System.getenv("COACH_COA9044_UID"));
            if (Platform.getInstance().isIOS()) {
                Assert.assertEquals("iOS module evidence supports preprod only", "com.vamapps.preprod.The-Coach",
                        Platform.getInstance().getIOSBundleId());
            } else {
                Assert.assertEquals("Android module evidence supports the approved manProd build",
                        "com.vamapps.thecoach", Platform.getInstance().getAndroidAppPackage());
            }
            program = System.getProperty("coach.modules.programId");
            Assert.assertTrue("Provide -Dcoach.modules.programId=<prepared program_id>",
                    program != null && program.matches("[a-z0-9_]{1,80}"));
        }
        super.setUp();
    }

    private void prepare() throws Exception {
        TestData.TestAccount account = TestData.testModelAccount("COA-9044");
        long boundary = System.currentTimeMillis();
        CoachFlowPageObjectFactory.get(driver).ensureExistingProgressUserIsLoggedIn(account.getEmail(), account.getOtp());
        Assert.assertEquals("Unexpected account; no module navigation performed",
                System.getenv("COACH_COA9044_UID"), Platform.getInstance().isAndroid()
                        ? AndroidModuleEvidence.currentUid() : SimulatorTestIdentity.currentUid());
        modules = ModulesPageObjectFactory.get(driver);
        modules.openTodayTab();
        original = modules.position();
        Assert.assertEquals("New isolated fixture must start in module 1", 1, original.module);
        Assert.assertEquals("New isolated fixture must start at stage 1", 1, original.stage);
        verifyIncompleteResponse(boundary, original);
    }

    @SuppressWarnings("unchecked")
    private void verifyIncompleteResponse(long boundary, ModuleStage expected) throws Exception {
        Map<String,Object> content = Platform.getInstance().isAndroid()
                ? AndroidModuleEvidence.readIncompleteModule(program, expected, boundary)
                : SimulatorProgramContent.readIncompleteModule(program, boundary);
        Map<String,Object> metadata = (Map<String,Object>) content.get("sectionMetadata");
        ModuleStage observed = new ModuleStage(((Number) metadata.get("module_order")).intValue(),
                ((Number) metadata.get("module_current_day")).intValue(),
                ((Number) metadata.get("module_total_days")).intValue());
        Assert.assertEquals("UI and fresh fixture response disagree", expected, observed);
        Assert.assertEquals("Evidence collection changed the UI-selected stage", expected, modules.position());
    }

    private void move(int direction) throws Exception {
        ModuleStage before = modules.position();
        ModuleStage expected = before.at(before.stage + direction);
        long boundary = direction > 0 ? modules.next() : modules.previous();
        modules.waitForPosition(expected);
        verifyIncompleteResponse(boundary, expected);
    }

    private void goToLastStage() throws Exception {
        for (int stage = original.stage; stage < original.total; stage++) move(1);
        Assert.assertEquals(original.at(original.total), modules.position());
    }

    private void runScenario(String name, RestoringTestAction.CheckedAction action) throws Exception {
        RestoringTestAction.run(() -> {
            try {
                prepare();
                action.run();
            } catch (Exception | AssertionError failure) {
                captureFailureArtifacts(failure, Description.createTestDescription(getClass(), name + "BeforeRestore"));
                throw failure;
            }
        }, () -> {
            if (original != null) {
                modules.restore(original);
                Allure.addAttachment("Modules fixture restoration", original.toString());
            }
        });
    }

    @Test @Issue("COA-9015") @Issue("COA-9016")
    public void testModuleTitleAndStageAreVisible() throws Exception {
        runScenario("modulePresence", () -> Assert.assertEquals(original, modules.position()));
    }

    @Test @Issue("COA-9014")
    @io.qameta.allure.Description("Partial visual coverage: header bounds and peer overlap only; no glyph clipping or safe-area oracle.")
    public void testHeaderBoundsAndPeerOverlap() throws Exception {
        runScenario("headerBounds", () -> modules.assertHeaderBounds());
    }

    @Test @Issue("COA-9044")
    public void testNextStageWithoutCompletingActivities() throws Exception {
        runScenario("nextStage", () -> move(1));
    }

    @Test @Issue("COA-9045")
    public void testPreviousStagesBackToFirst() throws Exception {
        runScenario("previousStages", () -> {
            goToLastStage();
            for (int stage = original.total; stage > 1; stage--) move(-1);
            Assert.assertEquals(original, modules.position());
        });
    }

    @Test @Issue("COA-9046")
    public void testLastStageIsAccessibleWithoutCompletion() throws Exception {
        runScenario("lastStage", this::goToLastStage);
    }

    @Test @Issue("COA-9047")
    public void testPreviousFromLastStage() throws Exception {
        runScenario("previousFromLast", () -> {
            goToLastStage();
            move(-1);
            Assert.assertEquals(original.at(original.total - 1), modules.position());
        });
    }

    @Test @Issue("COA-9048") @Issue("COA-9049") @Issue("COA-9050") @Issue("COA-9051")
    public void testNextModuleBlockedAndGotItRetainsStage() throws Exception {
        runScenario("blockedModule", () -> {
            goToLastStage();
            ModuleStage last = modules.position();
            modules.next();
            modules.assertBlocked(last);
            // Dismissal must not unlock the next module on a subsequent attempt.
            modules.next();
            modules.assertBlocked(last);
        });
    }
}
