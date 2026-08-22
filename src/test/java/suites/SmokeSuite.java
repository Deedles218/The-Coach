package suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.SmokeTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        SmokeTests.class
})
public class SmokeSuite {
}
