package tests;

import lib.UpdateEvidence;
import org.junit.Assert;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

public class UpdateEvidenceUnitTests {
    @Test public void sameUidAndActiveSubscriptionArePreserved() {
        UpdateEvidence.assertAccountPreserved(account("fixture", true), account("fixture", true));
    }

    @Test public void changedUidLostAndInitiallyInactiveSubscriptionFail() {
        rejects(() -> UpdateEvidence.assertAccountPreserved(account("fixture", true), account("other", true)));
        rejects(() -> UpdateEvidence.assertAccountPreserved(account("fixture", true), account("fixture", false)));
        rejects(() -> UpdateEvidence.assertAccountPreserved(account("fixture", false), account("fixture", false)));
    }

    @Test public void validBuildMetadataIsAccepted() {
        UpdateEvidence.validateIOSBuild(build("123.4", "bundle", "iphonesimulator"), "bundle");
    }

    @Test public void placeholderMissingWrongBundleAndDeviceBuildAreRejected() {
        for (String number : new String[]{"BITRISE_BUILD_NUMBER", "", null}) {
            rejects(() -> UpdateEvidence.validateIOSBuild(build(number, "bundle", "iphonesimulator"), "bundle"));
        }
        rejects(() -> UpdateEvidence.validateIOSBuild(build("123", "other", "iphonesimulator"), "bundle"));
        rejects(() -> UpdateEvidence.validateIOSBuild(build("123", "bundle", "iphoneos"), "bundle"));
    }

    @Test public void physicalIosInstalledMetadataUsesTheRequestedApplication() {
        Map<String,Object> app = new HashMap<>();
        app.put("bundleIdentifier", "expected.bundle"); app.put("version", "1.14.0"); app.put("bundleVersion", "3582");
        Map<String,Object> result = new HashMap<>(); result.put("apps", java.util.Collections.singletonList(app));
        Map<String,Object> response = new HashMap<>(); response.put("result", result);
        Assert.assertEquals("1.14.0", UpdateEvidence.iosDeviceInstalledBuild(response, "expected.bundle").get("version"));
        Assert.assertEquals("3582", UpdateEvidence.iosDeviceInstalledBuild(response, "expected.bundle").get("build"));
        rejects(() -> UpdateEvidence.iosDeviceInstalledBuild(response, "another.bundle"));
        app.remove("bundleVersion");
        rejects(() -> UpdateEvidence.iosDeviceInstalledBuild(response, "expected.bundle"));
    }

    @Test public void anEmptyOrAmbiguousDeviceAppListCannotSupplyTheBuild() {
        Map<String,Object> response = new HashMap<>();
        rejects(() -> UpdateEvidence.iosDeviceInstalledBuild(response, "bundle"));
        Map<String,Object> result = new HashMap<>(); response.put("result", result);
        result.put("apps", java.util.Collections.emptyList());
        rejects(() -> UpdateEvidence.iosDeviceInstalledBuild(response, "bundle"));
        result.put("apps", java.util.Arrays.asList(new HashMap<>(), new HashMap<>()));
        rejects(() -> UpdateEvidence.iosDeviceInstalledBuild(response, "bundle"));
    }

    @Test public void androidInstalledMetadataRequiresBothVersionAndBuild() {
        Map<String,String> info = UpdateEvidence.androidInstalledBuild("versionCode=353 minSdk=23\nversionName=1.44.1\n");
        Assert.assertEquals("1.44.1", info.get("version"));
        Assert.assertEquals("353", info.get("build"));
        rejects(() -> UpdateEvidence.androidInstalledBuild("versionName=1.44.1\n"));
        rejects(() -> UpdateEvidence.androidInstalledBuild("versionCode=353\n"));
    }

    private Map<String,Object> account(String uid, boolean active) {
        Map<String,Object> state = new HashMap<>(); state.put("uid", uid); state.put("subscriptionActive", active); return state;
    }

    private Map<String,Object> build(String number, String bundle, String platform) {
        Map<String,Object> info = new HashMap<>(); info.put("CFBundleVersion", number);
        info.put("CFBundleIdentifier", bundle); info.put("DTPlatformName", platform); return info;
    }

    private void rejects(Runnable check) {
        try { check.run(); } catch (AssertionError expected) { return; }
        Assert.fail("Invalid update evidence was accepted");
    }
}
