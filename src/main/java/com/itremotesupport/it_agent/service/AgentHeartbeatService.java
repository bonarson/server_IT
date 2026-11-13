package com.itremotesupport.it_agent.service;

import com.itremotesupport.it_agent.config.AgentConfig;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class AgentHeartbeatService {

    private final AgentConfig config;
    private final HttpClient httpClient;
    private final NetworkInfoService networkService;

    public AgentHeartbeatService(AgentConfig config) {
        this.config = config;
        this.networkService = new NetworkInfoService();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void sendHeartbeat() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();

            NetworkInfoService.NetworkAddress network =
                    networkService.getPreferredNetworkAddress();

            String json = """
                    {
                        "agentId": "%s",
                        "hostname": "%s",
                        "ipAddress": "%s"
                    }
                    """.formatted(
                    escape(config.getAgentId()),
                    escape(hostname),
                    escape(network.ipAddress())
            );

            String credentials = config.getServerUsername()
                    + ":" + config.getServerPassword();

            String auth = Base64.getEncoder().encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            config.getServerUrl() + "/api/agents/heartbeat"
                    ))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
//                    .header("Authorization", "Basic " + auth)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                System.out.println("Heartbeat : OK | IP : "
                        + network.ipAddress());
            } else {
                System.err.println("Heartbeat échoué : "
                        + response.statusCode() + " - "
                        + response.body());
            }

        } catch (Exception e) {
            System.err.println("Erreur Heartbeat : " + e.getMessage());
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}