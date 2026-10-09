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
