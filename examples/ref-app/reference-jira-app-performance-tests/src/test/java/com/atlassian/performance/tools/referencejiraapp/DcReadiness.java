package com.atlassian.performance.tools.referencejiraapp;

import com.atlassian.performance.tools.aws.api.Aws;
import com.atlassian.performance.tools.aws.api.Investment;
import com.atlassian.performance.tools.awsinfrastructure.api.DatasetCatalogue;
import com.atlassian.performance.tools.awsinfrastructure.api.InfrastructureFormula;
import com.atlassian.performance.tools.awsinfrastructure.api.TargetingVirtualUserOptions;
import com.atlassian.performance.tools.awsinfrastructure.api.jira.DataCenterFormula;
import com.atlassian.performance.tools.awsinfrastructure.api.loadbalancer.ElasticLoadBalancerFormula;
import com.atlassian.performance.tools.awsinfrastructure.api.virtualusers.Ec2VirtualUsersFormula;
import com.atlassian.performance.tools.infrastructure.api.app.AppSource;
import com.atlassian.performance.tools.infrastructure.api.app.Apps;
import com.atlassian.performance.tools.infrastructure.api.app.NoApp;
import com.atlassian.performance.tools.infrastructure.api.dataset.Dataset;
import com.atlassian.performance.tools.infrastructure.api.distribution.ProductDistribution;
import com.atlassian.performance.tools.infrastructure.api.distribution.PublicJiraSoftwareDistribution;
import com.atlassian.performance.tools.infrastructure.api.jira.JiraNodeConfig;
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario;
import com.atlassian.performance.tools.jiraperformancetests.api.ProvisioningPerformanceTest;
import com.atlassian.performance.tools.jirasoftwareactions.api.JiraSoftwareScenario;
import com.atlassian.performance.tools.report.api.FullReport;
import com.atlassian.performance.tools.report.api.StandardTimeline;
import com.atlassian.performance.tools.report.api.judge.FailureJudge;
import com.atlassian.performance.tools.report.api.result.EdibleResult;
import com.atlassian.performance.tools.report.api.result.RawCohortResult;
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad;
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions;
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior;
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget;
import com.atlassian.performance.tools.workspace.api.RootWorkspace;
import com.atlassian.performance.tools.workspace.api.TestWorkspace;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

class DcReadiness {

    private final AppSource app;
    private final Aws aws;
    private final File vuJar;
    private final Class<? extends Scenario> scenario = JiraSoftwareScenario.class;
    private final ProductDistribution product = new PublicJiraSoftwareDistribution("7.5.0");
    private final Dataset dataset = new DatasetCatalogue().largeJiraSeven();
    private final Duration duration = Duration.ofMinutes(20);
    private final Path outputDirectory = Paths.get("target");
    private final String appLabel;

    DcReadiness(AppSource app, Aws aws, File vuJar) {
        this.app = app;
        this.aws = aws;
        this.vuJar = vuJar;
        appLabel = app.getLabel();
    }

    void test() {
        VirtualUserLoad load = new VirtualUserLoad.Builder()
            .virtualUsers(10)
            .flat(duration)
            .build();
        TestWorkspace workspace = new RootWorkspace(outputDirectory)
            .getCurrentTask()
            .isolateTest("dc-readiness-test");
        List<ProvisioningPerformanceTest> tests = Arrays.asList(
            dcTest(
                "on-one-node-without-app",
                new NoApp(),
                1
            ),
            dcTest(
                "on-one-node",
                app,
                1
            ),
            dcTest(
                "on-two-nodes",
                app,
                2
            ),
            dcTest(
                "on-four-nodes",
                app,
                4
            )
        );
        TargetingVirtualUserOptions vuOptions = jiraUri -> new VirtualUserOptions(
            new VirtualUserTarget(
                jiraUri,
                "admin",
                "admin"
            ),
            new VirtualUserBehavior.Builder(scenario)
                .load(load)
                .build()
        );
        ExecutorService threadPool = Executors.newFixedThreadPool(
            tests.size(),
            runnable -> new Thread(runnable, "dc-readiness-test-thread")
        );
        List<CompletableFuture<RawCohortResult>> futureResults = tests
            .stream()
            .map(it -> it.executeAsync(workspace, threadPool, vuOptions))
            .collect(toList());
        List<EdibleResult> results = futureResults
            .stream()
            .map(it -> {
                try {
                    return it.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .map(it -> it.prepareForJudgement(new StandardTimeline(load.getTotal())))
            .collect(toList());
        threadPool.shutdownNow();
        results.forEach(it ->
            new FailureJudge().judge(it.getFailure())
        );
        List<String> labels = results
            .stream()
            .flatMap(it -> it.getActionLabels().stream())
            .distinct()
            .sorted()
            .collect(toList());
        new FullReport().dump(
            results,
            workspace,
            labels
        );
    }

    private ProvisioningPerformanceTest dcTest(
        String cohort,
        AppSource app,
        int nodeCount
    ) {
        return new ProvisioningPerformanceTest(
            new InfrastructureFormula<>(
                new Investment(
                    "Measure app impact of " + appLabel + " across a Data Center cluster",
                    Duration.ofHours(1),
                    true,
                    () -> "jpt-" + UUID.randomUUID().toString()
                ),
                new DataCenterFormula.Builder(
                    product,
                    dataset.getJiraHomeSource(),
                    dataset.getDatabase()
                )
                    .configs(
                        IntStream.rangeClosed(1, nodeCount)
                            .mapToObj(it ->
                                new JiraNodeConfig.Builder()
                                    .name("dc-" + it)
                                    .build()
                            )
                            .collect(toList())
                    )
                    .apps(new Apps(singletonList(app)))
                    .loadBalancerFormula(new ElasticLoadBalancerFormula())
                    .build(),
                new Ec2VirtualUsersFormula.Builder(vuJar).build(),
                aws
            ),
            cohort
        );
    }
}
