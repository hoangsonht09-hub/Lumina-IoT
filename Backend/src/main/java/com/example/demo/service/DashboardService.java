package com.example.demo.service;

import com.example.demo.dto.DashboardDtos.DashboardSummary;
import com.example.demo.dto.DashboardDtos.DeviceCount;
import com.example.demo.dto.DashboardDtos.Metric;
import com.example.demo.dto.DashboardDtos.Room;
import com.example.demo.enums.DeviceConnectionStatus;
import com.example.demo.enums.SensorType;
import com.example.demo.model.Device;
import com.example.demo.model.SensorReading;
import com.example.demo.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    private final DeviceRepository deviceRepository;
    private final SensorService sensorService;
    private final AutoLightStatsService autoLightStatsService;

    public DashboardService(DeviceRepository deviceRepository, SensorService sensorService,
                            AutoLightStatsService autoLightStatsService) {
        this.deviceRepository = deviceRepository;
        this.sensorService = sensorService;
        this.autoLightStatsService = autoLightStatsService;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        List<Device> devices = deviceRepository.findAllByDeviceCodeNotOrderByDeviceCode("LED_04");
        Map<String, Metric> sensors = new LinkedHashMap<>();
        OffsetDateTime updatedAt = null;
        for (SensorType type : SensorType.values()) {
            SensorReading reading = sensorService.latest(type);
            if (reading != null) {
                sensors.put(type.value(), new Metric(reading.getValue(), reading.getUnit()));
                if (updatedAt == null || reading.getRecordedAt().isAfter(updatedAt)) updatedAt = reading.getRecordedAt();
            }
        }
        int active = (int) devices.stream().filter(Device::isEnabled).count();
        int offline = (int) devices.stream()
                .filter(device -> device.getConnectionStatus() == DeviceConnectionStatus.OFFLINE).count();
        String roomStatus = offline == devices.size() ? "offline" : "online";
        AutoLightStatsService.DailyStats led4Stats = autoLightStatsService.getDailyStats();
        return new DashboardSummary(new Room("Phòng IoT 01", roomStatus), sensors,
                new Metric(0, "kWh"), new DeviceCount(devices.size(), active, offline),
                updatedAt == null ? OffsetDateTime.now() : updatedAt,
                led4Stats.onCountToday(), led4Stats.isOn(), led4Stats.updatedAt());
    }
}
