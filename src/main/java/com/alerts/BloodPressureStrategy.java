package com.alerts;

import java.util.ArrayList;
import java.util.List;

import com.data_management.PatientRecord;

/**
 * Detects blood pressure trend and threshold alerts.
 */
public class BloodPressureStrategy implements AlertStrategy {
    private static final String SYSTOLIC_PRESSURE = "SystolicPressure";
    private static final String DIASTOLIC_PRESSURE = "DiastolicPressure";

    private final AlertFactory alertFactory;

    public BloodPressureStrategy() {
        this(new BloodPressureAlertFactory());
    }

    public BloodPressureStrategy(AlertFactory alertFactory) {
        this.alertFactory = alertFactory;
    }

    @Override
    public List<Alert> checkAlert(List<PatientRecord> records) {
        List<Alert> alerts = new ArrayList<>();
        evaluatePressureTrend(records, SYSTOLIC_PRESSURE, alerts);
        evaluatePressureTrend(records, DIASTOLIC_PRESSURE, alerts);
        evaluatePressureThresholds(records, alerts);
        return alerts;
    }

    private void evaluatePressureTrend(List<PatientRecord> records, String recordType, List<Alert> alerts) {
        List<PatientRecord> pressureRecords = filterByType(records, recordType);

        for (int index = 2; index < pressureRecords.size(); index++) {
            PatientRecord first = pressureRecords.get(index - 2);
            PatientRecord second = pressureRecords.get(index - 1);
            PatientRecord third = pressureRecords.get(index);

            double firstChange = second.getMeasurementValue() - first.getMeasurementValue();
            double secondChange = third.getMeasurementValue() - second.getMeasurementValue();

            if (firstChange > 10 && secondChange > 10) {
                alerts.add(createAlert(third, AlertGenerator.BP_TREND_INCREASE));
            } else if (firstChange < -10 && secondChange < -10) {
                alerts.add(createAlert(third, AlertGenerator.BP_TREND_DECREASE));
            }
        }
    }

    private void evaluatePressureThresholds(List<PatientRecord> records, List<Alert> alerts) {
        for (PatientRecord record : records) {
            String type = record.getRecordType();
            double value = record.getMeasurementValue();
            boolean systolicCritical = SYSTOLIC_PRESSURE.equals(type) && (value > 180 || value < 90);
            boolean diastolicCritical = DIASTOLIC_PRESSURE.equals(type) && (value > 120 || value < 60);

            if (systolicCritical || diastolicCritical) {
                alerts.add(createAlert(record, AlertGenerator.BP_CRITICAL));
            }
        }
    }

    private Alert createAlert(PatientRecord record, String condition) {
        return alertFactory.createAlert(String.valueOf(record.getPatientId()), condition, record.getTimestamp());
    }

    private List<PatientRecord> filterByType(List<PatientRecord> records, String recordType) {
        List<PatientRecord> matchingRecords = new ArrayList<>();
        for (PatientRecord record : records) {
            if (recordType.equals(record.getRecordType())) {
                matchingRecords.add(record);
            }
        }
        return matchingRecords;
    }
}
