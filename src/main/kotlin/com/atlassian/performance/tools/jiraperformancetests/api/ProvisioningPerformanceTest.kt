package com.atlassian.performance.tools.jiraperformancetests.api

import com.atlassian.performance.tools.awsinfrastructure.api.InfrastructureFormula
import com.atlassian.performance.tools.concurrency.api.submitWithLogContext
import com.atlassian.performance.tools.io.api.ensureDirectory
import com.atlassian.performance.tools.jiraactions.api.parser.MergingActionMetricsParser
import com.atlassian.performance.tools.report.api.parser.MergingNodeCountParser
import com.atlassian.performance.tools.report.api.parser.SystemMetricsParser
import com.atlassian.performance.tools.report.api.result.CohortResult
import com.atlassian.performance.tools.report.api.result.FailedCohortResult
import com.atlassian.performance.tools.report.api.result.FullCohortResult
import com.atlassian.performance.tools.report.api.result.RawCohortResult
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.apache.logging.log4j.CloseableThreadContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

class ProvisioningPerformanceTest(
    private val infrastructureFormula: InfrastructureFormula<*>,
    private val cohort: String
) {
    private val logger: Logger = LogManager.getLogger(this::class.java)

    @Deprecated(
        message = "Use executeAsync method instead.",
        replaceWith = ReplaceWith(
            expression = "executeAsync(workingDirectory, executor, behavior)"
        )
    )
    fun runAsync(
        workingDirectory: TestWorkspace,
        executor: ExecutorService,
        behavior: VirtualUserBehavior
    ): CompletableFuture<CohortResult> = executor.submitWithLogContext(cohort) {
        CloseableThreadContext.put("cohort", cohort).use {
            run(workingDirectory, behavior)
        }
    }

    @Deprecated(
        message = "Use execute method instead.",
        replaceWith = ReplaceWith(
            expression = "execute(workingDirectory, behavior)"
        )
    )
    fun run(
        workingDirectory: TestWorkspace,
        behavior: VirtualUserBehavior
    ): CohortResult {
        val result = execute(workingDirectory, behavior)
        val failure = result.failure
        return if (failure == null) {
            return FullCohortResult(
                cohort = cohort,
                results = result.results,
                actionParser = MergingActionMetricsParser(),
                systemParser = SystemMetricsParser(),
                nodeParser = MergingNodeCountParser()
            )
        } else {
            FailedCohortResult(cohort, failure)
        }
    }

    fun executeAsync(
        workingDirectory: TestWorkspace,
        executor: ExecutorService,
        behavior: VirtualUserBehavior
    ): CompletableFuture<RawCohortResult> = executor.submitWithLogContext(cohort) {
        CloseableThreadContext.put("cohort", cohort).use {
            execute(workingDirectory, behavior)
        }
    }

    fun execute(
        workingDirectory: TestWorkspace,
        behavior: VirtualUserBehavior
    ): RawCohortResult {
        val workspace = workingDirectory.directory.resolve(cohort).ensureDirectory()
        try {
            val provisionedInfrastructure = infrastructureFormula.provision(workspace)
            val infrastructure = provisionedInfrastructure.infrastructure
            val resource = provisionedInfrastructure.resource
            val downloadedResults: Path
            try {
                infrastructure.applyLoad(behavior)
            } catch (e: Exception) {
                logger.error("Failed to test on $infrastructure", e)
                throw e
            } finally {
                if (resource.isExpired()) {
                    logger.warn("$resource is already expired, but the test just finished")
                }
                downloadedResults = infrastructure.downloadResults(workspace)
            }
            logger.info("Freeing AWS resources...")
            resource.release().get(2, TimeUnit.MINUTES)
            logger.info("AWS resources are freed")
            return RawCohortResult.Factory().fullResult(
                cohort = cohort,
                results = downloadedResults
            )
        } catch (e: Exception) {
            return RawCohortResult.Factory().failedResult(cohort, workingDirectory.directory, e)
        }
    }
}