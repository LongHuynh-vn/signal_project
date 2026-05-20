package com.alerts;

import java.util.ArrayList;
import java.util.List;

import com.data_management.PatientRecord;

/**
 * Detects oxygen saturation alerts and related hypotensive hypoxemia alerts.
 */
public class OxygenSaturationStrategy implements AlertStrategy {
    private static final String SATURATION = "Saturation";
    private static final String SYSTOLIC_PRESSURE = "SystolicPressure";
    private static final long TEN_MINUTES_IN_MILLIS = 10 * 60 * 1000L;

    private final AlertFactory alertFactory;

    public OxygenSaturationStrategy() {
        this(new BloodOxygenAlertFactory());
    }

    public OxygenSaturationStrategy(AlertFactory alertFactory) {
        this.alertFactory = alertFactory;
    }

    @Override
    public List<Alert> checkAlert(List<PatientRecord> records) {
        List<Alert> alerts = new ArrayList<>();
        List<PatientRecord> saturationRecords = filterByType(records, SATURATION);

        for (PatientRecord record : saturationRecords) {
            if (record.getMeasurementValue() < 92) {
                alerts.add(createAlert(record, AlertGenerator.LOW_SATURATION));
            }
        }

        addRapidSaturationDropAlert(saturationRecords, alerts);
        addHypotensiveHypoxemiaAlert(records, alerts);
        return alerts;
    }

    private void addRapidSaturationDropAlert(List<PatientRecord> saturationRecords, List<Alert> alerts) {
        for (int olderIndex = 0; olderIndex < saturationRecords.size(); olderIndex++) {
            PatientRecord older = saturationRecords.get(olderIndex);
            for (int newerIndex = olderIndex + 1; newerIndex < saturationRecords.size(); newerIndex++) {
                PatientRecord newer = saturationRecords.get(newerIndex);
                long elapsed = newer.getTimestamp() - older.getTimestamp();
                if (elapsed > TEN_MINUTES_IN_MILLIS) {
                    break;
                }
                if (older.getMeasurementValue() - newer.getMeasurementValue() >= 5) {
                    alerts.add(createAlert(newer, AlertGenerator.RAPID_SATURATION_DROP));
                    return;
                }
            }
        }
    }

    private void addHypotensiveHypoxemiaAlert(List<PatientRecord> records, List<Alert> alerts) {
        PatientRecord latestSystolic = latestRecord(records, SYSTOLIC_PRESSURE);
        PatientRecord latestSaturation = latestRecord(records, SATURATION);

        if (latestSystolic != null && latestSaturation != null
                && latestSystolic.getMeasurementValue() < 90
                && latestSaturation.getMeasurementValue() < 92) {
            long timestamp = Math.max(latestSystolic.getTimestamp(), latestSaturation.getTimestamp());
            alerts.add(alertFactory.createAlert(String.valueOf(latestSystolic.getPatientId()),
                    AlertGenerator.HYPOTENSIVE_HYPOXEMIA, timestamp));
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

    private PatientRecord latestRecord(List<PatientRecord> records, String recordType) {
        PatientRecord latest = null;
        for (PatientRecord record : records) {
            if (recordType.equals(record.getRecordType())
                    && (latest == null || record.getTimestamp() > latest.getTimestamp())) {
                latest = record;
            }
        }
        return latest;
    }
}
