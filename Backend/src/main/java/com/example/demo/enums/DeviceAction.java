package com.example.demo.enums;

public enum DeviceAction {
    TURN_ON("turn_on", "Bật thiết bị"),
    TURN_OFF("turn_off", "Tắt thiết bị"),
    CHANGE_BRIGHTNESS("change_brightness", "Đổi độ sáng");

    private final String value;
    private final String label;

    DeviceAction(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() { return value; }
    public String label() { return label; }
}
