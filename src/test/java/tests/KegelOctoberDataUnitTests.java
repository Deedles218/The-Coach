package tests;

import lib.TestData;
import org.junit.Assert;
import org.junit.Test;

/** Credential/link protection for failure XML and report attachments, without a device. */
public class KegelOctoberDataUnitTests {
    @Test
    public void octoberAccountsAndMarketingLinkAreRedactedFromArtifacts() throws Exception {
        for (String caseId : new String[]{"coa8094", "coa8096", "coa8170", "coa8171", "coa7937"}) {
            for (String field : new String[]{"email", "otp", "link"}) {
                String property = "coach." + caseId + "." + field;
                String original = System.getProperty(property);
                String secret = "private-" + caseId + "-" + field;
                try {
                    System.setProperty(property, secret);
                    String artifact = "<node value=\"" + secret + "\">" + secret + "</node>";
                    String redacted = TestData.sanitizeSensitiveData(artifact);
                    Assert.assertFalse("Secret leaked into failure artifact", redacted.contains(secret));
                    Assert.assertTrue("Sanitizer removed unrelated artifact content", redacted.contains("<node value="));
                    if ("link".equals(field)) {
                        javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                                new org.xml.sax.InputSource(new java.io.StringReader(redacted)));
                    }
                } finally {
                    if (original == null) System.clearProperty(property);
                    else System.setProperty(property, original);
                }
            }
        }
    }
}
