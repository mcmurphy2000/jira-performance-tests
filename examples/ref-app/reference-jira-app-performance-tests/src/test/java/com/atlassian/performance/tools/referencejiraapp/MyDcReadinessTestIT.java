package com.atlassian.performance.tools.referencejiraapp;

import com.atlassian.performance.tools.aws.api.Aws;
import com.atlassian.performance.tools.infrastructure.api.app.AppSource;
import com.atlassian.performance.tools.jiraperformancetests.api.LocalApp;
import com.atlassian.performance.tools.referencejiraapp.aws.MyAws;
import org.junit.Test;

import java.io.File;

/**
 * Tests the Data Center Readiness of your app.
 * <p>
 * Summary of test results (i.e. mean latency) can be found in <tt></tt>`summary-per-cohort.csv`</tt> file.
 * You should have it in <tt>./target/jpt-workspace/$timestamp/dc-readiness-tests/`</tt> directory.
 * <p>
 * For the "Endpoint testing" you should submit 'on-one-node-without-app' vs 'on-one-node' cohorts,
 * while for "Scale testing" you should submit 'one-one-node', 'on-two-nodes', 'on-four-nodes' results.
 * Ideally converted to a nice chart. For details visit the DC readiness guide.
 *
 * @see <a href="https://developer.atlassian.com/platform/marketplace/dc-apps-performance-and-scale-testing/">DC readiness guide</a>
 */
public class MyDcReadinessTestIT {

    @Test
    public void shouldTestDcReadiness() {
        final AppSource app = new LocalApp(
            new File("../reference-jira-app/target/reference-jira-app-1.0-SNAPSHOT.obr")
        );
        final File virtualUsersJar = new File("target/reference-jira-app-performance-tests-1.0-SNAPSHOT-fat-tests.jar");
        final Aws aws = new MyAws().aws;
        final DcReadiness readiness = new DcReadiness(app, aws, virtualUsersJar);
        readiness.test();
    }
}