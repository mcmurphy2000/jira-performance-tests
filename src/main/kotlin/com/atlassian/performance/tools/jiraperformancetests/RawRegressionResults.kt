package com.atlassian.performance.tools.jiraperformancetests

import com.atlassian.performance.tools.report.api.result.RawCohortResult

internal class RawRegressionResults(
    val baseline: RawCohortResult,
    val experiment: RawCohortResult
)

