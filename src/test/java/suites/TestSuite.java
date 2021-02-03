package suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.*;
import tests.HW.ArticleTests;
import tests.HW.ChangeAppCondition;
import tests.HW.GetStartedTest;
import tests.HW.MyListsTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ArticleTests.class,
        ChangeAppCondition.class,
        GetStartedTest.class,
        MyListsTests.class,
        SearchTests.class
})
public class TestSuite {
}
