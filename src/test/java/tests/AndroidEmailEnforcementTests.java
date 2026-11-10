package tests;

import io.qameta.allure.Feature;
import lib.EmailEnforcementFixture;
import lib.Platform;
import lib.ui.EmailEnforcementScreen;
import lib.ui.android.AndroidEmailEnforcementPageObject;
import org.junit.Assert;

/** Same reviewed Xray scenarios; only the UI implementation and fixture reader differ. */
@Feature("COA-9222 / COA-9619 Mandatory email collection — Android")
public class AndroidEmailEnforcementTests extends IOSEmailEnforcementTests {
    private AndroidEmailEnforcementPageObject screen;
    @Override protected void requireScenarioPlatform() {
        Assert.assertTrue("Run this suite with -Dplatform=android", Platform.getInstance().isAndroid());
    }

    @Override protected EmailEnforcementScreen createEmailScreen(EmailEnforcementFixture fixture) {
        io.qameta.allure.Allure.getLifecycle().updateTestCase(result -> {
            String dataset = fixture.appId() + "/" + fixture.stage() + "/" + fixture.value("dismiss.action", "cross");
            result.getParameters().add(new io.qameta.allure.model.Parameter().setName("dataset").setValue(dataset));
            result.setHistoryId(java.util.UUID.nameUUIDFromBytes((result.getFullName() + dataset)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
        });
        screen = new AndroidEmailEnforcementPageObject(driver, fixture);
        return screen;
    }

    @Override protected void reachDailyWithoutDismissingRequest() {
        screen.reachDailyWithoutDismissingRequest();
    }
}
