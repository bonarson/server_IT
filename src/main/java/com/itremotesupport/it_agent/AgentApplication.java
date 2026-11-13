
package com.itremotesupport.it_agent;

import com.itremotesupport.it_agent.config.AgentConfig;
import com.itremotesupport.it_agent.service.*;
import com.itremotesupport.it_agent.task.AgentScheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentApplication {

    public static void main(String[] args) throws Exception {

        AgentConfig config = new AgentConfig();
        SystemInfoService systemInfo = new SystemInfoService();

        System.out.println("================================");
        System.out.println("     IT REMOTE SUPPORT AGENT");
        System.out.println("================================");

        System.out.println("Agent ID : " + config.getAgentId());
        System.out.println("PC : " + systemInfo.getComputerName());

        systemInfo.displayNetworkInfo();

        // 1. Enregistrement de l'agent
        AgentRegistrationService registrationService =
                new AgentRegistrationService(config);

        AtomicBoolean registered = new AtomicBoolean(
                registrationService.register()
        );

        // Nouvelle tentative d'enregistrement toutes les 30 secondes
        ScheduledExecutorService registrationScheduler =
                Executors.newSingleThreadScheduledExecutor();

        if (!registered.get()) {

            System.out.println(
                    "Serveur indisponible. L'agent continue son démarrage."
            );

            registrationScheduler.scheduleWithFixedDelay(
                    () -> {
                        if (!registered.get()) {

                            System.out.println(
                                    "Nouvelle tentative d'enregistrement..."
                            );

                            if (registrationService.register()) {

                                registered.set(true);

                                System.out.println(
                                        "Agent enregistré avec succès !"
                                );

                                registrationScheduler.shutdown();
                            }
                        }
                    },
                    30,
                    30,
                    TimeUnit.SECONDS
            );

        } else {

            System.out.println("Agent enregistré avec succès.");

            registrationScheduler.shutdown();
        }

        // 2. Vérification de la connexion au serveur
        ServerConnectionService connectionService =
                new ServerConnectionService(config.getServerUrl());

        AgentScheduler connectionScheduler =
                new AgentScheduler(connectionService);

        connectionScheduler.start(config.getPollingInterval());

        // 3. Envoi périodique du heartbeat
        AgentHeartbeatService heartbeatService =
                new AgentHeartbeatService(config);

        AgentHeartbeatScheduler heartbeatScheduler =
                new AgentHeartbeatScheduler(
                        heartbeatService,
                        config.getPollingInterval()
                );

        heartbeatScheduler.start();

        // 4. Récupération des tâches à distance
        AgentTaskPollingService taskPollingService =
                new AgentTaskPollingService(config);

        AgentTaskExecutionService taskExecutionService =
                new AgentTaskExecutionService();

        // Test PowerShell
        taskExecutionService.testPowerShell();

        // Transmission des résultats au serveur
        AgentTaskResultService taskResultService =
                new AgentTaskResultService(config);

        // Scheduler complet
        AgentTaskPollingScheduler taskPollingScheduler =
                new AgentTaskPollingScheduler(
                        taskPollingService,
                        taskExecutionService,
                        taskResultService
                );

        taskPollingScheduler.start(config.getPollingInterval());

        System.out.println("================================");
        System.out.println("AGENT DEMARRE AVEC SUCCES");
        System.out.println("Surveillance active.");
        System.out.println("================================");

        // 5. Arrêt propre des schedulers
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {

                    System.out.println("\nArrêt de l'agent...");

                    registrationScheduler.shutdownNow();

                    connectionScheduler.stop();
                    heartbeatScheduler.stop();
                    taskPollingScheduler.stop();

                    System.out.println("Agent arrêté.");
                })
        );
    }
}
