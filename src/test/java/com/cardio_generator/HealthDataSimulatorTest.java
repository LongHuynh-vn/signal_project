package com.cardio_generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class HealthDataSimulatorTest {

    @Test
    void testGetInstanceReturnsSameHealthDataSimulator() {
        assertSame(HealthDataSimulator.getInstance(), HealthDataSimulator.getInstance());
    }
}
