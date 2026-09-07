package tests;

import lib.SimulatorTestIdentity;
import org.junit.Test;

/** No Appium, account operations or filesystem mutation. */
public class TestModelMailFixtureUnitTests {
    private static final String UID = "ownedAnonymousFixture123456789";

    @Test public void recordedAnonymousFixtureCanResume() {
        SimulatorTestIdentity.assertOwnedAnonymousMailFixture(UID, UID);
    }

    @Test(expected = IllegalStateException.class)
    public void differentAnonymousAccountCannotResume() {
        SimulatorTestIdentity.assertOwnedAnonymousMailFixture("anotherAnonymousFixture123456", UID);
    }

    @Test(expected = IllegalStateException.class)
    public void missingOwnershipCannotResume() {
        SimulatorTestIdentity.assertOwnedAnonymousMailFixture(UID, null);
    }

    @Test(expected = IllegalStateException.class)
    public void missingCurrentIdentityCannotResume() {
        SimulatorTestIdentity.assertOwnedAnonymousMailFixture(null, UID);
    }

    @Test(expected = IllegalStateException.class)
    public void invalidIdentityCannotBeRecorded() {
        SimulatorTestIdentity.assertOwnedAnonymousMailFixture("invalid", "invalid");
    }
}
