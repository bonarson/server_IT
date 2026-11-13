package com.itremotesupport.it_server.specification;

import com.itremotesupport.it_server.entity.RemoteTask;
import com.itremotesupport.it_server.enums.TaskStatus;
import com.itremotesupport.it_server.enums.TaskType;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class RemoteTaskSpecification {

    public static Specification<RemoteTask> hasAgentId(String agentId) {
        return (root, query, cb) ->
                agentId == null || agentId.isBlank()
                        ? cb.conjunction()
                        : cb.equal(root.get("agent").get("agentId"), agentId);
    }

    public static Specification<RemoteTask> hasStatus(TaskStatus status) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<RemoteTask> hasTaskType(TaskType taskType) {
        return (root, query, cb) ->
                taskType == null
                        ? cb.conjunction()
                        : cb.equal(root.get("taskType"), taskType);
    }

    public static Specification<RemoteTask> hasTaskId(Long id) {
        return (root, query, cb) ->
                id == null
                        ? cb.conjunction()
                        : cb.equal(root.get("id"), id);
    }

    public static Specification<RemoteTask> createdAfter(
            LocalDateTime startDate) {
        return (root, query, cb) ->
                startDate == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("createdAt"), startDate);
    }

    public static Specification<RemoteTask> createdBefore(
            LocalDateTime endDate) {
        return (root, query, cb) ->
                endDate == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                        root.get("createdAt"), endDate);
    }
}