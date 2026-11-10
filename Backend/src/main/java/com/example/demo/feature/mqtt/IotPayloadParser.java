package com.example.demo.feature.mqtt;

import com.example.demo.feature.mqtt.MqttEvents.DeviceSnapshot;
import com.example.demo.feature.mqtt.MqttEvents.SensorSnapshot;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class IotPayloadParser {
    private static final Pattern SENSOR_PATTERN = Pattern.compile(
            "Temp:((?:[+-]?(?:\\d+(?:\\.\\d+)?|nan)C)|ERR)\\s*\\|\\s*Hum:((?:[+-]?(?:\\d+(?:\\.\\d+)?|nan)%)|ERR)\\s*\\|\\s*(?:LDR:(\\d+)|Lux:([+-]?(?:\\d+(?:\\.\\d+)?|nan|ERR)\\s*(?:lux)?))\\s*\\|\\s*Light:(TOI|SANG|UNKNOWN)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS_PATTERN = Pattern.compile(
            "Red:(?<red>ON|OFF)\\s*\\|\\s*Yel:(?<yellow>ON|OFF)\\s*\\|\\s*Gre:(?<green>ON|OFF)(?:\\s*\\|\\s*(?:Mode:(?<mode>AUTO|MANUAL)|LED4:(?<led4>ON|OFF)))?",
            Pattern.CASE_INSENSITIVE);

    public SensorSnapshot parseSensorPayload(String payload) {
        Matcher matcher = SENSOR_PATTERN.matcher(payload.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Payload cáº£m biáº¿n khÃ´ng Ä‘Ãºng Ä‘á»‹nh dáº¡ng: " + payload);
        }
        Double temperature = parseDhtReading(matcher.group(1), "C", "temperature");
        Double humidity = parseDhtReading(matcher.group(2), "%", "humidity");
        String legacyLdr = matcher.group(3);
        String luxValue = matcher.group(4);
        String lightStatus = matcher.group(5);
        boolean lightKnown = !"UNKNOWN".equalsIgnoreCase(lightStatus)
                && !(luxValue != null && luxValue.toUpperCase(Locale.ROOT).contains("ERR"));
        double light = luxValue == null ? Integer.parseInt(legacyLdr)
                : (!lightKnown ? 0 : parseFinite(luxValue.replaceAll("(?i)\\s*lux$", ""), "light"));
        boolean dark = "TOI".equalsIgnoreCase(lightStatus);
        return new SensorSnapshot(temperature, humidity, light, dark, luxValue != null,
                lightKnown, OffsetDateTime.now());
    }

    public DeviceSnapshot parseStatusPayload(String payload) {
        Matcher matcher = STATUS_PATTERN.matcher(payload.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Payload tráº¡ng thÃ¡i khÃ´ng Ä‘Ãºng Ä‘á»‹nh dáº¡ng: " + payload);
        }
        Map<String, Boolean> states = new LinkedHashMap<>();
        states.put("LED_01", "ON".equalsIgnoreCase(matcher.group("red")));
        states.put("LED_02", "ON".equalsIgnoreCase(matcher.group("yellow")));
        states.put("LED_03", "ON".equalsIgnoreCase(matcher.group("green")));
        String led4 = matcher.group("led4");
        if (led4 != null) states.put("LED_04", "ON".equalsIgnoreCase(led4));
        String mode = matcher.group("mode");
        return new DeviceSnapshot(states, mode == null ? "AUTO" : mode.toUpperCase(Locale.ROOT), OffsetDateTime.now());
    }

    private double parseFinite(String value, String field) {
        double parsed = Double.parseDouble(value);
        if (!Double.isFinite(parsed)) {
            throw new IllegalArgumentException("GiÃ¡ trá»‹ " + field + " khÃ´ng há»£p lá»‡");
        }
        return parsed;
    }

    private Double parseDhtReading(String value, String unit, String field) {
        if ("ERR".equalsIgnoreCase(value)) return null;
        return parseFinite(value.substring(0, value.length() - unit.length()), field);
    }
}

