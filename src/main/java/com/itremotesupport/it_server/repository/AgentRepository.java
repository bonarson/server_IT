package com.itremotesupport.it_server.repository;


import com.itremotesupport.it_server.entity.Agent;
import com.itremotesupport.it_server.enums.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByAgentId(String agentId);

    List<Agent> findByStatusAndLastSeenBefore(
            AgentStatus status,
            LocalDateTime lastSeen
    );

    List<Agent> findByStatus(AgentStatus status);
}