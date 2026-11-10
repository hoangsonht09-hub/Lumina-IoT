package com.example.demo.enums;

public enum DeviceConnectionStatus {
    ONLINE("online"), OFFLINE("offline");

    private final String value;
    DeviceConnectionStatus(String value) { this.value = value; }
    public String value() { return value; }
}
