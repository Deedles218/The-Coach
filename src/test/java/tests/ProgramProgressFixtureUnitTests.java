package tests;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.util.*;

/** A reset must be rejected before creating a driver for a saved-progress fixture. */
public class ProgramProgressFixtureUnitTests {
    private static final class DriverReached extends RuntimeException { }
    private static final class Probe extends AndroidProgramProgressTests {
        boolean reached;
        @Override protected RemoteWebDriver createDriver() { reached = true; throw new DriverReached(); }
    }
    private void check(boolean noReset, boolean fullReset, boolean expectDriver) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("platform", "android"); values.put("android.noReset", String.valueOf(noReset));
        values.put("android.fullReset", String.valueOf(fullReset)); values.put("android.app", "");
        Map<String, String> before = new HashMap<>();
        for (String key : values.keySet()) { before.put(key, System.getProperty(key)); System.setProperty(key, values.get(key)); }
        Probe probe = new Probe();
        try {
            try { probe.setUp(); Assert.fail("Probe did not stop at the expected boundary"); }
            catch (DriverReached reached) { Assert.assertTrue("Reset mode reached driver creation", expectDriver); }
            catch (AssertionError rejected) { Assert.assertFalse("Preserved mode was rejected", expectDriver);
                Assert.assertTrue(rejected.getMessage(), rejected.getMessage().contains("noReset=true")); }
            Assert.assertEquals("Unexpected driver creation", expectDriver, probe.reached);
        } finally {
            for (String key : values.keySet()) if (before.get(key) == null) System.clearProperty(key); else System.setProperty(key, before.get(key));
        }
    }
    @Test public void fullResetWithoutAnApkStillCannotReachTheDriver() throws Exception { check(true, true, false); }
    @Test public void fastResetCannotReachTheDriver() throws Exception { check(false, false, false); }
    @Test public void preservedModeCanReachTheDriver() throws Exception { check(true, false, true); }
}
