package com.example.demo.service;

import com.example.demo.feature.mqtt.MqttEvents.DeviceSnapshot;
import com.example.demo.model.AutoLightEvent;
import com.example.demo.model.Device;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.ActionLogRepository;
import com.example.demo.repository.AutoLightEventRepository;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class AutoLightStatsServiceTest {
    @Mock
    private AutoLightEventRepository eventRepository;
    @Mock
    private ActionLogRepository actionRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private UserProfileRepository profileRepository;

    @Test
    void manualFourthLedDoesNotCreateAutomaticHistory() {
        var service = new AutoLightStatsService(eventRepository, actionRepository, deviceRepository, profileRepository);
        when(deviceRepository.findByDeviceCode("LED_04"))
                .thenReturn(Optional.of(new Device("LED_04", "Đèn 4", "LED4")));
        service.ingest(snapshot(true, OffsetDateTime.now()));
        org.mockito.Mockito.verifyNoInteractions(eventRepository, actionRepository, profileRepository);
    }

    @Test
    void storesOnlyLedFourStateTransitions() {
        AutoLightStatsService service = new AutoLightStatsService(
                eventRepository, actionRepository, deviceRepository, profileRepository);
        OffsetDateTime now = OffsetDateTime.parse("2026-10-14T16:00:00+07:00");
        AutoLightEvent onEvent = new AutoLightEvent(true, true, now);
        AutoLightEvent savedEvent = org.mockito.Mockito.mock(AutoLightEvent.class);
        when(savedEvent.getId()).thenReturn(1L, 2L);
        when(eventRepository.save(any(AutoLightEvent.class))).thenReturn(savedEvent);
        when(deviceRepository.findByDeviceCode("LED_04"))
                .thenReturn(Optional.of(new Device("LED_04", "Đèn 4 (tự động)", "AUTO")));
        when(profileRepository.findByUsername("SYSTEM_AUTO"))
                .thenReturn(Optional.of(new UserProfile("SYSTEM_AUTO", "Hệ thống tự động",
                        "system-auto@internal.invalid", "IoT", "Phòng IoT 01", "system", 0, "unused")));
        when(eventRepository.findTopByOrderByObservedAtDescIdDesc())
                .thenReturn(Optional.empty(), Optional.of(onEvent), Optional.of(onEvent));

        service.ingest(snapshot(true, now));
        service.ingest(snapshot(true, now.plusSeconds(2)));
        service.ingest(snapshot(false, now.plusSeconds(4)));

        verify(eventRepository, times(2)).save(any(AutoLightEvent.class));
        verify(actionRepository).save(argThat(action -> action.getAction().name().equals("TURN_ON")
                && action.getCommandId().equals("AUTO-LED4-1")));
        verify(actionRepository).save(argThat(action -> action.getAction().name().equals("TURN_OFF")
                && action.getCommandId().equals("AUTO-LED4-2")));
    }

    private DeviceSnapshot snapshot(boolean ledFourOn, OffsetDateTime receivedAt) {
        return new DeviceSnapshot(Map.of("LED_04", ledFourOn), "AUTO", receivedAt);
    }
}
