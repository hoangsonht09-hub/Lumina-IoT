package com.example.demo.mapper;

import com.example.demo.dto.ActionDtos.ActionDto;
import com.example.demo.dto.ActionDtos.DeviceState;
import com.example.demo.dto.ActionDtos.PerformedBy;
import com.example.demo.dto.DeviceDtos.DeviceDto;
import com.example.demo.dto.ProfileDtos.ProfileDto;
import com.example.demo.dto.ProfileDtos.ProfileLinksDto;
import com.example.demo.dto.SensorDtos.RealtimeSensorDto;
import com.example.demo.dto.SensorDtos.SensorReadingDto;
import com.example.demo.model.ActionLog;
import com.example.demo.model.Device;
import com.example.demo.model.SensorReading;
import com.example.demo.model.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class IotMapper {
    public DeviceDto toDeviceDto(Device device) {
        return new DeviceDto(device.getId(), device.getName(), "led",
                device.isEnabled(), device.getBrightness(), device.getConnectionStatus().value(), device.getUpdatedAt());
    }

    public SensorReadingDto toSensorDto(SensorReading reading) {
        return new SensorReadingDto("SS-%06d".formatted(reading.getId()), reading.getSensorId(),
                reading.getSensorName(), reading.getType().value(), reading.getValue(), reading.getUnit(),
                reading.getStatus().value(), reading.getRecordedAt());
    }

    public RealtimeSensorDto toRealtimeDto(SensorReading reading) {
        return new RealtimeSensorDto(reading.getSensorId(), reading.getType().value(), reading.getValue(),
                reading.getUnit(), reading.getStatus().value(), reading.getRecordedAt());
    }

    public ActionDto toActionDto(ActionLog action) {
        String actionLabel = action.getCommandId().startsWith("AUTO-LED4-")
                ? (action.isDeviceOn() ? "Bật tự động" : "Tắt tự động")
                : action.getAction().label();
        return new ActionDto("ACT-%06d".formatted(action.getId()), action.getCommandId(), action.getDeviceId(),
                action.getDeviceName(), action.getAction().value(), actionLabel, action.getStatus().value(),
                new DeviceState(action.isDeviceOn(), action.getBrightness()),
                new PerformedBy(action.getPerformedById(), action.getPerformedByName()), action.getCreatedAt());
    }

    public ProfileDto toProfileDto(UserProfile profile) {
        return new ProfileDto(profile.getDisplayStudentId(), profile.getFullName(), profile.getEmail(),
                profile.getOrganization(), profile.getPracticeRoom(), profile.getRole(), profile.getManagedDevices(),
                new ProfileLinksDto(profile.getGithubUrl(), profile.getFigmaUrl(),
                        profile.getProjectDocsUrl(), profile.getApiDocsUrl()));
    }
}
