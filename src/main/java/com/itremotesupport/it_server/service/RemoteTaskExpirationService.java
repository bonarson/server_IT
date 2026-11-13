package com.itremotesupport.it_server.service;

import com.itremotesupport.it_server.entity.RemoteTask;
import com.itremotesupport.it_server.enums.TaskStatus;
import com.itremotesupport.it_server.repository.RemoteTaskRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RemoteTaskExpirationService {

    private final RemoteTaskRepository remoteTaskRepository;

    public RemoteTaskExpirationService(
            RemoteTaskRepository remoteTaskRepository) {
        this.remoteTaskRepository = remoteTaskRepository;
    }

    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    @Transactional
    public void expireTasks() {

        LocalDateTime now = LocalDateTime.now();

        // 1. Expiration des tâches en attente
        List<RemoteTask> pendingTasks =
                remoteTaskRepository.findByStatusAndExpiresAtBefore(
                        TaskStatus.PENDING,
                        now
                );

        for (RemoteTask task : pendingTasks) {
            task.setStatus(TaskStatus.EXPIRED);
            task.setResultMessage(
                    "Tache expiree : delai de validite depasse."
            );
            task.setCompletedAt(now);
        }

        // 2. Expiration des tâches bloquées
        LocalDateTime executionThreshold = now.minusMinutes(2);

        List<RemoteTask> runningTasks =
                remoteTaskRepository.findByStatusAndStartedAtBefore(
                        TaskStatus.IN_PROGRESS,
                        executionThreshold
                );

        for (RemoteTask task : runningTasks) {
            task.setStatus(TaskStatus.EXPIRED);
            task.setResultMessage(
                    "Tache expiree : temps maximal d'execution depasse."
            );
            task.setCompletedAt(now);
        }

        remoteTaskRepository.flush();

        if (!pendingTasks.isEmpty() || !runningTasks.isEmpty()) {
            System.out.println("========== TASK EXPIRATION ==========");
            System.out.println(
                    "Taches PENDING expirees : " + pendingTasks.size()
            );
            System.out.println(
                    "Taches IN_PROGRESS expirees : " + runningTasks.size()
            );
            System.out.println("=====================================");
        }
    }
}
