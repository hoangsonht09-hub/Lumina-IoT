package com.example.demo.feature.mqtt;

public interface MqttPublisher {
    void publishCommand(String command);
    boolean isConnected();
    boolean isEnabled();
}
