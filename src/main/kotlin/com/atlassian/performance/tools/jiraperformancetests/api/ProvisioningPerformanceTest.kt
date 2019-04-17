package com.atlassian.performance.tools.jiraperformancetests.api

import com.atlassian.performance.tools.awsinfrastructure.api.InfrastructureFormula
import com.atlassian.performance.tools.awsinfrastructure.api.TargetingVirtualUserOptions
import com.atlassian.performance.tools.concurrency.api.submitWithLogContext
import com.atlassian.performance.tools.io.api.ensureDirectory
import com.atlassian.performance.tools.jiraactions.api.parser.MergingActionMetricsParser
import com.atlassian.performance.tools.report.api.parser.MergingNodeCountParser
import com.atlassian.performance.tools.report.api.parser.SystemMetricsParser
import com.atlassian.performance.tools.report.api.result.RawCohortResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserBehavior
import com.atlassian.performance.tools.virtualusers.api.config.VirtualUserTarget
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.apache.logging.log4j.CloseableThreadContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.net.URI
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
    @Suppress("DEPRECATION")
    fun runAsync(
        workingDirectory: TestWorkspace,
        executor: ExecutorService,
        behavior: VirtualUserBehavior
    ): CompletableFuture<com.atlassian.performance.tools.report.api.result.CohortResult> = executor.submitWithLogContext(cohort) {
        CloseableThreadContext.put("cohort", cohort).use {
            @Suppress("DEPRECATION")
            run(workingDirectory, behavior)
        }
    }

    @Deprecated(
        message = "Use execute method instead.",
        replaceWith = ReplaceWith(
            expression = "execute(workingDirectory, behavior)"
        )
    )
    @Suppress("DEPRECATION")
    fun run(
        workingDirectory: TestWorkspace,
        behavior: VirtualUserBehavior
    ): com.atlassian.performance.tools.report.api.result.CohortResult {
        @Suppress("DEPRECATION")
        val result = execute(workingDirectory, behavior)
        val failure = result.failure
        return if (failure == null) {
            return com.atlassian.performance.tools.report.api.result.FullCohortResult(
                cohort = cohort,
                results = result.results,
                actionParser = MergingActionMetricsParser(),
                systemParser = SystemMetricsParser(),
                nodeParser = MergingNodeCountParser()
            )
        } else {
            @Suppress("DEPRECATION")
            com.atlassian.performance.tools.report.api.result.FailedCohortResult(cohort, failure)
        }
    }

    @Deprecated(
        "Use a non-deprecated `executeAsync` method, because this one hardcodes Jira admin credentials",
        ReplaceWith(
            "executeAsync(workingDirectory, executor, LegacyTargetingVirtualUserOptions(behavior))",
            "com.atlassian.performance.tools.jiraperformancetests.api.LegacyTargetingVirtualUserOptions"
        )
    )
    fun executeAsync(
        workingDirectory: TestWorkspace,
        executor: ExecutorService,
        behavior: VirtualUserBehavior
    ): CompletableFuture<RawCohortResult> = executeAsync(
        workingDirectory,
        executor,
        LegacyTargetingVirtualUserOptions(behavior)
    )

    @Deprecated(
        "Use a non-deprecated `execute` method, because this one hardcodes Jira admin credentials",
        ReplaceWith(
            "execute(workingDirectory, LegacyTargetingVirtualUserOptions(behavior))",
            "com.atlassian.performance.tools.jiraperformancetests.api.LegacyTargetingVirtualUserOptions"
        )
    )
    fun execute(
        workingDirectory: TestWorkspace,
        behavior: VirtualUserBehavior
    ): RawCohortResult = execute(
        workingDirectory,
        LegacyTargetingVirtualUserOptions(behavior)
    )

    /**
     * @since 3.3.0
     */
    fun executeAsync(
        workingDirectory: TestWorkspace,
        executor: ExecutorService,
        options: TargetingVirtualUserOptions
    ): CompletableFuture<RawCohortResult> = executor.submitWithLogContext(cohort) {
        CloseableThreadContext.put("cohort", cohort).use {
            execute(workingDirectory, options)
        }
    }

    /**
     * @since 3.3.0
     */
    fun execute(
        workingDirectory: TestWorkspace,
        options: TargetingVirtualUserOptions
    ): RawCohortResult {
        val workspace = workingDirectory.directory.resolve(cohort).ensureDirectory()
        try {
            val provisionedInfrastructure = infrastructureFormula.provision(workspace)
            val infrastructure = provisionedInfrastructure.infrastructure
            val resource = provisionedInfrastructure.resource
            val downloadedResults: Path
            try {
                infrastructure.applyLoad(options)
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

private class LegacyTargetingVirtualUserOptions(
    private val behavior: VirtualUserBehavior
) : TargetingVirtualUserOptions {

    override fun target(
        jira: URI
    ): VirtualUserOptions = VirtualUserOptions(
        target = VirtualUserTarget(
            webApplication = jira,
            userName = "admin",
            password = "admin"
        ),
        behavior = behavior
    )
}
