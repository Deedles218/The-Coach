package suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.ReleaseSmokeTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ReleaseSmokeTests.class
})
public class ReleaseSmokeSuite {
}
