package com.itremotesupport.it_server.dto;

import com.itremotesupport.it_server.entity.RemoteTask;
import com.itremotesupport.it_server.enums.TaskStatus;
import com.itremotesupport.it_server.enums.TaskType;

import java.time.LocalDateTime;

public record RemoteTaskListResponse(

        Long id,
        String agentId,
        TaskType taskType,
        TaskStatus status,
        String resultMessage,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime expiresAt

) {

    public static RemoteTaskListResponse from(RemoteTask task) {

        return new RemoteTaskListResponse(
                task.getId(),
                task.getAgent().getAgentId(),
                task.getTaskType(),
                task.getStatus(),
                task.getResultMessage(),
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getExpiresAt()
        );
    }
}