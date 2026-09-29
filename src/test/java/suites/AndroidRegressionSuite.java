package suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.ModulesTests;
import tests.AndroidProgramSelectorTests;
import tests.ExploreTests;

/** Reuses shared scenarios; fresh-install, billing and mutable-fixture suites run separately. */
@RunWith(Suite.class)
@Suite.SuiteClasses({ModulesTests.class, AndroidProgramSelectorTests.class, ExploreTests.class})
public class AndroidRegressionSuite { }
