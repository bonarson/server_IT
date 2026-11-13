package com.itremotesupport.it_server.dto;

import jakarta.validation.constraints.NotBlank;

public record HeartbeatRequest(

        @NotBlank String agentId,
        @NotBlank String hostname,
        @NotBlank String ipAddress
) {
}
