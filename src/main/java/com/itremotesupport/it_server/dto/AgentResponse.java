package com.itremotesupport.it_server.dto;

import com.itremotesupport.it_server.enums.AgentStatus;

import java.time.LocalDateTime;

public record AgentResponse(
        String agentId,
        String hostname,
        String ipAddress,
        AgentStatus status,
        LocalDateTime lastSeen
) {}