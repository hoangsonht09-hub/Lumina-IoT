package com.example.demo.enums;

public enum SensorStatus {
    NORMAL("normal"), WARNING("warning"), LOW("low");

    private final String value;
    SensorStatus(String value) { this.value = value; }
    public String value() { return value; }
}
