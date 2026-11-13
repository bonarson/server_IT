package com.itremotesupport.it_server.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentRegistrationRequest(
        @NotBlank String agentId,
        @NotBlank String hostname,
        String ipAddress
) {
}