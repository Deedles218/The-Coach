package tests;

import lib.TestData;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Synthetic identifiers only; never reads a simulator, account or Keychain. */
public class IOSRegressionIdentityUnitTests {
    @Test public void matchingIdentityStillPassesWithTrimmedConfiguration() {
        IOSRegressionPreflightTests.assertFixtureIdentity("  synthetic-expected-uid  ", "synthetic-expected-uid");
    }

    @Test public void wrongIdentityFailsWithoutEchoingExpectedOrActualUid() {
        assertPrivateMismatch("synthetic-expected-uid", "synthetic-unexpected-uid");
    }

    @Test public void missingIdentitiesCannotSatisfyTheIdentityGate() {
        assertPrivateMismatch(null, null);
        assertPrivateMismatch("  ", "  ");
        assertPrivateMismatch("synthetic-expected-uid", null);
    }

    private void assertPrivateMismatch(String expected, String actual) {
        AssertionError failure = null;
        try { IOSRegressionPreflightTests.assertFixtureIdentity(expected, actual); }
        catch (AssertionError mismatch) { failure = mismatch; }
        Assert.assertNotNull("Wrong identity was accepted", failure);
        Assert.assertEquals("Wrong fixture identity", failure.getMessage());
        StringWriter trace = new StringWriter();
        failure.printStackTrace(new PrintWriter(trace));
        if (expected != null && !expected.trim().isEmpty()) Assert.assertFalse(trace.toString().contains(expected.trim()));
        if (actual != null && !actual.trim().isEmpty()) Assert.assertFalse(trace.toString().contains(actual.trim()));
    }

    @Test public void configuredUidsAreRedactedFromFailureArtifacts() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("coach.expectedAccount.uid", "synthetic-expected-uid");
        values.put("coach.coa9044.uid", "synthetic-module-uid");
        values.put("coach.bookmarks.content.uid", "synthetic-bookmarks-uid");
        withProperties(values, () -> Assert.assertEquals("[redacted-uid] [redacted-uid] [redacted-uid]",
                TestData.sanitizeSensitiveData("synthetic-expected-uid synthetic-module-uid synthetic-bookmarks-uid")));
    }

    @Test public void uidRedactionHandlesTrimmedOverlappingValuesAndMixedCaseKeys() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("coach.expectedAccount.uid", "  synthetic-uid  ");
        values.put("coach.fixture.UID", "synthetic-uid-long");
        withProperties(values, () -> Assert.assertEquals("[redacted-uid] [redacted-uid] [redacted-uid]",
                TestData.sanitizeSensitiveData("  synthetic-uid   synthetic-uid-long synthetic-uid")));
    }

    @Test public void uidReplacementPreservesXmlAttributeSyntax() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("coach.expectedAccount.uid", "synthetic-xml-uid");
        withProperties(values, () -> {
            String sanitized = TestData.sanitizeSensitiveData("<node value=\"synthetic-xml-uid\"/>");
            Assert.assertEquals("<node value=\"[redacted-uid]\"/>", sanitized);
            javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new org.xml.sax.InputSource(new StringReader(sanitized)));
        });
    }

    @Test public void blankUidsAndUnrelatedFixtureIdsDoNotRedactArtifactContent() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("coach.expectedAccount.uid", "  ");
        values.put("coach.fixture.id", "fixture-identifier");
        withProperties(values, () -> Assert.assertEquals("fixture-identifier remains readable",
                TestData.sanitizeSensitiveData("fixture-identifier remains readable")));
        Assert.assertEquals("", TestData.sanitizeSensitiveData(null));
    }

    private interface CheckedAction { void run() throws Exception; }

    private void withProperties(Map<String, String> values, CheckedAction action) throws Exception {
        Map<String, String> original = new LinkedHashMap<>();
        for (String key : values.keySet()) original.put(key, System.getProperty(key));
        try {
            values.forEach(System::setProperty);
            action.run();
        } finally {
            original.forEach((key, value) -> {
                if (value == null) System.clearProperty(key); else System.setProperty(key, value);
            });
        }
    }
}
