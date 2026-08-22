package suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.PushPermissionTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        PushPermissionTests.class
})
public class PushPermissionSuite {
}
