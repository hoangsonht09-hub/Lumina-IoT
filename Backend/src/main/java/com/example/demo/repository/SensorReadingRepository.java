package com.example.demo.repository;

import com.example.demo.enums.SensorType;
import com.example.demo.model.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long>, JpaSpecificationExecutor<SensorReading> {
    Optional<SensorReading> findTopBySensor_TypeOrderByCreatedAtDesc(SensorType type);
}
