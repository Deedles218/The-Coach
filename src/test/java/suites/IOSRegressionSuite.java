package suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.CoachProfileTests;
import tests.ConfiguredExploreTests;
import tests.DailyPlanTests;
import tests.IOSSmokeTests;

/** Existing-progress profile; isolated and legacy suites keep their own entry points. */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        IOSSmokeTests.class,
        DailyPlanTests.class,
        CoachProfileTests.class,
        ConfiguredExploreTests.class
})
public class IOSRegressionSuite { }
