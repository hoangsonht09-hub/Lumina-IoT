package com.example.demo.model;

import com.example.demo.enums.SensorStatus;
import com.example.demo.enums.SensorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "data_sensors", indexes = {
        @Index(name = "idx_data_sensor_sensor_time", columnList = "sensor_id,created_at")
})
public class SensorReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Column(name = "`value`", nullable = false)
    private double value;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SensorStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SensorReading() { }

    public SensorReading(Sensor sensor, double value, SensorStatus status, OffsetDateTime createdAt) {
        this.sensor = sensor;
        this.value = value;
        this.unit = sensor.getUnit();
        this.status = status;
        this.createdAt = createdAt;
    }

    public SensorReading(Sensor sensor, double value, String unit, SensorStatus status, OffsetDateTime createdAt) {
        this.sensor = sensor;
        this.value = value;
        this.unit = unit;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Sensor getSensor() { return sensor; }
    public String getSensorId() { return sensor.getSensorCode(); }
    public String getSensorName() { return sensor.getName(); }
    public SensorType getType() { return sensor.getType(); }
    public double getValue() { return value; }
    public String getUnit() { return unit; }
    public SensorStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getRecordedAt() { return createdAt; }
}
