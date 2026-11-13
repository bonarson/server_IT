package com.itremotesupport.it_agent.task;
import com.itremotesupport.it_agent.service.ServerConnectionService;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentScheduler {

    private final ServerConnectionService connectionService;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public AgentScheduler(
            ServerConnectionService connectionService) {
        this.connectionService = connectionService;
    }



    public void start(int interval) {

        scheduler.scheduleAtFixedRate(() -> {

            System.out.println(
                    "\n[" + LocalDateTime.now() + "]"
            );

            boolean connected =
                    connectionService.checkConnection();

            if (connected) {
                System.out.println(
                        "Serveur IT : CONNECTE"
                );
            } else {
                System.out.println(
                        "Serveur IT : DECONNECTE"
                );
            }

        }, 0, interval, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
