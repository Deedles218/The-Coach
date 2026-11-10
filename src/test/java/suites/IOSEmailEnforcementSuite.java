package suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.experimental.categories.Categories;
import lib.EmailEnforcementScope;
import tests.IOSEmailEnforcementTests;

@RunWith(Categories.class)
@Categories.ExcludeCategory({EmailEnforcementScope.NotRequired.class, EmailEnforcementScope.Deferred.class})
@Suite.SuiteClasses({IOSEmailEnforcementTests.class})
public class IOSEmailEnforcementSuite { }
