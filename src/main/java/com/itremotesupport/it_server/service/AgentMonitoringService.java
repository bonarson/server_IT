package com.itremotesupport.it_server.service;

import com.itremotesupport.it_server.entity.Agent;
import com.itremotesupport.it_server.enums.AgentStatus;
import com.itremotesupport.it_server.repository.AgentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgentMonitoringService {

    private final AgentRepository agentRepository;

    public AgentMonitoringService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Scheduled(fixedRate = 10000, initialDelay = 15000)
    @Transactional
    public void checkOfflineAgents() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusSeconds(30);

        System.out.println("========== AGENT MONITORING ==========");

        List<Agent> onlineAgents =
                agentRepository.findByStatus(AgentStatus.ONLINE);

        for (Agent agent : onlineAgents) {

            LocalDateTime lastSeen = agent.getLastSeen();

            if (lastSeen == null) {
                System.out.println(
                        "Agent ignoré : aucun heartbeat enregistré : "
                                + agent.getAgentId()
                );
                continue;
            }

            if (lastSeen.isBefore(threshold)) {

                agent.setStatus(AgentStatus.OFFLINE);

                System.out.println(
                        "Agent OFFLINE : " + agent.getAgentId()
                );

                System.out.println("Dernier heartbeat : " + lastSeen);
            }
        }

        agentRepository.flush();

        System.out.println("Vérification terminée à : " + now);
        System.out.println("======================================");
    }
}