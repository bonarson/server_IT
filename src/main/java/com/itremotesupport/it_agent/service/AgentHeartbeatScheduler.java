package com.itremotesupport.it_agent.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentHeartbeatScheduler {

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();

    private final AgentHeartbeatService heartbeatService;
    private final int interval;

    public AgentHeartbeatScheduler(
            AgentHeartbeatService heartbeatService,
            int interval
    ) {
        this.heartbeatService = heartbeatService;
        this.interval = interval;
    }

    public void start() {
        executor.scheduleAtFixedRate(
                heartbeatService::sendHeartbeat,
                0,
                interval,
                TimeUnit.SECONDS
        );

        System.out.println(
                "Heartbeat activé : " + interval + " secondes"
        );
    }

    public void stop() {
        executor.shutdownNow();
    }
}