package com.example.demo;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.AuthDtos.LoginRequest;
import com.example.demo.dto.ProfileDtos.ProfileLinksDto;
import com.example.demo.dto.ProfileDtos.ProfileUpdateRequest;
import com.example.demo.enums.CommandStatus;
import com.example.demo.enums.DeviceAction;
import com.example.demo.enums.SensorStatus;
import com.example.demo.enums.SensorType;
import com.example.demo.model.ActionLog;
import com.example.demo.model.Device;
import com.example.demo.model.Sensor;
import com.example.demo.model.SensorReading;
import com.example.demo.model.UserProfile;
import com.example.demo.feature.mqtt.MqttEvents.DeviceSnapshot;
import com.example.demo.repository.ActionLogRepository;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.SensorReadingRepository;
import com.example.demo.repository.SensorRepository;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.service.ActionService;
import com.example.demo.service.AutoLightStatsService;
import com.example.demo.service.AuthService;
import com.example.demo.service.DeviceService;
import com.example.demo.service.ProfileService;
import com.example.demo.service.SensorService;
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
class IotLightSensorApplicationTests {
    @Autowired
    private SensorReadingRepository sensorReadingRepository;
    @Autowired
    private SensorRepository sensorRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private SensorService sensorService;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private ActionLogRepository actionLogRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private ActionService actionService;
    @Autowired
    private AutoLightStatsService autoLightStatsService;
    @Autowired
    private DeviceService deviceService;

    @Test
    void contextLoadsAndPersistsSensorReading() {
        Sensor sensor = sensorRepository.findBySensorCode("TEMP_01").orElseThrow();
        SensorReading saved = sensorReadingRepository.save(new SensorReading(
                sensor, 28.5, SensorStatus.NORMAL, OffsetDateTime.now()
        ));
        assertThat(sensorReadingRepository.findById(saved.getId())).isPresent();
        assertThat(authService.login(new LoginRequest("test@example.com", "test-only-password")).accessToken())
                .isNotBlank();
    }

    @Test
    @Transactional
    void updatesEditableProfileFieldsAndProjectLinks() {
        var links = new ProfileLinksDto(
                "https://github.com/example/iot",
                "https://www.figma.com/design/example",
                "/docs/project.md",
                "/docs/api.md"
        );
        var updated = profileService.update("B23DCCN833", new ProfileUpdateRequest(
                "B23-IOT-001", "Nguyen Van A", "updated@example.com", "PTIT Ha Noi", links
        ));

        assertThat(updated.studentId()).isEqualTo("B23-IOT-001");
        assertThat(updated.fullName()).isEqualTo("Nguyen Van A");
        assertThat(updated.email()).isEqualTo("updated@example.com");
        assertThat(updated.organization()).isEqualTo("PTIT Ha Noi");
        assertThat(updated.links()).isEqualTo(links);
    }

    @Test
    @Transactional
    void filtersAndPaginatesSensorHistory() {
        OffsetDateTime baseTime = OffsetDateTime.parse("2026-10-06T08:00:00+07:00");
        Sensor temperatureSensor = sensorRepository.save(new Sensor(
                "TEMP_FILTER", "Temperature test", SensorType.TEMPERATURE, "C"
        ));
        Sensor humiditySensor = sensorRepository.save(new Sensor(
                "HUM_FILTER", "Humidity test", SensorType.HUMIDITY, "%"
        ));
        sensorReadingRepository.save(new SensorReading(
                temperatureSensor, 20, SensorStatus.NORMAL, baseTime
        ));
        sensorReadingRepository.save(new SensorReading(
                temperatureSensor, 25, SensorStatus.NORMAL, baseTime.plusMinutes(1)
        ));
        sensorReadingRepository.save(new SensorReading(
                temperatureSensor, 25.1, SensorStatus.NORMAL, baseTime.plusMinutes(2)
        ));
        sensorReadingRepository.save(new SensorReading(
                temperatureSensor, 25.2, SensorStatus.NORMAL, baseTime.plusMinutes(3)
        ));
        sensorReadingRepository.save(new SensorReading(
                temperatureSensor, 35, SensorStatus.WARNING, baseTime.plusMinutes(4)
        ));
        sensorReadingRepository.save(new SensorReading(
                humiditySensor, 50, SensorStatus.NORMAL, baseTime.plusMinutes(5)
        ));

        var result = sensorService.history(
                "test", "temperature", null, "normal", null,
                baseTime.minusMinutes(1), baseTime.plusMinutes(5), 1, 1, "desc"
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().value()).isEqualTo(25.2);
        assertThat(result.pagination().totalItems()).isEqualTo(4);
        assertThat(result.pagination().totalPages()).isEqualTo(4);

        var timeResult = sensorService.history(
                null, null, null, null, "2026-10-06 08:01",
                null, null, 1, 10, "desc"
        );
        assertThat(timeResult.items()).singleElement().satisfies(item -> assertThat(item.value()).isEqualTo(25));

        var exactValueResult = sensorService.history(
                null, "temperature", 25D, null, null,
                null, null, 1, 5, "desc"
        );
        assertThat(exactValueResult.items()).extracting(item -> item.value())
                .containsExactlyInAnyOrder(25D, 25.1, 25.2);

        var searchValueResult = sensorService.history(
                "35", "temperature", null, "warning", null,
                null, null, 1, 5, "desc"
        );
        assertThat(searchValueResult.items()).singleElement()
                .satisfies(item -> assertThat(item.value()).isEqualTo(35));
    }

