package com.example.demo.service;

import com.example.demo.enums.SensorStatus;
import com.example.demo.enums.SensorType;
import com.example.demo.model.Sensor;
import com.example.demo.model.SensorReading;
import com.example.demo.repository.SensorRepository;
import com.example.demo.repository.SensorReadingRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

/** Explicitly simulated samples, persisted so charts and history share one source. */
@Service
@ConditionalOnProperty(name = "app.simulation.motion-enabled", havingValue = "true", matchIfMissing = true)
public class MotionSimulationService {
    private final SensorRepository sensors;
    private final SensorReadingRepository readings;
    public MotionSimulationService(SensorRepository sensors, SensorReadingRepository readings) {
        this.sensors = sensors;
        this.readings = readings;
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 5000)
    @Transactional
    public void generate() {
        Sensor sensor = sensors.findByType(SensorType.MOTION).orElseGet(() -> sensors.save(
                new Sensor("MOTION_01", "Chuyển động (giả lập)", SensorType.MOTION, "0/1")));
        long tick = System.currentTimeMillis() / 5000;
        int[] pattern = {0, 0, 1, 1, 0, 0, 1, 0, 1, 1};
        readings.save(new SensorReading(sensor, pattern[(int) (tick % pattern.length)],
                SensorStatus.NORMAL, OffsetDateTime.now()));
    }
}
