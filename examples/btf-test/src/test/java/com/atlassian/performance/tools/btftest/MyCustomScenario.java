package com.atlassian.performance.tools.btftest;

import com.atlassian.performance.tools.jiraactions.api.SeededRandom;
import com.atlassian.performance.tools.jiraactions.api.WebJira;
import com.atlassian.performance.tools.jiraactions.api.action.Action;
import com.atlassian.performance.tools.jiraactions.api.measure.ActionMeter;
import com.atlassian.performance.tools.jiraactions.api.scenario.Scenario;

import java.util.ArrayList;
import java.util.List;

public class MyCustomScenario implements Scenario {

    private SharedConfigProjectMemory memory = new SharedConfigProjectMemory("admin", "admin");

    @Override
    public List<Action> getActions(WebJira webJira, SeededRandom seededRandom, ActionMeter actionMeter) {
//        Scenario scenario = new JiraSoftwareScenario();
//        ActionMeter meter = actionMeter.withW3cPerformanceTimeline(
//            new JavascriptW3cPerformanceTimeline((JavascriptExecutor) webJira.getDriver())
//        );
//        return scenario.getActions(webJira, seededRandom, meter);
        List<Action> list = new ArrayList<>();
        list.add(new CreateSharedProjectAction(webJira, actionMeter, memory));
        return list;
    }

    @Override
    public Action getSetupAction(WebJira jira, ActionMeter meter) {
        return new SetupAction(jira, meter, memory);
    }
}
