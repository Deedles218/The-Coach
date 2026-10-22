package suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.BookmarksTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({BookmarksTests.class})
public class BookmarksSuite {
}
