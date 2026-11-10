package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.config.MqttProperties;
import com.example.demo.dto.DeviceDtos.ControlAllResponse;
import com.example.demo.dto.DeviceDtos.CommandStatusResponse;
import com.example.demo.dto.DeviceDtos.DeviceControlRequest;
import com.example.demo.dto.DeviceDtos.DeviceControlResponse;
import com.example.demo.dto.DeviceDtos.DeviceDto;
import com.example.demo.enums.DeviceAction;
import com.example.demo.enums.DeviceConnectionStatus;
import com.example.demo.feature.mqtt.MqttEvents.DeviceSnapshot;
import com.example.demo.feature.mqtt.MqttPublisher;
import com.example.demo.mapper.IotMapper;
import com.example.demo.model.Device;
import com.example.demo.repository.DeviceRepository;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final CommandLifecycleService commandLifecycleService;
    private final MqttPublisher mqttPublisher;
    private final MqttProperties mqttProperties;
    private final IotMapper mapper;

    public DeviceService(DeviceRepository deviceRepository,
                         CommandLifecycleService commandLifecycleService, MqttPublisher mqttPublisher,
                         MqttProperties mqttProperties, IotMapper mapper) {
        this.deviceRepository = deviceRepository;
        this.commandLifecycleService = commandLifecycleService;
        this.mqttPublisher = mqttPublisher;
        this.mqttProperties = mqttProperties;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<DeviceDto> getDevices() {
        return deviceRepository.findAllByOrderByDeviceCode().stream().map(mapper::toDeviceDto).toList();
    }

    @Transactional
    public DeviceControlResponse control(String id, DeviceControlRequest request, String userId) {
        Device device = deviceRepository.findByDeviceCode(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", "Không tìm thấy thiết bị"));
        int brightness = request.brightness() == null ? device.getBrightness() : request.brightness();
        String commandId = nextCommandId();
        mqttPublisher.publishCommand(device.getColor() + (request.isOn() ? " ON" : " OFF"));

        DeviceAction action = device.isEnabled() == request.isOn() && request.brightness() != null
                ? DeviceAction.CHANGE_BRIGHTNESS
                : (request.isOn() ? DeviceAction.TURN_ON : DeviceAction.TURN_OFF);
        OffsetDateTime now = OffsetDateTime.now();
        device.setEnabled(request.isOn());
        device.setBrightness(brightness);
        device.setUpdatedAt(now);
        if (!mqttPublisher.isEnabled()) device.setConnectionStatus(DeviceConnectionStatus.ONLINE);
        UserProfile user = getUser(userId);
        actionRepository.save(new ActionLog(commandId, device, user, action, CommandStatus.SUCCESS,
                request.isOn(), brightness, now));
        return new DeviceControlResponse(commandId, CommandStatus.SUCCESS.value(), mapper.toDeviceDto(device));
    }

    @Transactional
    public ControlAllResponse controlAll(boolean isOn, String userId) {
        List<Device> devices = deviceRepository.findAllByOrderByDeviceCode();
        if (devices.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", "Không có thiết bị");
        }
        mqttPublisher.publishCommand("ALL " + (isOn ? "ON" : "OFF"));
        UserProfile user = getUser(userId);
        OffsetDateTime now = OffsetDateTime.now();
        for (Device device : devices) {
            device.setEnabled(isOn);
            device.setUpdatedAt(now);
            if (!mqttPublisher.isEnabled()) device.setConnectionStatus(DeviceConnectionStatus.ONLINE);
            actionRepository.save(new ActionLog(nextCommandId(), device, user,
                    isOn ? DeviceAction.TURN_ON : DeviceAction.TURN_OFF, CommandStatus.SUCCESS,
                    isOn, device.getBrightness(), now));
        }
        return new ControlAllResponse(devices.size(), List.of());
    }

    @EventListener
    @Transactional
    public void ingest(DeviceSnapshot snapshot) {
        snapshot.states().forEach((id, enabled) -> deviceRepository.findByDeviceCode(id).ifPresent(device -> {
            device.setEnabled(enabled);
            device.setConnectionStatus(DeviceConnectionStatus.ONLINE);
            device.setLastSeenAt(snapshot.receivedAt());
            device.setUpdatedAt(snapshot.receivedAt());
        }));
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void markStaleDevicesOffline() {
        if (!mqttProperties.enabled()) return;
        OffsetDateTime threshold = OffsetDateTime.now().minusSeconds(mqttProperties.offlineAfterSeconds());
        for (Device device : deviceRepository.findAll()) {
            if (device.getLastSeenAt() == null || device.getLastSeenAt().isBefore(threshold)) {
                device.setConnectionStatus(DeviceConnectionStatus.OFFLINE);
            }
        }
    }

    private UserProfile getUser(String userId) {
        return profileRepository.findByUsername(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Không tìm thấy người dùng"));
    }

    private String nextCommandId() {
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT));
        return "CMD-" + timestamp + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
