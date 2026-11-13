package com.itremotesupport.it_server.repository;

import com.itremotesupport.it_server.entity.RemoteTask;
import com.itremotesupport.it_server.enums.TaskStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RemoteTaskRepository
        extends JpaRepository<RemoteTask, Long>,
        JpaSpecificationExecutor<RemoteTask> {

    List<RemoteTask> findByAgent_AgentIdAndStatusOrderByCreatedAtAsc(
            String agentId, TaskStatus status
    );

    List<RemoteTask> findByStatusAndExpiresAtBefore(
            TaskStatus status, LocalDateTime date
    );

    List<RemoteTask> findByAgent_AgentIdOrderByCreatedAtDesc(
            String agentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT t FROM RemoteTask t
        WHERE t.agent.agentId = :agentId
        AND t.status = :status
        AND t.expiresAt > :now
        ORDER BY t.createdAt ASC
    """)
    List<RemoteTask> findPendingTasksForUpdate(
            @Param("agentId") String agentId,
            @Param("status") TaskStatus status,
            @Param("now") LocalDateTime now
    );

    List<RemoteTask> findByStatusAndStartedAtBefore(
            TaskStatus status, LocalDateTime date
    );
}