package com.example.demo.model;

import com.example.demo.enums.SensorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sensors")
public class Sensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_code", nullable = false, unique = true, length = 50)
    private String sensorCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SensorType type;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "sensor")
    private List<SensorReading> readings = new ArrayList<>();

    protected Sensor() { }

    public Sensor(String sensorCode, String name, SensorType type, String unit) {
        this.sensorCode = sensorCode;
        this.name = name;
        this.type = type;
        this.unit = unit;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getSensorCode() { return sensorCode; }
    public String getName() { return name; }
    public SensorType getType() { return type; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<SensorReading> getReadings() { return readings; }
}
