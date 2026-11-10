package tests;

import lib.EmailEnforcementFixture;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class EmailEnforcementFixtureUnitTests {
    private Map<String, Object> build(String number) {
        Map<String, Object> state = new HashMap<>();
        state.put("bundleId", "com.vamapps.preprod.The-Coach");
        state.put("platform", "iphoneos");
        state.put("build", number);
        return state;
    }

    @Test(expected = AssertionError.class) public void sameBuildCannotMasqueradeAsUpdate() {
        EmailEnforcementFixture.assertNewerBuild(build("3575"), build("3575"));
    }

    @Test(expected = AssertionError.class) public void downgradeCannotMasqueradeAsUpdate() {
        EmailEnforcementFixture.assertNewerBuild(build("3575"), build("3574"));
    }

    @Test(expected = AssertionError.class) public void anotherAppCannotBeInstalledAsUpdate() {
        Map<String, Object> update = build("3576");
        update.put("bundleId", "com.vamapps.preprod.The-Coach-for-her");
        EmailEnforcementFixture.assertNewerBuild(build("3575"), update);
    }

    @Test(expected = AssertionError.class) public void switchingAccountsIsNotSuccessfulPreservation() {
        Map<String, Object> before = new HashMap<>();
        before.put("identity", "original");
        Map<String, Object> after = new HashMap<>();
        after.put("identity", "another");
        EmailEnforcementFixture.assertPreserved(before, after);
    }

    @Test public void genuinelyNewerBuildIsAccepted() {
        EmailEnforcementFixture.assertNewerBuild(build("3575"), build("3576"));
        EmailEnforcementFixture.assertNewerBuild(build("3575.1"), build("3575.2"));
    }
}
