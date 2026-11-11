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
                commandId, "LED_01", "B23DCCN721",
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
                "LED_02", new DeviceControlRequest(true, 100), "B23DCCN721"
        );

        assertThat(response.status()).isEqualTo("success");
        assertThat(commandLifecycleService.getCommand(response.commandId()).status()).isEqualTo("success");
    }
}
