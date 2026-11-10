package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mqtt")
public record MqttProperties(
        boolean enabled,
        String brokerUri,
        String username,
        String password,
        String clientId,
        String sensorTopic,
        String statusTopic,
        String commandTopic,
        long reconnectDelayMs,
        long offlineAfterSeconds,
        long commandTimeoutSeconds
) {
}
