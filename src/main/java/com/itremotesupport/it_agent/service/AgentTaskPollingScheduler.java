
package com.itremotesupport.it_agent.service;

import com.itremotesupport.it_agent.dto.RemoteTaskResponse;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentTaskPollingScheduler {

    private final AgentTaskPollingService taskService;
    private final AgentTaskExecutionService executionService;
    private final AgentTaskResultService resultService;

    private ScheduledExecutorService scheduler;

    public AgentTaskPollingScheduler(
            AgentTaskPollingService taskService,
            AgentTaskExecutionService executionService,
            AgentTaskResultService resultService) {

        this.taskService = taskService;
        this.executionService = executionService;
        this.resultService = resultService;
    }

    public void start(int interval) {

        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleWithFixedDelay(
                this::checkTasks,
                0,
                interval,
                TimeUnit.SECONDS
        );
    }

    private void checkTasks() {
        taskService.fetchNextTask().ifPresent(this::executeTask);
    }

    private void executeTask(RemoteTaskResponse task) {

        System.out.println("================================");
        System.out.println("       EXECUTION DE TACHE");
        System.out.println("================================");

        System.out.println("Task ID : " + task.id());
        System.out.println("Agent : " + task.agentId());
        System.out.println("Type : " + task.taskType());

        String result = executionService.execute(task);

        System.out.println("--------------------------------");
        System.out.println("RESULTAT :");
        System.out.println(result);

        // Transmettre le résultat au serveur
        resultService.sendResult(task, result);

        System.out.println("================================");
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
