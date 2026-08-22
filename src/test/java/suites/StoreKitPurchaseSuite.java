package suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.StoreKitPurchaseTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        StoreKitPurchaseTests.class
})
public class StoreKitPurchaseSuite {
}
