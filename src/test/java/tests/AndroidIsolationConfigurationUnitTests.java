package tests;

import lib.TestData;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class AndroidIsolationConfigurationUnitTests {
    @After
    public void clearProperties() {
        System.clearProperty("android.app.variant");
        System.clearProperty("android.appPackage");
        System.clearProperty("android.recovery.mode");
        System.clearProperty("coach.account.variant");
    }

    @Test
    public void matchingFemaleVariantIsAccepted() {
        System.setProperty("android.app.variant", "female");
        System.setProperty("coach.account.variant", "female");

        TestData.validateAndroidAccountVariantIfConfigured();
        Assert.assertEquals("female", TestData.androidAppVariant());
    }

    @Test(expected = IllegalStateException.class)
    public void mismatchedAccountVariantIsRejected() {
        System.setProperty("android.app.variant", "female");
        System.setProperty("coach.account.variant", "male");

        TestData.validateAndroidAccountVariantIfConfigured();
    }

    @Test(expected = IllegalStateException.class)
    public void unsupportedRecoveryModeIsRejected() {
        System.setProperty("android.recovery.mode", "clear-data");

        TestData.validateAndroidRecoveryMode();
    }
}
