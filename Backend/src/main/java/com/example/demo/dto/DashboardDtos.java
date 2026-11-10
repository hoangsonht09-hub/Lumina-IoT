package com.example.demo.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public final class DashboardDtos {
    private DashboardDtos() { }

    public record Room(String name, String status) { }
    public record Metric(double value, String unit) { }
    public record DeviceCount(int total, int active, int offline) { }
    public record DashboardSummary(
            Room room, Map<String, Metric> sensors, Metric energyToday,
            DeviceCount devices, OffsetDateTime updatedAt,
            long led4OnCountToday, Boolean led4IsOn, OffsetDateTime led4UpdatedAt
    ) { }
}
