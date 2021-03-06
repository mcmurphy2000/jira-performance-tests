package com.atlassian.performance.tools.referencejiraapp.scenario;

import com.atlassian.performance.tools.jiraactions.api.SeededRandom;
import com.atlassian.performance.tools.jiraactions.api.WebJira;
import com.atlassian.performance.tools.jiraactions.api.action.Action;
import com.atlassian.performance.tools.jiraactions.api.action.BrowseProjectsAction;
import com.atlassian.performance.tools.jiraactions.api.action.CreateIssueAction;
import com.atlassian.performance.tools.jiraactions.api.action.SearchJqlAction;
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter;
import com.atlassian.performance.tools.jiraactions.api.memories.IssueKeyMemory;
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory;
import com.atlassian.performance.tools.jiraactions.api.memories.adaptive.AdaptiveIssueKeyMemory;
import com.atlassian.performance.tools.jiraactions.api.memories.adaptive.AdaptiveJqlMemory;
import com.atlassian.performance.tools.jiraactions.api.memories.adaptive.AdaptiveProjectMemory;
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Example of custom scenario. Provides custom actions and explains how to override default login action.
 */
public class MyScenario implements Scenario {

    @Override
    public Action getLogInAction(WebJira jira, ActionMeter meter, UserMemory userMemory) {
        return new MyLoginAction(jira, meter, userMemory);
    }

    @NotNull
    @Override
    public List<Action> getActions(WebJira webJira, SeededRandom seededRandom, ActionMeter actionMeter) {

        final AdaptiveJqlMemory jqlMemory = new AdaptiveJqlMemory(seededRandom);
        final IssueKeyMemory issueKeyMemory = new AdaptiveIssueKeyMemory(seededRandom);
        final AdaptiveProjectMemory adaptiveProjectMemory = new AdaptiveProjectMemory(seededRandom);
        return ImmutableList.of(
            new SearchJqlAction(webJira, actionMeter, jqlMemory, issueKeyMemory),
            new BrowseProjectsAction(webJira, actionMeter, adaptiveProjectMemory),
            new CreateIssueAction(webJira, actionMeter, adaptiveProjectMemory, seededRandom),
            new CreateIssueAction(webJira, actionMeter, adaptiveProjectMemory, seededRandom),
            new CreateIssueAction(webJira, actionMeter, adaptiveProjectMemory, seededRandom),
            new CreateIssueAction(webJira, actionMeter, adaptiveProjectMemory, seededRandom),
            new CreateIssueAction(webJira, actionMeter, adaptiveProjectMemory, seededRandom),
            new CreateIssueAction(webJira, actionMeter, adaptiveProjectMemory, seededRandom),
            new CreateIssueAction(webJira, actionMeter, adaptiveProjectMemory, seededRandom),
            new CreateIssueAction(webJira, actionMeter, adaptiveProjectMemory, seededRandom),
            new CustomViewIssueAction(webJira, actionMeter, issueKeyMemory)
        );
    }
}
