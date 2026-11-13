
package com.itremotesupport.it_agent.config;

public class AgentConfig {

    public String getAgentId() {
        return getRequiredEnv("AGENT_ID");
    }

    public String getServerUrl() {
        return getRequiredEnv("SERVER_URL");
    }

    public int getPollingInterval() {
        return Integer.parseInt(
                System.getenv().getOrDefault(
                        "AGENT_POLLING_INTERVAL", "10"
                )
        );
    }

    public String getServerUsername() {
        return System.getenv("SERVER_USERNAME");
    }

    public String getServerPassword() {
        return System.getenv("SERVER_PASSWORD");
    }

    private String getRequiredEnv(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Variable d'environnement manquante : " + name
            );
        }

        return value;
    }
}