    @Test
    @Transactional
    void filtersHistoryByOrderedDateTimePrecision() {
        OffsetDateTime baseTime = OffsetDateTime.parse("2042-03-04T05:06:07+07:00");
        Sensor sensor = sensorRepository.save(new Sensor(
                "LIGHT_TIME_FILTER", "Light time filter", SensorType.LIGHT, "lux"
        ));
        sensorReadingRepository.save(new SensorReading(sensor, 1, SensorStatus.NORMAL, baseTime));
        sensorReadingRepository.save(new SensorReading(sensor, 2, SensorStatus.NORMAL, baseTime.plusSeconds(1)));
        sensorReadingRepository.save(new SensorReading(sensor, 3, SensorStatus.NORMAL, baseTime.plusMinutes(1)));
        sensorReadingRepository.save(new SensorReading(sensor, 4, SensorStatus.NORMAL, baseTime.plusHours(1)));
        sensorReadingRepository.save(new SensorReading(sensor, 5, SensorStatus.NORMAL, baseTime.plusDays(1)));
        sensorReadingRepository.save(new SensorReading(sensor, 6, SensorStatus.NORMAL, baseTime.plusMonths(1)));
        sensorReadingRepository.save(new SensorReading(sensor, 7, SensorStatus.NORMAL, baseTime.plusYears(1)));

        assertThat(sensorHistoryCount("2042-03-04 05:06:07")).isEqualTo(1);
        assertThat(sensorHistoryCount("2042-03-04 05:06")).isEqualTo(2);
        assertThat(sensorHistoryCount("2042-03-04 05:06:")).isEqualTo(2);
        assertThat(sensorHistoryCount("2042-03-04 05")).isEqualTo(3);
        assertThat(sensorHistoryCount("2042-03-04 05::")).isEqualTo(3);
        assertThat(sensorHistoryCount("2042-03-04")).isEqualTo(4);
        assertThat(sensorHistoryCount("2042-03-04 ::")).isEqualTo(4);
        assertThat(sensorHistoryCount("2042-03")).isEqualTo(5);
        assertThat(sensorHistoryCount("2042-03-")).isEqualTo(5);
        assertThat(sensorHistoryCount("2042")).isEqualTo(6);
        assertThat(sensorHistoryCount("2042-")).isEqualTo(6);

        assertThatThrownBy(() -> sensorHistoryCount("04/03/2042 05:06"))
                .isInstanceOf(BusinessException.class);
    }

    private long sensorHistoryCount(String timeQuery) {
        return sensorService.history(
                null, "light", null, null, timeQuery,
                null, null, 1, 20, "desc"
        ).pagination().totalItems();
    }

