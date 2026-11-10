package com.example.demo.feature.mqtt;

import java.time.OffsetDateTime;
import java.util.Map;

public final class MqttEvents {
    private MqttEvents() { }

    public record SensorSnapshot(
            Double temperature,
            Double humidity,
            double lightValue,
            boolean dark,
            boolean luxReading,
            boolean lightKnown,
            OffsetDateTime receivedAt
    ) { }

    public record DeviceSnapshot(
            Map<String, Boolean> states,
            String mode,
            OffsetDateTime receivedAt
    ) { }
}
