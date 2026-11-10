package com.example.demo.feature.mqtt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IotPayloadParserTest {
    private final IotPayloadParser parser = new IotPayloadParser();

    @Test
    void parsesThreeLedStatusFromUploadedFirmware() {
        var result = parser.parseStatusPayload("Red:OFF | Yel:OFF | Gre:OFF");
        assertThat(result.states()).hasSize(3).containsEntry("LED_01", false)
                .containsEntry("LED_02", false).containsEntry("LED_03", false);
        assertThat(result.states()).doesNotContainKey("LED_04");
    }

    @Test
    void parsesSensorPayloadProducedByEsp32() {
        var result = parser.parseSensorPayload("Temp:28.5C | Hum:72.0% | LDR:1 | Light:TOI");
        assertThat(result.temperature()).isEqualTo(28.5);
        assertThat(result.humidity()).isEqualTo(72.0);
        assertThat(result.lightValue()).isEqualTo(1);
        assertThat(result.dark()).isTrue();
        assertThat(result.luxReading()).isFalse();
    }

    @Test
    void parsesBh1750LuxPayloadProducedByNewFirmware() {
        var result = parser.parseSensorPayload("Temp:28.5C | Hum:72.0% | Lux:123.4lux | Light:SANG");
        assertThat(result.lightValue()).isEqualTo(123.4);
        assertThat(result.dark()).isFalse();
        assertThat(result.luxReading()).isTrue();
        assertThat(result.lightKnown()).isTrue();
    }

    @Test
    void parsesLuxPayloadWithoutSpaceBeforeUnit() {
        var result = parser.parseSensorPayload("Temp:28.5C | Hum:72.0% | Lux:123.4 lux | Light:SANG");
        assertThat(result.lightValue()).isEqualTo(123.4);
    }

    @Test
    void parsesErrBh1750ReadingWithoutTreatingItAsDark() {
        var result = parser.parseSensorPayload("Temp:28.5C | Hum:72.0% | Lux:ERR | Light:UNKNOWN");
        assertThat(result.lightKnown()).isFalse();
        assertThat(result.dark()).isFalse();
    }

    @Test
    void parsesLedStatusProducedByEsp32() {
        var result = parser.parseStatusPayload("Red:ON | Yel:OFF | Gre:ON | Mode:MANUAL");
        assertThat(result.states()).containsEntry("LED_01", true)
                .containsEntry("LED_02", false)
                .containsEntry("LED_03", true);
        assertThat(result.mode()).isEqualTo("MANUAL");
    }

    @Test
    void parsesAutomaticLedFourStatusProducedByEsp32() {
        var result = parser.parseStatusPayload("Red:OFF | Yel:OFF | Gre:OFF | LED4:ON");
        assertThat(result.states()).containsEntry("LED_01", false)
                .containsEntry("LED_02", false)
                .containsEntry("LED_03", false)
                .containsEntry("LED_04", true);
        assertThat(result.mode()).isEqualTo("AUTO");
    }

    @Test
    void rejectsNanDhtValues() {
        assertThatThrownBy(() -> parser.parseSensorPayload("Temp:nanC | Hum:72.0% | Lux:123.4 lux | Light:SANG"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
