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
import java.util.Map;

public class AgentTaskResultService {

    private final AgentConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AgentTaskResultService(AgentConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }


public boolean sendResult(
        RemoteTaskResponse task,
        String resultMessage) {

    try {
        // Vérifier si l'exécution a échoué
        String normalizedResult = resultMessage == null
                ? ""
                : resultMessage.trim();

        boolean success = !normalizedResult
                .toUpperCase()
                .startsWith("FAILED");

        Map<String, Object> result = Map.of(
                "success", success,
                "resultMessage", normalizedResult
        );

        String json = objectMapper.writeValueAsString(result);

        String serverUrl = config.getServerUrl()
                .replaceAll("/+$", "");

        String url = serverUrl
                + "/api/tasks/agents/"
                + task.agentId()
                + "/"
                + task.id()
                + "/result";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        json,
                        StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() >= 200
                && response.statusCode() < 300) {

            System.out.println(
                    "Résultat transmis au serveur : OK"
            );

            System.out.println(
                    "Statut d'exécution : "
                            + (success ? "SUCCESS" : "FAILED")
            );

            return true;
        }

        System.err.println(
                "Erreur transmission résultat : HTTP "
                        + response.statusCode()
                        + " - "
                        + response.body()
        );

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.err.println("Envoi du résultat interrompu.");

    } catch (Exception e) {
        System.err.println(
                "Erreur envoi résultat : " + e.getMessage()
        );
    }

    return false;
}

}
