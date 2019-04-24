package com.atlassian.performance.tools.jiraperformancetests.api

@Deprecated("Give us feedback about your use case.")
class RegressionResults(
    @Suppress("DEPRECATION") val baseline: com.atlassian.performance.tools.report.api.result.CohortResult,
    @Suppress("DEPRECATION") val experiment: com.atlassian.performance.tools.report.api.result.CohortResult
)