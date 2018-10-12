package com.atlassian.performance.tools.jiraperformancetests.api

import com.atlassian.performance.tools.aws.api.Aws
import com.atlassian.performance.tools.aws.api.Investment
import com.atlassian.performance.tools.awsinfrastructure.api.DatasetCatalogue
import com.atlassian.performance.tools.awsinfrastructure.api.InfrastructureFormula
import com.atlassian.performance.tools.awsinfrastructure.api.VirtualUsersConfiguration
import com.atlassian.performance.tools.awsinfrastructure.api.jira.DataCenterFormula
import com.atlassian.performance.tools.awsinfrastructure.api.storage.JiraSoftwareStorage
import com.atlassian.performance.tools.awsinfrastructure.api.virtualusers.Ec2VirtualUsersFormula
import com.atlassian.performance.tools.infrastructure.api.app.AppSource
import com.atlassian.performance.tools.infrastructure.api.app.Apps
import com.atlassian.performance.tools.infrastructure.api.app.MavenApp
import com.atlassian.performance.tools.infrastructure.api.app.NoApp
import com.atlassian.performance.tools.infrastructure.api.dataset.Dataset
import com.atlassian.performance.tools.infrastructure.api.jira.JiraNodeConfig
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario
import com.atlassian.performance.tools.jirasoftwareactions.api.JiraSoftwareScenario
import com.atlassian.performance.tools.report.api.FullReport
import com.atlassian.performance.tools.report.api.StandardTimeline
import com.atlassian.performance.tools.report.api.judge.FailureJudge
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.workspace.api.RootWorkspace
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.Executors

/**
 * Tests the Data Center Readiness Program of the [app].
 *
 * Summary of test results (i.e. mean latency) can be found in 'summary-per-cohort.csv' file.
 * You should have it in './target/jpt-workspace/<timestamp>/dc-readiness-tests/' directory.
 *
 * For the "Endpoint testing" you should submit 'on-one-node-without-app' vs 'on-one-node' cohorts,
 * while for "Scale testing" you should submit 'one-one-node', 'on-two-nodes', 'on-four-nodes' results.
 * Ideally converted to a nice chart. For details visit
 * https://developer.atlassian.com/platform/marketplace/dc-apps-performance-and-scale-testing/
 */
class DcReadinessTests(
    private val app: AppSource,
    private val aws: Aws,
    var testJar: File
) {
    constructor(
        app: MavenApp,
        aws: Aws
    ) : this(
        app = app,
        aws = aws,
        testJar = File("target/${app.artifactId}-performance-tests-${app.version}-fat-tests.jar")
    )

    var scenario: Class<out Scenario> = JiraSoftwareScenario::class.java
    var jiraVersion: String = "7.5.0"
    var duration: Duration = Duration.ofMinutes(20)
    internal var dataset: Dataset = DatasetCatalogue().largeJira()
    private val outputDirectory: Path = Paths.get("target")
    private val appLabel = app.getLabel()

    fun run() {
        val virtualUserLoad = VirtualUserLoad(
            virtualUsers = 10,
            hold = Duration.ZERO,
            ramp = Duration.ZERO,
            flat = duration
        )
        val workspace = RootWorkspace(outputDirectory).currentTask.isolateTest("dc-readiness-tests")
        val tests = listOf(
            dcTestingCohort(
                cohort = "on-one-node-without-app",
                app = NoApp(),
                numberOfNodes = 1
            ),
            dcTestingCohort(
                cohort = "on-one-node",
                app = app,
                numberOfNodes = 1
            ),
            dcTestingCohort(
                cohort = "on-two-nodes",
                app = app,
                numberOfNodes = 2
            ),
            dcTestingCohort(
                cohort = "on-four-nodes",
                app = app,
                numberOfNodes = 4
            )
        )
        val vuConfig = VirtualUsersConfiguration(
            scenario = scenario,
            virtualUserLoad = virtualUserLoad
        )

        val executor = Executors.newFixedThreadPool(
            tests.size,
            ThreadFactoryBuilder()
                .setNameFormat("dc-readiness-test-thread-%d")
                .build()
        )
        val results = tests
            .map { it.runAsync(workspace, executor, vuConfig) }
            .map { it.get() }
            .map { it.prepareForJudgement(StandardTimeline(virtualUserLoad.total)) }
        executor.shutdownNow()

        results.forEach {
            FailureJudge().judge(it.failure)
        }
        FullReport().dump(
            results = results,
            workspace = TestWorkspace(workspace.directory)
        )
    }

    private fun dcTestingCohort(
        cohort: String,
        app: AppSource,
        numberOfNodes: Int
    ): ProvisioningPerformanceTest = ProvisioningPerformanceTest(
        cohort = cohort,
        infrastructureFormula = InfrastructureFormula(
            investment = Investment(
                useCase = "Measure app impact of $appLabel across a Data Center cluster",
                lifespan = Duration.ofHours(1)
            ),
            jiraFormula = DataCenterFormula(
                configs = JiraNodeConfig().clone(numberOfNodes),
                apps = Apps(listOf(app)),
                application = JiraSoftwareStorage(jiraVersion),
                jiraHomeSource = dataset.jiraHomeSource,
                database = dataset.database
            ),
            virtualUsersFormula = Ec2VirtualUsersFormula(
                shadowJar = testJar
            ),
            aws = aws
        )
    )
}
