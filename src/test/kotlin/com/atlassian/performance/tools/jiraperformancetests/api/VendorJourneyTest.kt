package com.atlassian.performance.tools.jiraperformancetests.api

import com.atlassian.performance.tools.jiraperformancetests.AcceptanceCategory
import com.atlassian.performance.tools.jiraperformancetests.MavenProcess
import com.atlassian.performance.tools.jiraperformancetests.SystemProperty
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.categories.Category
import org.zeroturnaround.exec.ProcessExecutor
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.TimeUnit

class VendorJourneyTest {

    private val jptVersion: String = SystemProperty("jpt.version").dereference()

    @Test
    @Category(AcceptanceCategory::class)
    fun shouldRunRefApp() {
        testRefApp(
            listOf("install", "-Djpt.version=$jptVersion", "-Papp-impact"),
            Duration.ofMinutes(55)
        )
    }

    private fun testRefApp(
        mavenArgs: List<String>,
        timeout: Duration
    ) {
        val mavenProcess = MavenProcess(
            arguments = mavenArgs,
            processExecutor = ProcessExecutor()
                .directory(Paths.get("examples", "ref-app").toFile())
                .timeout(timeout.seconds, TimeUnit.SECONDS)
        )

        val result = mavenProcess.run()

        val lastFewLinesOfOutput = result.output.lines.takeLast(12).joinToString(separator = "\n")
        assertThat(lastFewLinesOfOutput)
            .`as`("last few lines of output")
            .contains("BUILD SUCCESS")
    }

    @Test
    @Category(AcceptanceCategory::class)
    fun shouldTestDcReadiness() {
        testRefApp(
            listOf("install", "-Djpt.version=$jptVersion", "-Pdc-readiness"),
            Duration.ofMinutes(70)
        )
    }
}
