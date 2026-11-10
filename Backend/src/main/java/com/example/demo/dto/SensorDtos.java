package com.example.demo.dto;

import java.time.OffsetDateTime;
import java.util.List;

public final class SensorDtos {
    private SensorDtos() { }

    public record SensorReadingDto(
            String id, String sensorId, String sensorName, String type,
            double value, String unit, String status, OffsetDateTime recordedAt
    ) { }

    public record RealtimeSensorDto(
            String sensorId, String type, double value, String unit,
            String status, OffsetDateTime recordedAt
    ) { }

    public record RealtimeSensorsResponse(List<RealtimeSensorDto> readings) { }
}
