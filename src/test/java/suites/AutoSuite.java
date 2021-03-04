package suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.ChatTests;
import tests.ProfileTests;
import tests.SearchTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({
       // SearchTests.class,
        ProfileTests.class
        //ChatTests.class
})
public class AutoSuite {
}
