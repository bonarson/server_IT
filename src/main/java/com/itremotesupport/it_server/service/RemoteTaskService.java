package com.itremotesupport.it_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itremotesupport.it_server.dto.RemoteTaskListResponse;
import com.itremotesupport.it_server.dto.RemoteTaskResponse;
import com.itremotesupport.it_server.entity.Agent;
import com.itremotesupport.it_server.entity.RemoteTask;
import com.itremotesupport.it_server.enums.AgentStatus;
import com.itremotesupport.it_server.enums.TaskStatus;
import com.itremotesupport.it_server.enums.TaskType;
import com.itremotesupport.it_server.repository.AgentRepository;
import com.itremotesupport.it_server.repository.RemoteTaskRepository;
import com.itremotesupport.it_server.specification.RemoteTaskSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class RemoteTaskService {

    private final RemoteTaskRepository taskRepository;
    private final AgentRepository agentRepository;
    private final ObjectMapper objectMapper;

    public RemoteTaskService(
            RemoteTaskRepository taskRepository,
            AgentRepository agentRepository,
            ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.agentRepository = agentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RemoteTaskResponse createTask(
            String agentId,
            TaskType taskType,
            String parametersJson) {

        Agent agent = agentRepository.findByAgentId(agentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Agent introuvable"));


        if (agent.getStatus() != AgentStatus.ONLINE) {
            throw new IllegalStateException(
                    "Impossible d'attribuer une tâche : agent OFFLINE");
        }


        if (taskType == null) {
            throw new IllegalArgumentException("Type de tâche obligatoire");
        }

        validateParameters(taskType, parametersJson);

        LocalDateTime now = LocalDateTime.now();

        RemoteTask task = new RemoteTask();
        task.setAgent(agent);
        task.setTaskType(taskType);
        task.setStatus(TaskStatus.PENDING);
        task.setParametersJson(parametersJson);
        task.setExpiresAt(now.plusMinutes(5));

        RemoteTask savedTask = taskRepository.save(task);

        return RemoteTaskResponse.from(savedTask);
    }

    private void validateParameters(
            TaskType taskType,
            String parametersJson) {

        try {
            JsonNode params = objectMapper.readTree(parametersJson);

            if (params == null || !params.isObject()) {
                throw new IllegalArgumentException(
                        "Les paramètres doivent être un objet JSON");
            }

            Set<String> keys = new HashSet<>();
            params.fieldNames().forEachRemaining(keys::add);

            switch (taskType) {

                case PING_PRINTER -> {
                    requireKeys(keys, Set.of("ipAddress"));

                    JsonNode ip = params.get("ipAddress");

                    if (!ip.isTextual() || !isValidIpv4(ip.asText())) {
                        throw new IllegalArgumentException(
                                "Adresse IP de l'imprimante invalide");
                    }
                }

                case CHECK_PRINTER_PORT -> {
                    requireKeys(keys, Set.of("ipAddress", "port"));

                    JsonNode ip = params.get("ipAddress");
                    JsonNode port = params.get("port");

                    if (!ip.isTextual() || !isValidIpv4(ip.asText())) {
                        throw new IllegalArgumentException(
                                "Adresse IP invalide");
                    }

                    if (!port.isIntegralNumber()) {
                        throw new IllegalArgumentException(
                                "Port invalide");
                    }

                    int portNumber = port.asInt();

                    if (!Set.of(9100, 515, 631).contains(portNumber)) {
                        throw new IllegalArgumentException(
                                "Port imprimante non autorisé");
                    }
                }

                case RESTART_PRINT_SPOOLER,
                     GET_SYSTEM_INFO,
                     GET_PRINTERS -> {
                    requireKeys(keys, Set.of());
                }

                case TEST_PRINT -> {
                    JsonNode modeNode = params.get("mode");

                    if (modeNode == null || !modeNode.isTextual()) {
                        throw new IllegalArgumentException(
                                "Mode d'impression obligatoire");
                    }

                    String mode = modeNode.asText();

                    switch (mode) {

                        case "WINDOWS" -> {
                            requireKeys(keys, Set.of("mode", "printerName"));

                            JsonNode printerName = params.get("printerName");

                            if (!printerName.isTextual()
                                    || printerName.asText().isBlank()
                                    || printerName.asText().length() > 128) {
                                throw new IllegalArgumentException(
                                        "Nom de l'imprimante invalide");
                            }
                        }

                        case "NETWORK" -> {
                            requireKeys(keys, Set.of("mode", "ipAddress", "port"));

                            JsonNode ip = params.get("ipAddress");
                            JsonNode port = params.get("port");

                            if (!ip.isTextual() || !isValidIpv4(ip.asText())) {
                                throw new IllegalArgumentException(
                                        "Adresse IP invalide");
                            }

                            if (!port.isIntegralNumber()
                                    || !port.canConvertToInt()
                                    || port.asInt() != 9100) {
                                throw new IllegalArgumentException(
                                        "Seul le port RAW 9100 est autorisé");
                            }
                        }

                        default -> throw new IllegalArgumentException(
                                "Mode d'impression non autorisé");
                    }
                }

                case INSTALL_DEFAULT_PRINTER -> {
                    requireKeys(keys, Set.of(
                            "printerName",
                            "ipAddress",
                            "port",
                            "driverName"
                    ));

                    JsonNode printerName = params.get("printerName");
                    JsonNode ip = params.get("ipAddress");
                    JsonNode port = params.get("port");
                    JsonNode driverName = params.get("driverName");

                    if (!printerName.isTextual()
                            || printerName.asText().isBlank()
                            || printerName.asText().length() > 128) {
                        throw new IllegalArgumentException(
                                "Nom de l'imprimante invalide");
                    }

                    if (!ip.isTextual() || !isValidIpv4(ip.asText())) {
                        throw new IllegalArgumentException(
                                "Adresse IP invalide");
                    }

                    if (!port.isIntegralNumber()
                            || !port.canConvertToInt()
                            || port.asInt() != 9100) {
                        throw new IllegalArgumentException(
                                "Seul le port RAW 9100 est autorisé");
                    }

                    if (!driverName.isTextual()
                            || driverName.asText().isBlank()
                            || driverName.asText().length() > 128) {
                        throw new IllegalArgumentException(
                                "Nom du pilote invalide");
                    }
                }

            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Paramètres JSON invalides");
        }
    }

    private void requireKeys(
            Set<String> actual,
            Set<String> expected) {

        if (!actual.equals(expected)) {
            throw new IllegalArgumentException(
                    "Paramètres manquants ou non autorisés");
        }
    }

    private boolean isValidIpv4(String ip) {
        if (ip == null || !ip.matches(
                "^(\\d{1,3}\\.){3}\\d{1,3}$")) {
            return false;
        }

        for (String part : ip.split("\\.")) {
            int value = Integer.parseInt(part);

            if (value < 0 || value > 255) {
                return false;
            }
        }

        return true;
    }

    @Transactional
    public RemoteTaskResponse getNextTask(String agentId) {

        Agent agent = agentRepository.findByAgentId(agentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Agent introuvable"));

        if (agent.getStatus() != AgentStatus.ONLINE) {
            throw new IllegalStateException("Agent OFFLINE");
        }

        LocalDateTime now = LocalDateTime.now();

        var tasks = taskRepository.findPendingTasksForUpdate(
                agentId,
                TaskStatus.PENDING,
                now
        );

        if (tasks.isEmpty()) {
            return null;
        }

        RemoteTask task = tasks.get(0);

        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartedAt(now);

        RemoteTask savedTask = taskRepository.save(task);

        return RemoteTaskResponse.from(savedTask);
    }


    @Transactional
    public void completeTask(
            Long taskId,
            String agentId,
            boolean success,
            String resultMessage) {

        RemoteTask task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new RuntimeException("Tâche introuvable"));

        if (!task.getAgent().getAgentId().equals(agentId)) {
            throw new RuntimeException(
                    "Cette tâche n'appartient pas à cet agent");
        }

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new RuntimeException(
                    "Cette tâche n'est pas en cours d'exécution");
        }

        task.setStatus(
                success ? TaskStatus.SUCCESS : TaskStatus.FAILED
        );

        task.setResultMessage(resultMessage);
        task.setCompletedAt(LocalDateTime.now());

        taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Page<RemoteTaskListResponse> getAllTasks(
            Long id,
            String agentId,
            TaskStatus status,
            TaskType taskType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size
    ) {

        if (page < 0) {
            throw new IllegalArgumentException("Page invalide");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "La taille doit être comprise entre 1 et 100"
            );
        }

        if (startDate != null && endDate != null
                && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "La date de début doit précéder la date de fin"
            );
        }

        Specification<RemoteTask> spec =
                Specification.unrestricted();

        spec = spec.and(RemoteTaskSpecification.hasTaskId(id));
        spec = spec.and(RemoteTaskSpecification.hasAgentId(agentId));
        spec = spec.and(RemoteTaskSpecification.hasStatus(status));
        spec = spec.and(RemoteTaskSpecification.hasTaskType(taskType));
        spec = spec.and(RemoteTaskSpecification.createdAfter(startDate));
        spec = spec.and(RemoteTaskSpecification.createdBefore(endDate));

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return taskRepository.findAll(spec, pageable)
                .map(RemoteTaskListResponse::from);
    }

}