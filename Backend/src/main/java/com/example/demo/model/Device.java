package com.example.demo.model;

import com.example.demo.enums.DeviceConnectionStatus;
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
@Table(name = "devices")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long databaseId;

    @Column(name = "device_code", nullable = false, unique = true, length = 50)
    private String deviceCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String color;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int brightness;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeviceConnectionStatus connectionStatus;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "device")
    private List<ActionLog> actions = new ArrayList<>();

    protected Device() { }

    public Device(String deviceCode, String name, String color) {
        this.deviceCode = deviceCode;
        this.name = name;
        this.color = color;
        this.enabled = false;
        this.brightness = 100;
        this.connectionStatus = DeviceConnectionStatus.OFFLINE;
        this.updatedAt = OffsetDateTime.now();
        this.createdAt = this.updatedAt;
    }

    public Long getDatabaseId() { return databaseId; }
    public String getId() { return deviceCode; }
    public String getDeviceCode() { return deviceCode; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public boolean isEnabled() { return enabled; }
    public int getBrightness() { return brightness; }
    public DeviceConnectionStatus getConnectionStatus() { return connectionStatus; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<ActionLog> getActions() { return actions; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setBrightness(int brightness) { this.brightness = brightness; }
    public void setConnectionStatus(DeviceConnectionStatus connectionStatus) { this.connectionStatus = connectionStatus; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setLastSeenAt(OffsetDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
