package com.example.demo.repository;

import com.example.demo.enums.SensorType;
import com.example.demo.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
    Optional<Sensor> findBySensorCode(String sensorCode);
    Optional<Sensor> findByType(SensorType type);
}
