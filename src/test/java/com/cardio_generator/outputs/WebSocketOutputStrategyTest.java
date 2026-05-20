package com.cardio_generator.outputs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebSocketOutputStrategyTest {

    @Test
    void testFormatMessageUsesCsvWireFormat() {
        String message = WebSocketOutputStrategy.formatMessage(1, 1000L, "ECG", "0.25");

        assertEquals("1,1000,ECG,0.25", message);
    }

    @Test
    void testFormatMessageKeepsAlertValues() {
        String message = WebSocketOutputStrategy.formatMessage(2, 2000L, "Alert", "triggered");

        assertEquals("2,2000,Alert,triggered", message);
    }
}
