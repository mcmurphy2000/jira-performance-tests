package com.atlassian.performance.tools.jiraperformancetests.api

import com.atlassian.performance.tools.awsinfrastructure.api.hardware.C5NineExtraLargeEphemeral
import com.atlassian.performance.tools.awsinfrastructure.api.jira.DataCenterFormula
import com.atlassian.performance.tools.awsinfrastructure.api.jira.JiraFormula
import com.atlassian.performance.tools.awsinfrastructure.api.jira.StandaloneFormula
import com.atlassian.performance.tools.awsinfrastructure.api.storage.ApplicationStorage
import com.atlassian.performance.tools.infrastructure.api.app.Apps
import com.atlassian.performance.tools.infrastructure.api.database.Database
import com.atlassian.performance.tools.infrastructure.api.jira.JiraHomeSource
import com.atlassian.performance.tools.infrastructure.api.jira.JiraNodeConfig
import java.time.Duration

interface AwsJiraDeployment {

    fun createJiraFormula(
        apps: Apps,
        application: ApplicationStorage,
        jiraHomeSource: JiraHomeSource,
        database: Database
    ): JiraFormula
}

class StandaloneAwsDeployment : AwsJiraDeployment {
    override fun createJiraFormula(
        apps: Apps,
        application: ApplicationStorage,
        jiraHomeSource: JiraHomeSource,
        database: Database
    ): JiraFormula = StandaloneFormula.Builder(
        application = application,
        jiraHomeSource = jiraHomeSource,
        database = database
    )
        .apps(apps)
        .build()
}

class DataCenterAwsDeployment(
    private val nodes: Int = 2
) : AwsJiraDeployment {
    override fun createJiraFormula(
        apps: Apps,
        application: ApplicationStorage,
        jiraHomeSource: JiraHomeSource,
        database: Database
    ): JiraFormula = DataCenterFormula.Builder(
        application = application,
        jiraHomeSource = jiraHomeSource,
        database = database
    )
        .configs((1..nodes).map { JiraNodeConfig.Builder().name("jira-node-$it").build() })
        .apps(apps)
        .computer(C5NineExtraLargeEphemeral())
        .build()
}