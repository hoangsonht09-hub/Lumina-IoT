package com.example.demo.service;

import com.example.demo.dto.DeviceDtos.DeviceControlRequest;
import com.example.demo.enums.DeviceAction;
import com.example.demo.feature.mqtt.MqttEvents.DeviceSnapshot;
import com.example.demo.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class CommandLifecycleServiceTests {
    @Autowired
    private CommandLifecycleService commandLifecycleService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    @Transactional
    void pendingCommandCompletesOnlyAfterMatchingHardwareStatus() {
        String commandId = "CMD-ACK-TEST-1";
        var pending = commandLifecycleService.createPending(
                commandId, "LED_01", "B23DCCN833",
                DeviceAction.TURN_ON, true, 100
        );

        assertThat(pending.status()).isEqualTo("pending");
        assertThat(deviceRepository.findByDeviceCode("LED_01").orElseThrow().isEnabled()).isFalse();

        commandLifecycleService.confirmSnapshot(new DeviceSnapshot(
                Map.of("LED_01", true), "MANUAL", OffsetDateTime.now()
        ));

        var completed = commandLifecycleService.getCommand(commandId);
        assertThat(completed.status()).isEqualTo("success");
        assertThat(completed.completedAt()).isNotNull();
        assertThat(deviceRepository.findByDeviceCode("LED_01").orElseThrow().isEnabled()).isTrue();
    }

    @Test
    @Transactional
    void mqttDisabledModeCompletesCommandImmediately() {
        var response = deviceService.control(
                "LED_02", new DeviceControlRequest(true, 100), "B23DCCN833"
        );

        assertThat(response.status()).isEqualTo("success");
        assertThat(commandLifecycleService.getCommand(response.commandId()).status()).isEqualTo("success");
    }

    @Test
    @Transactional
    void automaticLedIsExcludedFromManualCommands() {
        assertThatThrownBy(() -> deviceService.control(
                "LED_04", new DeviceControlRequest(true, 100), "B23DCCN833"))
                .hasMessageContaining("Đèn 4 được điều khiển tự động");

        var response = deviceService.controlAll(true, "B23DCCN833");
        assertThat(response.updatedDevices()).isEqualTo(3);
        assertThat(response.commands()).extracting(command -> command.deviceId())
                .containsExactly("LED_01", "LED_02", "LED_03");
    }
}
