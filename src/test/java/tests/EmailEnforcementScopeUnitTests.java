package tests;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Request;
import suites.IOSEmailEnforcementSuite;
import java.util.*;

public class EmailEnforcementScopeUnitTests {
    @Test public void iosAndAndroidReleaseSuitesRunTheSameTenMethods() {
        Set<String> android = releaseMethods(suites.AndroidEmailReleaseRegressionSuite.class);
        Set<String> ios = releaseMethods(suites.IOSEmailReleaseRegressionSuite.class);
        Assert.assertEquals(android, ios);
        Assert.assertEquals(10, ios.size());
        Assert.assertTrue(ios.contains("testCoa9807ExistingAccountCopyInSpanish"));
    }

    private Set<String> releaseMethods(Class<?> suite) {
        Set<String> methods = new HashSet<>();
        for (Description testClass : Request.aClass(suite).getRunner().getDescription().getChildren()) {
            for (Description test : testClass.getChildren()) {
                Assert.assertTrue("Expected a JUnit test in the release suite", test.isTest());
                Assert.assertTrue("Duplicate release scenario", methods.add(test.getMethodName()));
            }
        }
        return methods;
    }

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
