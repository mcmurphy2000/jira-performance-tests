package com.atlassian.performance.tools.jiraperformancetests

import java.util.concurrent.ThreadFactory

internal class CountingThreadFactory(
    private val prefix: String
) : ThreadFactory {
    private var count = 0

    override fun newThread(r: Runnable): Thread {
        count++
        return Thread(r, "$prefix-$count")
    }
}
