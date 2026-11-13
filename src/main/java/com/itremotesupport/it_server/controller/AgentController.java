package com.itremotesupport.it_server.controller;

import com.itremotesupport.it_server.dto.AgentRegistrationRequest;
import com.itremotesupport.it_server.dto.AgentResponse;
import com.itremotesupport.it_server.dto.HeartbeatRequest;
import com.itremotesupport.it_server.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AgentResponse register(
            @Valid @RequestBody AgentRegistrationRequest request) {
        return agentService.register(request);
    }

    @GetMapping
    public List<AgentResponse> findAll() {
        return agentService.findAll();
    }

    @PostMapping("/heartbeat")
    public AgentResponse heartbeat(
            @Valid @RequestBody HeartbeatRequest request) {

        return agentService.heartbeat(request);
    }
}