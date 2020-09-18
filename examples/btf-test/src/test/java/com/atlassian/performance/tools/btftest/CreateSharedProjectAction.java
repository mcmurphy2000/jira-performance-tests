package com.atlassian.performance.tools.btftest;

import com.atlassian.jira.webtests.ztests.bundledplugins2.rest.client.CreateSharedProjectResourceClient;
import com.atlassian.performance.tools.jiraactions.api.WebJira;
import com.atlassian.performance.tools.jiraactions.api.action.Action;
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.atlassian.performance.tools.jiraactions.api.ActionTypes.PROJECT_SUMMARY;

public class CreateSharedProjectAction implements Action {
    private static final Logger log = LoggerFactory.getLogger(CreateSharedProjectAction.class);

    private final ActionMeter meter;
    private final SharedConfigProjectMemory memory;
    private final CreateSharedProjectResourceClient client;

    public CreateSharedProjectAction(WebJira jira, ActionMeter meter, SharedConfigProjectMemory memory) {
        this.meter = meter;
        this.memory = memory;
        this.client = new SharedProjectResourceClient(jira, memory).createSharedProjectResourceClient();
    }

    @Override
    public void run() {
        final Optional<Long> projectId = memory.getProjectId();
        if (!projectId.isPresent()) {
            log.debug("Skipping creating project from shared config action. I have no knowledge of the base project to create from.");
            return;
        }
        meter.measure(PROJECT_SUMMARY, () -> {
            final int count = memory.getAndIncrementCreateCount();
            client.createSharedProject(projectId.get(), "PYKEY" + count, "PYName" + count, memory.getUserName());
            return null;
        });
    }
}
