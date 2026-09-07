package suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.TestModelAutomationTests;
import tests.TodayProgramSelectorTests;

/** Reviewed Jira/Xray cases, including the iOS Today selector pool. */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestModelAutomationTests.class,
        TodayProgramSelectorTests.class
})
public class TestModelAutomationSuite {
}
