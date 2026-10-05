package suites;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.IOSSmokeTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({IOSSmokeTests.class})
public class IOSParitySmokeSuite { }
