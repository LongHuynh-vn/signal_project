package com.alerts;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.data_management.PatientRecord;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertStrategyTest {

    @Test
    void testBloodPressureStrategyCreatesBloodPressureAlerts() {
        List<PatientRecord> records = List.of(
                record(1, 100.0, "SystolicPressure", 1000L),
                record(1, 112.0, "SystolicPressure", 2000L),
                record(1, 125.0, "SystolicPressure", 3000L),
                record(1, 181.0, "SystolicPressure", 4000L));

        List<Alert> alerts = new BloodPressureStrategy().checkAlert(records);

        assertHasCondition(alerts, AlertGenerator.BP_TREND_INCREASE);
        assertHasCondition(alerts, AlertGenerator.BP_CRITICAL);
        assertTrue(alerts.stream().allMatch(alert -> alert instanceof BloodPressureAlert));
    }

    @Test
    void testOxygenSaturationStrategyCreatesOxygenAlerts() {
        List<PatientRecord> records = List.of(
                record(1, 85.0, "SystolicPressure", 1000L),
                record(1, 98.0, "Saturation", 2000L),
                record(1, 91.0, "Saturation", 3000L));

        List<Alert> alerts = new OxygenSaturationStrategy().checkAlert(records);

        assertHasCondition(alerts, AlertGenerator.LOW_SATURATION);
        assertHasCondition(alerts, AlertGenerator.RAPID_SATURATION_DROP);
        assertHasCondition(alerts, AlertGenerator.HYPOTENSIVE_HYPOXEMIA);
        assertTrue(alerts.stream().allMatch(alert -> alert instanceof BloodOxygenAlert));
    }

    @Test
    void testHeartRateStrategyCreatesEcgAlertsForEcgAndHeartRate() {
        List<PatientRecord> records = List.of(
                record(1, 0.1, "ECG", 1000L),
                record(1, 0.1, "ECG", 2000L),
                record(1, 0.8, "ECG", 3000L),
                record(1, 49.0, "HeartRate", 4000L),
                record(1, 121.0, "HeartRate", 5000L));

        List<Alert> alerts = new HeartRateStrategy().checkAlert(records);

        assertHasCondition(alerts, AlertGenerator.ABNORMAL_ECG_PEAK);
        assertHasCondition(alerts, AlertGenerator.ABNORMAL_HEART_RATE);
        assertTrue(alerts.stream().allMatch(alert -> alert instanceof ECGAlert));
    }

    @Test
    void testTriggeredAlertStrategyPreservesBedsideAlertBehavior() {
        List<PatientRecord> records = List.of(
                record(1, 1.0, "Alert", 1000L),
                record(1, 0.0, "Alert", 2000L));

        List<Alert> alerts = new TriggeredAlertStrategy().checkAlert(records);

        assertHasCondition(alerts, AlertGenerator.TRIGGERED_ALERT);
        assertInstanceOf(GenericAlert.class, alerts.get(0));
    }

    private PatientRecord record(int patientId, double measurementValue, String recordType, long timestamp) {
        return new PatientRecord(patientId, measurementValue, recordType, timestamp);
    }

    private void assertHasCondition(List<Alert> alerts, String condition) {
        assertTrue(alerts.stream().anyMatch(alert -> condition.equals(alert.getCondition())));
    }
}
