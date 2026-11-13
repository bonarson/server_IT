package com.itremotesupport.it_agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itremotesupport.it_agent.config.AgentConfig;
import com.itremotesupport.it_agent.dto.RemoteTaskResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class AgentTaskPollingService {

    private final AgentConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AgentTaskPollingService(AgentConfig config) {
        this.config = config;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    public Optional<RemoteTaskResponse> fetchNextTask() {

        try {
            String url = config.getServerUrl()
                    + "/api/tasks/agents/"
                    + config.getAgentId()
                    + "/next";

            String credentials = config.getServerUsername()
                    + ":"
                    + config.getServerPassword();

            String encodedCredentials = Base64.getEncoder()
                    .encodeToString(
                            credentials.getBytes(StandardCharsets.UTF_8)
                    );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
//                    .header("Authorization",
//                            "Basic " + encodedCredentials)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 204) {
                return Optional.empty();
            }

            if (response.statusCode() == 200) {
                RemoteTaskResponse task = objectMapper.readValue(
                        response.body(),
                        RemoteTaskResponse.class
                );

                return Optional.of(task);
            }

            System.err.println(
                    "Erreur récupération tâche : HTTP "
                            + response.statusCode()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Récupération des tâches interrompue.");

        } catch (Exception e) {
            System.err.println(
                    "Erreur récupération tâche : " + e.getMessage()
            );
        }

        return Optional.empty();
    }
}