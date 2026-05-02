package com.alerts;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.data_management.DataStorage;
import com.data_management.Patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertGeneratorTest {

    @Test
    void testIncreasingBloodPressureTrendTriggersAlert() {
        Patient patient = new Patient(1);
        patient.addRecord(100.0, "SystolicPressure", 1000L);
        patient.addRecord(112.0, "SystolicPressure", 2000L);
        patient.addRecord(125.0, "SystolicPressure", 3000L);

        AlertGenerator generator = new AlertGenerator(null);
        generator.evaluateData(patient);

        assertHasCondition(generator.getAlerts(), AlertGenerator.BP_TREND_INCREASE);
    }

    @Test
    void testDecreasingBloodPressureTrendTriggersAlert() {
        Patient patient = new Patient(1);
        patient.addRecord(130.0, "DiastolicPressure", 1000L);
        patient.addRecord(118.0, "DiastolicPressure", 2000L);
        patient.addRecord(105.0, "DiastolicPressure", 3000L);

        AlertGenerator generator = new AlertGenerator(null);
        generator.evaluateData(patient);

        assertHasCondition(generator.getAlerts(), AlertGenerator.BP_TREND_DECREASE);
    }

    @Test
    void testCriticalBloodPressureThresholdsTriggerAlerts() {
        Patient patient = new Patient(1);
        patient.addRecord(181.0, "SystolicPressure", 1000L);
        patient.addRecord(89.0, "SystolicPressure", 2000L);
        patient.addRecord(121.0, "DiastolicPressure", 3000L);
        patient.addRecord(59.0, "DiastolicPressure", 4000L);

        AlertGenerator generator = new AlertGenerator(null);
        generator.evaluateData(patient);

        long criticalCount = countCondition(generator.getAlerts(), AlertGenerator.BP_CRITICAL);
        assertEquals(4, criticalCount);
    }

    @Test
    void testLowSaturationTriggersAlert() {
        Patient patient = new Patient(1);
        patient.addRecord(91.0, "Saturation", 1000L);

        AlertGenerator generator = new AlertGenerator(null);
        generator.evaluateData(patient);

        assertHasCondition(generator.getAlerts(), AlertGenerator.LOW_SATURATION);
    }

    @Test
    void testRapidSaturationDropWithinTenMinutesTriggersAlert() {
        Patient patient = new Patient(1);
        patient.addRecord(98.0, "Saturation", 1000L);
        patient.addRecord(93.0, "Saturation", 1000L + 5 * 60 * 1000L);

        AlertGenerator generator = new AlertGenerator(null);
        generator.evaluateData(patient);

        assertHasCondition(generator.getAlerts(), AlertGenerator.RAPID_SATURATION_DROP);
    }

    @Test
    void testHypotensiveHypoxemiaUsesLatestReadings() {
        Patient patient = new Patient(1);
        patient.addRecord(85.0, "SystolicPressure", 1000L);
        patient.addRecord(91.0, "Saturation", 2000L);

        AlertGenerator generator = new AlertGenerator(null);
        generator.evaluateData(patient);

        assertHasCondition(generator.getAlerts(), AlertGenerator.HYPOTENSIVE_HYPOXEMIA);
    }

    @Test
    void testAbnormalEcgPeakTriggersAlert() {
        Patient patient = new Patient(1);
        patient.addRecord(0.1, "ECG", 1000L);
        patient.addRecord(0.1, "ECG", 2000L);
        patient.addRecord(0.1, "ECG", 3000L);
        patient.addRecord(0.8, "ECG", 4000L);

        AlertGenerator generator = new AlertGenerator(null);
        generator.evaluateData(patient);

        assertHasCondition(generator.getAlerts(), AlertGenerator.ABNORMAL_ECG_PEAK);
    }

    @Test
    void testTriggeredAlertCreatesAlertButResolvedDoesNot() {
        Patient patient = new Patient(1);
        patient.addRecord(1.0, "Alert", 1000L);
        patient.addRecord(0.0, "Alert", 2000L);

        AlertGenerator generator = new AlertGenerator(null);
        generator.evaluateData(patient);

        assertEquals(1, countCondition(generator.getAlerts(), AlertGenerator.TRIGGERED_ALERT));
    }

    @Test
    void testNormalValuesDoNotTriggerAlerts() {
        Patient patient = new Patient(1);
        patient.addRecord(120.0, "SystolicPressure", 1000L);
        patient.addRecord(80.0, "DiastolicPressure", 2000L);
        patient.addRecord(97.0, "Saturation", 3000L);
        patient.addRecord(0.1, "ECG", 4000L);
        patient.addRecord(0.2, "ECG", 5000L);
        patient.addRecord(0.0, "Alert", 6000L);

        AlertGenerator generator = new AlertGenerator(null);
        generator.evaluateData(patient);

        assertTrue(generator.getAlerts().isEmpty());
    }

    @Test
    void testClearAlertsRemovesGeneratedAlerts() {
        Patient patient = new Patient(1);
        patient.addRecord(91.0, "Saturation", 1000L);

        AlertGenerator generator = new AlertGenerator(null);
        generator.evaluateData(patient);
        assertFalse(generator.getAlerts().isEmpty());

        generator.clearAlerts();

        assertTrue(generator.getAlerts().isEmpty());
    }

    @Test
    void testEvaluateAllPatientsUsesConfiguredDataStorage() {
        DataStorage storage = new DataStorage();
        storage.addPatientData(1, 91.0, "Saturation", 1000L);

        AlertGenerator generator = new AlertGenerator(storage);
        generator.evaluateAllPatients();

        assertHasCondition(generator.getAlerts(), AlertGenerator.LOW_SATURATION);
    }

    private void assertHasCondition(List<Alert> alerts, String condition) {
        assertTrue(alerts.stream().anyMatch(alert -> condition.equals(alert.getCondition())));
    }

    private long countCondition(List<Alert> alerts, String condition) {
        return alerts.stream()
                .filter(alert -> condition.equals(alert.getCondition()))
                .count();
    }
}
