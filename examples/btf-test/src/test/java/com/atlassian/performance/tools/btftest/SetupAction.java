package com.atlassian.performance.tools.btftest;

import com.atlassian.jira.rest.v2.issue.IssueTypeSchemeBean;
import com.atlassian.jira.webtests.ztests.bundledplugins2.rest.client.IssueTypeSchemeClient;
import com.atlassian.performance.tools.jiraactions.api.WebJira;
import com.atlassian.performance.tools.jiraactions.api.action.Action;
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter;

import java.util.Optional;

import static com.atlassian.performance.tools.jiraactions.api.ActionTypes.SET_UP;

public class SetupAction implements Action {
    private final WebJira jira;
    private final ActionMeter meter;
    private final SharedConfigProjectMemory memory;

    public SetupAction(WebJira jira, ActionMeter meter, SharedConfigProjectMemory memory) {
        this.jira = jira;
        this.meter = meter;
        this.memory = memory;
    }

    @Override
    public void run() {
        meter.measure(SET_UP, () -> {
            jira.configureRichTextEditor().disable();
            jira.configureBackupPolicy().delete();
            final IssueTypeSchemeClient client = new SharedProjectResourceClient(jira, memory).createIssueTypeSchemeClient();
            final Optional<Long> nonDefaultSchemeId = getIssueTypeSchemeId(client);
            nonDefaultSchemeId.ifPresent(schemeId -> memory.setProjectId(getProjectId(client, schemeId)));
            return null;
        });
    }

    private Optional<Long> getIssueTypeSchemeId(final IssueTypeSchemeClient client) {
        final String defaultSchemeName = "Default Issue Type Scheme";
        return client.getAllIssueTypeSchemes().body.getSchemes().stream()
            .filter(its -> !defaultSchemeName.equals(its.getName()))
            .map(IssueTypeSchemeBean::getId)
            .map(Long::parseLong)
            .findFirst();
    }

    private Long getProjectId(final IssueTypeSchemeClient client, final long issueTypeSchemeId) {
        return client.getProjectsAssociatedWithIssueTypeScheme(issueTypeSchemeId).body.stream()
            .map(ps -> ps.id)
            .map(Long::valueOf)
            .findFirst()
            .orElse(null);
    }
}
