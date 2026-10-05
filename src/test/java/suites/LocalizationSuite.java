package suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.LocalizationTests;

/** Localization UI matrix for one platform/build; launch with mvn test -Plocalization. */
@RunWith(Suite.class)
@Suite.SuiteClasses({LocalizationTests.class})
public class LocalizationSuite { }
