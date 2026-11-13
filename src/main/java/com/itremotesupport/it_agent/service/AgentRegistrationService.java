package com.itremotesupport.it_agent.service;

import com.itremotesupport.it_agent.config.AgentConfig;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class AgentRegistrationService {

    private final AgentConfig config;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public AgentRegistrationService(AgentConfig config) {
        this.config = config;
    }

//    public void register() {
//        try {
//            String hostname = InetAddress.getLocalHost().getHostName();
//
//            NetworkInfoService networkService = new NetworkInfoService();
//
//            NetworkInfoService.NetworkAddress network =
//                    networkService.getPreferredNetworkAddress();
//
//            String ipAddress = network.ipAddress();
//
//            System.out.println("Interface sélectionnée : "
//                    + network.interfaceName());
//
//            System.out.println("IP sélectionnée : " + ipAddress);
//
//            String json = """
//                    {
//                        "agentId": "%s",
//                        "hostname": "%s",
//                        "ipAddress": "%s"
//                    }
//                    """.formatted(
//                    escape(config.getAgentId()),
//                    escape(hostname),
//                    escape(ipAddress)
//            );
//
//            String credentials = config.getServerUsername()
//                    + ":" + config.getServerPassword();
//
//            String auth = Base64.getEncoder().encodeToString(
//                    credentials.getBytes(StandardCharsets.UTF_8)
//            );
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(
//                            config.getServerUrl() + "/api/agents/register"
//                    ))
//                    .timeout(Duration.ofSeconds(10))
//                    .header("Content-Type", "application/json")
////                    .header("Authorization", "Basic " + auth)
//                    .POST(HttpRequest.BodyPublishers.ofString(json))
//                    .build();
//
//            HttpResponse<String> response = httpClient.send(
//                    request,
//                    HttpResponse.BodyHandlers.ofString()
//            );
//
//            if (response.statusCode() == 201) {
//                System.out.println("Enregistrement de l'agent : REUSSI");
//                System.out.println(response.body());
//            } else {
//                System.out.println("Erreur d'enregistrement : "
//                        + response.statusCode());
//                System.out.println(response.body());
//            }
//
//        } catch (Exception e) {
//            System.err.println("Impossible d'enregistrer l'agent : "
//                    + e.getMessage());
//        }
//    }


public boolean register() {
    try {
        String hostname = InetAddress.getLocalHost().getHostName();

        NetworkInfoService networkService = new NetworkInfoService();

        NetworkInfoService.NetworkAddress network =
                networkService.getPreferredNetworkAddress();

        String ipAddress = network.ipAddress();

        System.out.println("Agent ID : " + config.getAgentId());
        System.out.println("Interface sélectionnée : " + network.interfaceName());
        System.out.println("IP sélectionnée : " + ipAddress);

        String json = """
                {
                    "agentId": "%s",
                    "hostname": "%s",
                    "ipAddress": "%s"
                }
                """.formatted(
                escape(config.getAgentId()),
                escape(hostname),
                escape(ipAddress)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        config.getServerUrl() + "/api/agents/register"
                ))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("Code HTTP : " + response.statusCode());
        System.out.println("Réponse serveur : " + response.body());

        if (response.statusCode() >= 200 &&
                response.statusCode() < 300) {

            System.out.println("Enregistrement de l'agent : REUSSI");
            return true;
        }

        System.err.println("Échec de l'enregistrement de l'agent.");
        return false;

    } catch (Exception e) {
        System.err.println(
                "Impossible d'enregistrer l'agent : " + e.getMessage()
        );
        return false;
    }
}


    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}