package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.config.MqttProperties;
import com.example.demo.dto.DeviceDtos.CommandStatusResponse;
import com.example.demo.enums.CommandStatus;
import com.example.demo.enums.DeviceAction;
import com.example.demo.enums.DeviceConnectionStatus;
import com.example.demo.feature.mqtt.MqttEvents.DeviceSnapshot;
import com.example.demo.model.ActionLog;
import com.example.demo.model.Device;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.ActionLogRepository;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommandLifecycleService {
    private static final String TIMEOUT_REASON = "HARDWARE_ACK_TIMEOUT";
    private static final String PUBLISH_REASON = "MQTT_PUBLISH_FAILED";

    private final DeviceRepository deviceRepository;
    private final UserProfileRepository profileRepository;
    private final ActionLogRepository actionRepository;
    private final MqttProperties mqttProperties;

    public CommandLifecycleService(DeviceRepository deviceRepository,
                                   UserProfileRepository profileRepository,
                                   ActionLogRepository actionRepository,
                                   MqttProperties mqttProperties) {
        this.deviceRepository = deviceRepository;
        this.profileRepository = profileRepository;
        this.actionRepository = actionRepository;
        this.mqttProperties = mqttProperties;
    }

    @Transactional
    public CommandStatusResponse createPending(String commandId, String deviceCode, String userId,
                                               DeviceAction action, boolean requestedOn, int brightness) {
        Device device = deviceRepository.findByDeviceCodeForUpdate(deviceCode)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "DEVICE_NOT_FOUND", "Không tìm thấy thiết bị"));
        ensureNoPendingCommand(device);
        UserProfile user = getUser(userId);
        ActionLog command = actionRepository.saveAndFlush(new ActionLog(
                commandId, device, user, action, CommandStatus.PENDING,
                requestedOn, brightness, OffsetDateTime.now()
        ));
        return toResponse(command);
    }

    @Transactional
    public List<CommandStatusResponse> createPendingForAll(String userId, boolean requestedOn,
                                                            List<String> commandIds) {
        List<Device> devices = deviceRepository.findAllForUpdate();
        if (devices.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", "Không có thiết bị");
        }
        if (devices.size() != commandIds.size()) {
            throw new IllegalArgumentException("Số commandId không khớp số thiết bị");
        }
        devices.forEach(this::ensureNoPendingCommand);
        UserProfile user = getUser(userId);
        OffsetDateTime now = OffsetDateTime.now();
        List<CommandStatusResponse> commands = new ArrayList<>();
        for (int index = 0; index < devices.size(); index++) {
            Device device = devices.get(index);
            ActionLog command = actionRepository.save(new ActionLog(
                    commandIds.get(index), device, user,
                    requestedOn ? DeviceAction.TURN_ON : DeviceAction.TURN_OFF,
                    CommandStatus.PENDING, requestedOn, device.getBrightness(), now
            ));
            commands.add(toResponse(command));
        }
        actionRepository.flush();
        return commands;
    }

    @Transactional(readOnly = true)
    public CommandStatusResponse getCommand(String commandId) {
        return actionRepository.findByCommandId(commandId)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "COMMAND_NOT_FOUND", "Không tìm thấy lệnh điều khiển"));
    }

    @Transactional
    public void confirmSnapshot(DeviceSnapshot snapshot) {
        snapshot.states().forEach((deviceCode, enabled) ->
                deviceRepository.findByDeviceCodeForUpdate(deviceCode).ifPresent(device -> {
                    device.setEnabled(enabled);
                    device.setConnectionStatus(DeviceConnectionStatus.ONLINE);
                    device.setLastSeenAt(snapshot.receivedAt());
                    device.setUpdatedAt(snapshot.receivedAt());

                    actionRepository.findFirstByDeviceAndStatusOrderByCreatedAtAsc(
                                    device, CommandStatus.PENDING)
                            .filter(command -> command.isDeviceOn() == enabled)
                            .ifPresent(command -> {
                                device.setBrightness(command.getBrightness());
                                command.markSuccess(snapshot.receivedAt());
                            });
                }));
    }

    @Transactional
    public void completeWithoutHardware(String commandId) {
        ActionLog command = requireCommand(commandId);
        Device device = command.getDevice();
        OffsetDateTime now = OffsetDateTime.now();
        device.setEnabled(command.isDeviceOn());
        device.setBrightness(command.getBrightness());
        device.setConnectionStatus(DeviceConnectionStatus.ONLINE);
        device.setUpdatedAt(now);
        command.markSuccess(now);
    }

    @Transactional
    public void markPublishFailed(List<String> commandIds) {
        OffsetDateTime now = OffsetDateTime.now();
        commandIds.forEach(commandId -> actionRepository.findByCommandId(commandId)
                .ifPresent(command -> command.markFailed(PUBLISH_REASON, now)));
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void expirePendingCommands() {
        long timeoutSeconds = Math.max(1, mqttProperties.commandTimeoutSeconds());
        OffsetDateTime cutoff = OffsetDateTime.now().minusSeconds(timeoutSeconds);
        OffsetDateTime now = OffsetDateTime.now();
        actionRepository.findByStatusAndCreatedAtBefore(CommandStatus.PENDING, cutoff)
                .forEach(command -> command.markFailed(TIMEOUT_REASON, now));
    }

    private void ensureNoPendingCommand(Device device) {
        if (actionRepository.existsByDeviceAndStatus(device, CommandStatus.PENDING)) {
            throw new BusinessException(HttpStatus.CONFLICT, "DEVICE_COMMAND_PENDING",
                    "Thiết bị đang xử lý một lệnh khác");
        }
    }

    private ActionLog requireCommand(String commandId) {
        return actionRepository.findByCommandId(commandId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "COMMAND_NOT_FOUND", "Không tìm thấy lệnh điều khiển"));
    }

    private UserProfile getUser(String userId) {
        return profileRepository.findByUsername(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED,
                        "USER_NOT_FOUND", "Không tìm thấy người dùng"));
    }

    private CommandStatusResponse toResponse(ActionLog command) {
        return new CommandStatusResponse(
                command.getCommandId(), command.getDeviceId(), command.getStatus().value(),
                command.isDeviceOn(), command.getCreatedAt(), command.getCompletedAt(),
                command.getFailureReason()
        );
    }
}
