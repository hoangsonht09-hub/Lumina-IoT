package com.example.demo.enums;

public enum SensorType {
    TEMPERATURE("temperature", "TEMP_01", "Cảm biến nhiệt độ", "°C"),
    HUMIDITY("humidity", "HUM_01", "Cảm biến độ ẩm", "%"),
    LIGHT("light", "LIGHT_01", "Cảm biến ánh sáng", "lux");

    private final String value;
    private final String sensorId;
    private final String label;
    private final String unit;

    SensorType(String value, String sensorId, String label, String unit) {
        this.value = value;
        this.sensorId = sensorId;
        this.label = label;
        this.unit = unit;
    }

    public String value() { return value; }
    public String sensorId() { return sensorId; }
    public String label() { return label; }
    public String unit() { return unit; }

    public static SensorType fromValue(String value) {
        for (SensorType type : values()) {
            if (type.value.equalsIgnoreCase(value)) return type;
        }
        throw new IllegalArgumentException("Loại cảm biến không hợp lệ: " + value);
    }
}
