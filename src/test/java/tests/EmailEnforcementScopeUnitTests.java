package tests;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Request;
import suites.IOSEmailEnforcementSuite;
import java.util.*;

public class EmailEnforcementScopeUnitTests {
    @Test public void androidSuiteRetainsTheSameSixCasesAndExclusions() {
        Set<String> cases = new HashSet<>();
        collect(Request.aClass(suites.AndroidEmailEnforcementSuite.class).getRunner().getDescription(), cases);
        Assert.assertEquals(new HashSet<>(Arrays.asList("COA-9411", "COA-9424", "COA-9696",
                "COA-9699", "COA-9701", "COA-9702")), cases);
    }

    @Test public void activeSuiteContainsOnlyTheSixApprovedCases() {
        Set<String> cases = new HashSet<>();
        collect(Request.aClass(IOSEmailEnforcementSuite.class).getRunner().getDescription(), cases);
        Assert.assertEquals(new HashSet<>(Arrays.asList("COA-9411", "COA-9424", "COA-9696",
                "COA-9699", "COA-9701", "COA-9702")), cases);
    }

    private void collect(Description description, Set<String> cases) {
        if (description.isTest()) {
            Issue issue = description.getAnnotation(Issue.class);
            Assert.assertNotNull("Every active scenario must retain its Jira key", issue);
            cases.add(issue.value());
        }
        for (Description child : description.getChildren()) collect(child, cases);
    }
}
