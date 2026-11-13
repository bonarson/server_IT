package com.itremotesupport.it_agent.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ServerConnectionService {

    private final HttpClient httpClient;
    private final String serverUrl;

    public ServerConnectionService(String serverUrl) {
        this.serverUrl = serverUrl;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean checkConnection() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            serverUrl + "/actuator/health"
                    ))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            return response.statusCode() == 200
                    && response.body().contains("\"UP\"");

        } catch (Exception e) {
            System.err.println(
                    "Serveur inaccessible : " + e.getMessage()
            );

            return false;
        }
    }
}
