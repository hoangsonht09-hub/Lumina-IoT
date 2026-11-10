package com.example.demo;

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
import com.example.demo.repository.ActionLogRepository;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.SensorReadingRepository;
import com.example.demo.repository.SensorRepository;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.service.ActionService;
import com.example.demo.service.AuthService;
import com.example.demo.service.ProfileService;
import com.example.demo.service.SensorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

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
                temperatureSensor, 35, SensorStatus.WARNING, baseTime.plusMinutes(2)
        ));
        sensorReadingRepository.save(new SensorReading(
                humiditySensor, 50, SensorStatus.NORMAL, baseTime.plusMinutes(3)
        ));

        var result = sensorService.history(
                "test", "temperature", "normal", 19D, 30D, null,
                baseTime.minusMinutes(1), baseTime.plusMinutes(5), 1, 1, "desc"
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().value()).isEqualTo(25);
        assertThat(result.pagination().totalItems()).isEqualTo(2);
        assertThat(result.pagination().totalPages()).isEqualTo(2);

        var timeResult = sensorService.history(
                null, null, null, null, null, "06/10/2026 08:01",
                null, null, 1, 10, "desc"
        );
        assertThat(timeResult.items()).singleElement().satisfies(item -> assertThat(item.value()).isEqualTo(25));

        var fieldValueResult = sensorService.history(
                null, null, null, null, null, null,
                null, null, 1, 5, "desc", "temperature", "25"
        );
        assertThat(fieldValueResult.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.type()).isEqualTo("temperature");
                    assertThat(item.value()).isEqualTo(25);
                });

        var fieldTimeResult = sensorService.history(
                null, null, null, null, null, null,
                null, null, 1, 5, "desc", "time", "08:01"
        );
        assertThat(fieldTimeResult.items()).singleElement()
                .satisfies(item -> assertThat(item.value()).isEqualTo(25));
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
                null, null, null, null, "09:01",
                null, null, 1, 10, "desc"
        );
        assertThat(timeResult.items()).singleElement()
                .satisfies(item -> assertThat(item.commandId()).isEqualTo("CMD-FILTER-2"));

        var uiFilterResult = actionService.history(
                null, null, "on", "success", "09:01",
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
    }
}
