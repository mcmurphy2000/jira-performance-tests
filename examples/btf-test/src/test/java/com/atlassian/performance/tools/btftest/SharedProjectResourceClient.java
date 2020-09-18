package com.atlassian.performance.tools.btftest;

import com.atlassian.jira.webtests.util.JIRAEnvironmentData;
import com.atlassian.jira.webtests.util.LocalTestEnvironmentData;
import com.atlassian.jira.webtests.ztests.bundledplugins2.rest.client.CreateSharedProjectResourceClient;
import com.atlassian.jira.webtests.ztests.bundledplugins2.rest.client.IssueTypeSchemeClient;
import com.atlassian.performance.tools.jiraactions.api.WebJira;

import java.util.Properties;

public class SharedProjectResourceClient {

    private final WebJira jira;
    private final SharedConfigProjectMemory memory;

    SharedProjectResourceClient(final WebJira jira, final SharedConfigProjectMemory memory) {
        this.jira = jira;
        this.memory = memory;
    }

    CreateSharedProjectResourceClient createSharedProjectResourceClient() {
        final CreateSharedProjectResourceClient client = new CreateSharedProjectResourceClient(envData());
        return client.loginAs(memory.getUserName(), memory.getPassword());
    }

    IssueTypeSchemeClient createIssueTypeSchemeClient() {
        final IssueTypeSchemeClient client = new IssueTypeSchemeClient(envData());
        return client.loginAs(memory.getUserName(), memory.getPassword());
    }

    private JIRAEnvironmentData envData() {
        final Properties props = new Properties();
        props.put("jira.protocol", jira.getBase().getScheme());
        props.put("jira.host", jira.getBase().getHost());
        props.put("jira.port", jira.getBase().getPort());
        props.put("jira.context", jira.getBase().getPath());
        return new LocalTestEnvironmentData(props, null);
    }
}
