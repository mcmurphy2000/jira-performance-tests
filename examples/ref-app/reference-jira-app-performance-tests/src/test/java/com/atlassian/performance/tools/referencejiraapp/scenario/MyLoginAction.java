package com.atlassian.performance.tools.referencejiraapp.scenario;

import com.atlassian.performance.tools.jiraactions.api.ActionTypes;
import com.atlassian.performance.tools.jiraactions.api.WebJira;
import com.atlassian.performance.tools.jiraactions.api.action.Action;
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter;
import com.atlassian.performance.tools.jiraactions.api.memories.User;
import com.atlassian.performance.tools.jiraactions.api.memories.UserMemory;
import com.atlassian.performance.tools.jiraactions.api.page.DashboardPage;

/**
 * Example of custom login action.
 */
public class MyLoginAction implements Action {
    private final WebJira jira;
    private final ActionMeter meter;
    private final UserMemory userMemory;

    MyLoginAction(WebJira jira, ActionMeter meter, UserMemory userMemory) {
        this.jira = jira;
        this.meter = meter;
        this.userMemory = userMemory;
    }

    @Override
    public void run() {
        final User user = userMemory.recall();
        if (user == null) {
            throw new RuntimeException("Can't login. User not available.");
        }
        meter.measure(ActionTypes.LOG_IN, () -> {
            final DashboardPage dashboardPage = jira.goToLogin().logIn(user);
            return dashboardPage.waitForDashboard();
        });
    }
}