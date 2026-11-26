package tests;

import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.appmanagement.ApplicationState;
import lib.ui.ios.iOSBookmarksPageObject;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A background interruption may be recovered; a crash must remain a failure. */
public class BookmarksRecoveryUnitTests {
    private void verify(ApplicationState state, boolean expected) throws Exception {
        List<String> activations = new ArrayList<>();
        InteractsWithApps apps = (InteractsWithApps) Proxy.newProxyInstance(
                InteractsWithApps.class.getClassLoader(), new Class<?>[]{InteractsWithApps.class},
                (proxy, method, arguments) -> {
                    Assert.assertEquals("fixture.bundle", arguments[0]);
                    if ("queryAppState".equals(method.getName())) return state;
                    if ("activateApp".equals(method.getName())) {
                        activations.add((String) arguments[0]);
                        return null;
                    }
                    throw new AssertionError("Unexpected application operation: " + method.getName());
                });
        Method recovery = iOSBookmarksPageObject.class.getDeclaredMethod("reactivateCoachAfterBackgrounding",
                InteractsWithApps.class, String.class, String.class);
        recovery.setAccessible(true);
        Assert.assertEquals(expected, recovery.invoke(null, apps, "fixture.bundle", "unit check"));
        Assert.assertEquals(expected ? Arrays.asList("fixture.bundle") : java.util.Collections.emptyList(), activations);
    }

    @Test public void runningBackgroundAppMayBeReactivated() throws Exception {
        verify(ApplicationState.RUNNING_IN_BACKGROUND, true);
    }
    @Test public void suspendedBackgroundAppMayBeReactivated() throws Exception {
        verify(ApplicationState.RUNNING_IN_BACKGROUND_SUSPENDED, true);
    }
    @Test public void aStoppedOrCrashedAppCannotBeSilentlyRelaunched() throws Exception {
        verify(ApplicationState.NOT_RUNNING, false);
    }
    @Test public void anUninstalledAppCannotBeRecoveredAsABackgroundInterruption() throws Exception {
        verify(ApplicationState.NOT_INSTALLED, false);
    }
    @Test public void aForegroundTimeoutCannotTriggerAnUnrelatedRelaunch() throws Exception {
        verify(ApplicationState.RUNNING_IN_FOREGROUND, false);
    }
}
