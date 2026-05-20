package com.alerts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AlertFactoryTest {

    @Test
    void testBloodPressureFactoryCreatesBloodPressureAlert() {
        Alert alert = new BloodPressureAlertFactory().createAlert("1", AlertGenerator.BP_CRITICAL, 1000L);

        assertInstanceOf(BloodPressureAlert.class, alert);
        assertAlertFields(alert, "1", AlertGenerator.BP_CRITICAL, 1000L);
    }

    @Test
    void testBloodOxygenFactoryCreatesBloodOxygenAlert() {
        Alert alert = new BloodOxygenAlertFactory().createAlert("2", AlertGenerator.LOW_SATURATION, 2000L);

        assertInstanceOf(BloodOxygenAlert.class, alert);
        assertAlertFields(alert, "2", AlertGenerator.LOW_SATURATION, 2000L);
    }

    @Test
    void testEcgFactoryCreatesEcgAlert() {
        Alert alert = new ECGAlertFactory().createAlert("3", AlertGenerator.ABNORMAL_ECG_PEAK, 3000L);

        assertInstanceOf(ECGAlert.class, alert);
        assertAlertFields(alert, "3", AlertGenerator.ABNORMAL_ECG_PEAK, 3000L);
    }

    private void assertAlertFields(Alert alert, String patientId, String condition, long timestamp) {
        assertEquals(patientId, alert.getPatientId());
        assertEquals(condition, alert.getCondition());
        assertEquals(timestamp, alert.getTimestamp());
    }
}
