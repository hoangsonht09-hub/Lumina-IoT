package com.example.demo.config;

import com.example.demo.model.Device;
import com.example.demo.model.Sensor;
import com.example.demo.model.UserProfile;
import com.example.demo.enums.SensorType;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.SensorRepository;
import com.example.demo.repository.UserProfileRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class DataInitializer implements ApplicationRunner {
    private final DeviceRepository deviceRepository;
    private final SensorRepository sensorRepository;
    private final UserProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public DataInitializer(DeviceRepository deviceRepository, SensorRepository sensorRepository,
                           UserProfileRepository profileRepository,
                           PasswordEncoder passwordEncoder, AuthProperties authProperties) {
        this.deviceRepository = deviceRepository;
        this.sensorRepository = sensorRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (deviceRepository.count() == 0) {
            deviceRepository.saveAll(List.of(
                    new Device("LED_01", "LED đỏ", "RED"),
                    new Device("LED_02", "LED vàng", "YELLOW"),
                    new Device("LED_03", "LED xanh", "GREEN")
            ));
        }
        deviceRepository.findByDeviceCode("LED_04")
                .orElseGet(() -> deviceRepository.save(new Device("LED_04", "Đèn 4 (tự động)", "AUTO")));

        if (sensorRepository.count() == 0) {
            sensorRepository.saveAll(List.of(
                    new Sensor(SensorType.TEMPERATURE.sensorId(), SensorType.TEMPERATURE.label(),
                            SensorType.TEMPERATURE, SensorType.TEMPERATURE.unit()),
                    new Sensor(SensorType.HUMIDITY.sensorId(), SensorType.HUMIDITY.label(),
                            SensorType.HUMIDITY, SensorType.HUMIDITY.unit()),
                    new Sensor(SensorType.LIGHT.sensorId(), SensorType.LIGHT.label(),
                            SensorType.LIGHT, SensorType.LIGHT.unit())
            ));
        }
        sensorRepository.findByType(SensorType.LIGHT).ifPresent(sensor -> {
            if (!SensorType.LIGHT.unit().equals(sensor.getUnit())) {
                sensor.setUnit(SensorType.LIGHT.unit());
                sensorRepository.save(sensor);
            }
        });

        UserProfile profile = profileRepository.findByUsername("B23DCCN833")
                .orElseGet(() -> new UserProfile(
                        "B23DCCN833", "Nguyễn Hoàng Sơn", authProperties.email(), "PTIT",
                        "Phòng IoT 01", "student", 3, passwordEncoder.encode(authProperties.password())
                ));

        if (!passwordEncoder.matches(authProperties.password(), profile.getPasswordHash())) {
            profile.setPasswordHash(passwordEncoder.encode(authProperties.password()));
        }
        if (profile.getDisplayStudentId() == null || profile.getDisplayStudentId().isBlank()) {
            profile.setDisplayStudentId(profile.getStudentId());
        }
        if (profile.getGithubUrl() == null) {
            profile.setGithubUrl("https://github.com/Toanproptit/IOT_Light_Sensor.git");
        }
        if (profile.getFigmaUrl() == null) {
            profile.setFigmaUrl("https://www.figma.com/design/FAFolqqY1l4d1ouGOGe4EN/Untitled?node-id=0-1&p=f&t=hX9CBeMNZOFfyx5F-0");
        }
        if (profile.getProjectDocsUrl() == null) {
            profile.setProjectDocsUrl("#project-docs");
        }
        if (profile.getApiDocsUrl() == null) {
            profile.setApiDocsUrl("/docs/API_DOCUMENTATION.md");
        }
        profileRepository.save(profile);
        profileRepository.findByUsername("SYSTEM_AUTO").orElseGet(() -> profileRepository.save(
                new UserProfile("SYSTEM_AUTO", "Hệ thống tự động", "system-auto@internal.invalid",
                        "IoT", "Phòng IoT 01", "system", 0,
                        passwordEncoder.encode(UUID.randomUUID().toString()))));
    }
}
