package suites;

import lib.EmailEnforcementScope;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.AndroidEmailEnforcementTests;

@RunWith(Categories.class)
@Categories.ExcludeCategory({EmailEnforcementScope.NotRequired.class, EmailEnforcementScope.Deferred.class})
@Suite.SuiteClasses(AndroidEmailEnforcementTests.class)
public class AndroidEmailEnforcementSuite { }
