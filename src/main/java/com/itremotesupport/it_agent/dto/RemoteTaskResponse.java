package com.itremotesupport.it_agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteTaskResponse(
        Long id,
        String agentId,
        String taskType,
        String status,
        String parametersJson
) {
}