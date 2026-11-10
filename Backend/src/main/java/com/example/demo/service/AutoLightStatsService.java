package com.example.demo.service;

import com.example.demo.feature.mqtt.MqttEvents.DeviceSnapshot;
import com.example.demo.model.AutoLightEvent;
import com.example.demo.model.ActionLog;
import com.example.demo.enums.CommandStatus;
import com.example.demo.enums.DeviceAction;
import com.example.demo.repository.ActionLogRepository;
import com.example.demo.repository.AutoLightEventRepository;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.UserProfileRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class AutoLightStatsService {
    private static final ZoneId HISTORY_ZONE = ZoneId.of("Asia/Bangkok");

    private final AutoLightEventRepository eventRepository;
    private final ActionLogRepository actionRepository;
    private final DeviceRepository deviceRepository;
    private final UserProfileRepository profileRepository;

    public AutoLightStatsService(AutoLightEventRepository eventRepository,
                                 ActionLogRepository actionRepository,
                                 DeviceRepository deviceRepository,
                                 UserProfileRepository profileRepository) {
        this.eventRepository = eventRepository;
        this.actionRepository = actionRepository;
        this.deviceRepository = deviceRepository;
        this.profileRepository = profileRepository;
    }

    @EventListener
    @Transactional
    public void ingest(DeviceSnapshot snapshot) {
        Boolean turnedOn = snapshot.states().get("LED_04");
        if (turnedOn == null) return;

        Optional<AutoLightEvent> latest = eventRepository.findTopByOrderByObservedAtDescIdDesc();
        if (latest.isPresent() && latest.get().isTurnedOn() == turnedOn) return;

        // Treat the first observed ON as a turn-on; an initial OFF is only a baseline.
        boolean transition = latest.isEmpty() ? turnedOn : latest.get().isTurnedOn() != turnedOn;
        AutoLightEvent event = eventRepository.save(new AutoLightEvent(turnedOn, transition, snapshot.receivedAt()));
        if (!transition) return;

        var device = deviceRepository.findByDeviceCode("LED_04").orElseThrow();
        var system = profileRepository.findByUsername("SYSTEM_AUTO").orElseThrow();
        actionRepository.save(new ActionLog(
                "AUTO-LED4-" + event.getId(), device, system,
                turnedOn ? DeviceAction.TURN_ON : DeviceAction.TURN_OFF,
                CommandStatus.SUCCESS, turnedOn, 100, snapshot.receivedAt()));
    }

    @Transactional(readOnly = true)
    public DailyStats getDailyStats() {
        OffsetDateTime now = OffsetDateTime.now(HISTORY_ZONE);
        OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay(HISTORY_ZONE).toOffsetDateTime();
        OffsetDateTime startOfNextDay = now.toLocalDate().plusDays(1)
                .atStartOfDay(HISTORY_ZONE).toOffsetDateTime();
        long onCount = eventRepository
                .countByTransitionTrueAndTurnedOnTrueAndObservedAtGreaterThanEqualAndObservedAtLessThan(
                        startOfDay, startOfNextDay);

        Optional<AutoLightEvent> latest = eventRepository.findTopByOrderByObservedAtDescIdDesc();
        return new DailyStats(onCount,
                latest.map(AutoLightEvent::isTurnedOn).orElse(null),
                latest.map(AutoLightEvent::getObservedAt).orElse(null));
    }

    public record DailyStats(long onCountToday, Boolean isOn, OffsetDateTime updatedAt) { }
}
