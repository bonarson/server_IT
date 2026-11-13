
package com.itremotesupport.it_server.controller;

import com.itremotesupport.it_server.dto.RemoteTaskRequest;
import com.itremotesupport.it_server.dto.RemoteTaskResponse;
import com.itremotesupport.it_server.dto.RemoteTaskListResponse;
import com.itremotesupport.it_server.dto.RemoteTaskResultRequest;
import com.itremotesupport.it_server.enums.TaskStatus;
import com.itremotesupport.it_server.enums.TaskType;
import com.itremotesupport.it_server.service.RemoteTaskService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/tasks")
public class RemoteTaskController {

    private final RemoteTaskService taskService;

    public RemoteTaskController(RemoteTaskService taskService) {
        this.taskService = taskService;
    }

    // Créer une tâche
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RemoteTaskResponse createTask(
            @Valid @RequestBody RemoteTaskRequest request) {

        return taskService.createTask(
                request.agentId(),
                request.taskType(),
                request.parametersJson()
        );
    }

    // Récupérer la prochaine tâche d'un agent
    @GetMapping("/agents/{agentId}/next")
    public ResponseEntity<RemoteTaskResponse> getNextTask(
            @PathVariable String agentId) {

        RemoteTaskResponse task = taskService.getNextTask(agentId);

        if (task == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(task);
    }

    // Envoyer le résultat d'une tâche
    @PostMapping("/agents/{agentId}/{taskId}/result")
    public ResponseEntity<Void> submitResult(
            @PathVariable String agentId,
            @PathVariable Long taskId,
            @Valid @RequestBody RemoteTaskResultRequest request) {

        taskService.completeTask(
                taskId,
                agentId,
                request.success(),
                request.resultMessage()
        );

        return ResponseEntity.ok().build();
    }

    // Consulter et filtrer toutes les tâches
    @GetMapping
    public ResponseEntity<Page<RemoteTaskListResponse>> getAllTasks(

            @RequestParam(required = false)
            Long id,

            @RequestParam(required = false)
            String agentId,

            @RequestParam(required = false)
            TaskStatus status,

            @RequestParam(required = false)
            TaskType taskType,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size
    ) {

        Page<RemoteTaskListResponse> tasks =
                taskService.getAllTasks(
                        id,
                        agentId,
                        status,
                        taskType,
                        startDate,
                        endDate,
                        page,
                        size
                );

        return ResponseEntity.ok(tasks);
    }
}
