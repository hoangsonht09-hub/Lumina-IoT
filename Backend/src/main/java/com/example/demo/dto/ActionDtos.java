package com.example.demo.dto;

import java.time.OffsetDateTime;

public final class ActionDtos {
    private ActionDtos() { }

    public record DeviceState(boolean isOn, int brightness) { }
    public record PerformedBy(String id, String name) { }
    public record ActionDto(
            String id, String commandId, String deviceId, String deviceName,
            String action, String actionLabel, String status, DeviceState deviceState,
            PerformedBy performedBy, OffsetDateTime createdAt
    ) { }
}
