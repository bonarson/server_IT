package com.itremotesupport.it_server.service;

import com.itremotesupport.it_server.dto.AgentRegistrationRequest;
import com.itremotesupport.it_server.dto.AgentResponse;
import com.itremotesupport.it_server.dto.HeartbeatRequest;
import com.itremotesupport.it_server.entity.Agent;
import com.itremotesupport.it_server.enums.AgentStatus;
import com.itremotesupport.it_server.repository.AgentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgentService {

    private final AgentRepository agentRepository;

    public AgentService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Transactional
    public AgentResponse register(AgentRegistrationRequest request) {

        Agent agent = agentRepository
                .findByAgentId(request.agentId())
                .orElseGet(Agent::new);

        agent.setAgentId(request.agentId());
        agent.setHostname(request.hostname());
        agent.setIpAddress(request.ipAddress());
        agent.setStatus(AgentStatus.ONLINE);
        agent.setLastSeen(LocalDateTime.now());

        return toResponse(agentRepository.save(agent));
    }

    public List<AgentResponse> findAll() {
        return agentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AgentResponse toResponse(Agent agent) {
        return new AgentResponse(
                agent.getAgentId(),
                agent.getHostname(),
                agent.getIpAddress(),
                agent.getStatus(),
                agent.getLastSeen()
        );
    }

    @Transactional
    public AgentResponse heartbeat(HeartbeatRequest request) {
        Agent agent = agentRepository.findByAgentId(request.agentId())
                .orElseThrow(() -> new RuntimeException(
                        "Agent non enregistre : " + request.agentId()
                ));
        agent.setHostname(request.hostname());
        agent.setIpAddress(request.ipAddress());
        agent.setStatus(AgentStatus.ONLINE);
        agent.setLastSeen(LocalDateTime.now());

        return toResponse(agentRepository.save(agent));
    }
}