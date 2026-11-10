package com.example.demo.enums;

public enum CommandStatus {
    PENDING("pending"), SUCCESS("success"), FAILED("failed");

    private final String value;
    CommandStatus(String value) { this.value = value; }
    public String value() { return value; }
}
