package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.config.MqttProperties;
import com.example.demo.dto.DeviceDtos.CommandStatusResponse;
import com.example.demo.dto.DeviceDtos.ControlAllResponse;
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
    private static final String AUTO_DEVICE_CODE = "LED_04";
    private final DeviceRepository deviceRepository;
    private final CommandLifecycleService commandLifecycleService;
    private final MqttPublisher mqttPublisher;
    private final MqttProperties mqttProperties;
    private final IotMapper mapper;

    public DeviceService(DeviceRepository deviceRepository,
                         CommandLifecycleService commandLifecycleService,
                         MqttPublisher mqttPublisher,
                         MqttProperties mqttProperties,
                         IotMapper mapper) {
        this.deviceRepository = deviceRepository;
        this.commandLifecycleService = commandLifecycleService;
        this.mqttPublisher = mqttPublisher;
        this.mqttProperties = mqttProperties;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<DeviceDto> getDevices() {
        return deviceRepository.findAllByOrderByDeviceCode()
                .stream().map(mapper::toDeviceDto).toList();
    }

    public DeviceControlResponse control(String id, DeviceControlRequest request, String userId) {
        Device device = deviceRepository.findByDeviceCode(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "DEVICE_NOT_FOUND", "Không tìm thấy thiết bị"));
        int brightness = request.brightness() == null ? device.getBrightness() : request.brightness();
        String commandId = nextCommandId();
        DeviceAction action = device.isEnabled() == request.isOn() && request.brightness() != null
                ? DeviceAction.CHANGE_BRIGHTNESS
                : (request.isOn() ? DeviceAction.TURN_ON : DeviceAction.TURN_OFF);

        CommandStatusResponse command = commandLifecycleService.createPending(
                commandId, id, userId, action, request.isOn(), brightness);

        if (!mqttPublisher.isEnabled()) {
            commandLifecycleService.completeWithoutHardware(commandId);
            command = commandLifecycleService.getCommand(commandId);
            Device updated = deviceRepository.findByDeviceCode(id).orElseThrow();
            return new DeviceControlResponse(commandId, command.status(), mapper.toDeviceDto(updated));
        }

        try {
            mqttPublisher.publishCommand(device.getColor() + (request.isOn() ? " ON" : " OFF"));
        } catch (BusinessException exception) {
            commandLifecycleService.markPublishFailed(List.of(commandId));
            throw exception;
        }
        return new DeviceControlResponse(commandId, command.status(), mapper.toDeviceDto(device));
    }

    public ControlAllResponse controlAll(boolean isOn, String userId) {
        List<Device> devices = deviceRepository.findAllByOrderByDeviceCode();
        if (devices.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", "Không có thiết bị");
        }
        List<String> commandIds = devices.stream().map(device -> nextCommandId()).toList();
        List<CommandStatusResponse> commands = commandLifecycleService.createPendingForAll(
                userId, isOn, commandIds);

        if (!mqttPublisher.isEnabled()) {
            commandIds.forEach(commandLifecycleService::completeWithoutHardware);
            commands = commandIds.stream().map(commandLifecycleService::getCommand).toList();
            return new ControlAllResponse(devices.size(), List.of(), commands);
        }

        try {
            mqttPublisher.publishCommand("ALL " + (isOn ? "ON" : "OFF"));
        } catch (BusinessException exception) {
            commandLifecycleService.markPublishFailed(commandIds);
            throw exception;
        }
        return new ControlAllResponse(devices.size(), List.of(), commands);
    }

    @EventListener
    public void ingest(DeviceSnapshot snapshot) {
        commandLifecycleService.confirmSnapshot(snapshot);
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

    @Transactional(readOnly = true)
    public CommandStatusResponse getCommandStatus(String commandId) {
        return commandLifecycleService.getCommand(commandId);
    }

    private String nextCommandId() {
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT));
        return "CMD-" + timestamp + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
