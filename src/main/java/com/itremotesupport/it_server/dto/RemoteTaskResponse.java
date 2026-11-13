package com.itremotesupport.it_server.dto;


import com.itremotesupport.it_server.entity.RemoteTask;
import com.itremotesupport.it_server.enums.TaskStatus;
import com.itremotesupport.it_server.enums.TaskType;

import java.time.LocalDateTime;

public record RemoteTaskResponse(
        Long id,
        String agentId,
        TaskType taskType,
        TaskStatus status,
        String parametersJson,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
    public static RemoteTaskResponse from(RemoteTask task) {
        return new RemoteTaskResponse(
                task.getId(),
                task.getAgent().getAgentId(),
                task.getTaskType(),
                task.getStatus(),
                task.getParametersJson(),
                task.getCreatedAt(),
                task.getExpiresAt()
        );
    }
}