    @Test
    @Transactional
    void filtersAndPaginatesActionHistory() {
        Device device = deviceRepository.save(new Device(
                "FILTER_LED", "Test LED", "RED"
        ));
        UserProfile user = userProfileRepository.findByUsername("B23DCCN833").orElseThrow();
        UserProfile otherUser = userProfileRepository.save(new UserProfile(
                "OTHER_USER", "Other User", "other-user@example.com", "PTIT",
                "IoT room", "student", 0, "test-password-hash"
        ));
        OffsetDateTime baseTime = OffsetDateTime.parse("2026-10-06T09:00:00+07:00");
        actionLogRepository.save(new ActionLog(
                "CMD-FILTER-1", device, user, DeviceAction.TURN_ON, CommandStatus.SUCCESS,
                true, 100, baseTime
        ));
        actionLogRepository.save(new ActionLog(
                "CMD-FILTER-2", device, user, DeviceAction.TURN_ON, CommandStatus.SUCCESS,
                true, 100, baseTime.plusMinutes(1)
        ));
        actionLogRepository.save(new ActionLog(
                "CMD-FILTER-3", device, otherUser, DeviceAction.TURN_OFF, CommandStatus.FAILED,
                false, 0, baseTime.plusMinutes(2)
        ));

        var result = actionService.history(
                "B23DCCN833", "FILTER_LED", "turn_on", "success", null,
                baseTime.minusMinutes(1), baseTime.plusMinutes(5), 1, 1, "desc"
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().commandId()).isEqualTo("CMD-FILTER-2");
        assertThat(result.pagination().totalItems()).isEqualTo(2);
        assertThat(result.pagination().totalPages()).isEqualTo(2);

        var timeResult = actionService.history(
                null, null, null, null, "2026-10-06 09:01",
                null, null, 1, 10, "desc"
        );
        assertThat(timeResult.items()).singleElement()
                .satisfies(item -> assertThat(item.commandId()).isEqualTo("CMD-FILTER-2"));

        var uiFilterResult = actionService.history(
                null, null, "on", "success", "2026-10-06 09:01",
                null, null, 1, 5, "desc", device.getName()
        );
        assertThat(uiFilterResult.items()).singleElement()
                .satisfies(item -> assertThat(item.commandId()).isEqualTo("CMD-FILTER-2"));

        var disconnectedResult = actionService.history(
                null, null, "disconnected", null, null,
                null, null, 1, 5, "desc", device.getName()
        );
        assertThat(disconnectedResult.items()).singleElement()
                .satisfies(item -> assertThat(item.commandId()).isEqualTo("CMD-FILTER-3"));

        var manualResult = actionService.history(
                null, null, "manual_turn_on", null, null,
                null, null, 1, 10, "desc", device.getName()
        );
        assertThat(manualResult.items()).hasSize(2);
    }

    @Test
    @Transactional
    void recordsAutomaticLedFourTransitionsInActionHistory() {
        OffsetDateTime first = OffsetDateTime.parse("2043-05-06T09:00:00+07:00");
        autoLightStatsService.ingest(new DeviceSnapshot(Map.of("LED_04", false), "AUTO", first));
        autoLightStatsService.ingest(new DeviceSnapshot(Map.of("LED_04", true), "AUTO", first.plusSeconds(2)));
        autoLightStatsService.ingest(new DeviceSnapshot(Map.of("LED_04", true), "AUTO", first.plusSeconds(4)));
        autoLightStatsService.ingest(new DeviceSnapshot(Map.of("LED_04", false), "AUTO", first.plusSeconds(6)));

        var history = actionService.history(null, "LED_04", null, "success", "2043-05-06 09:00",
                null, null, 1, 10, "asc");
        assertThat(history.items()).hasSize(2);
        assertThat(history.items().get(0).actionLabel()).isEqualTo("Bật tự động");
        assertThat(history.items().get(0).performedBy().name()).isEqualTo("Hệ thống tự động");
        assertThat(history.items().get(1).actionLabel()).isEqualTo("Tắt tự động");
        assertThat(history.pagination().totalItems()).isEqualTo(2);
        var automaticOn = actionService.history(null, "LED_04", "auto_turn_on", null, null,
                null, null, 1, 10, "desc");
        assertThat(automaticOn.items()).singleElement()
                .satisfies(item -> assertThat(item.actionLabel()).isEqualTo("Bật tự động"));
        var automaticOff = actionService.history(null, "LED_04", "auto_turn_off", null, null,
                null, null, 1, 10, "desc");
        assertThat(automaticOff.items()).singleElement()
                .satisfies(item -> assertThat(item.actionLabel()).isEqualTo("Tắt tự động"));
        var manualOn = actionService.history(null, "LED_04", "manual_turn_on", null, null,
                null, null, 1, 10, "desc");
        assertThat(manualOn.items()).isEmpty();
        assertThat(deviceService.getDevices()).noneMatch(device -> device.id().equals("LED_04"));
    }
}
