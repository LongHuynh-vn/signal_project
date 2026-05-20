package com.alerts;

import java.util.ArrayList;
import java.util.List;

import com.data_management.PatientRecord;

/**
 * Detects ECG rhythm and heart-rate alerts.
 */
public class HeartRateStrategy implements AlertStrategy {
    private static final String ECG = "ECG";
    private static final String HEART_RATE = "HeartRate";
    private static final double ECG_PEAK_OFFSET = 0.5;
    private static final int ECG_WINDOW_SIZE = 5;
    private static final double LOW_HEART_RATE_THRESHOLD = 50.0;
    private static final double HIGH_HEART_RATE_THRESHOLD = 120.0;

    private final AlertFactory alertFactory;

    public HeartRateStrategy() {
        this(new ECGAlertFactory());
    }

    public HeartRateStrategy(AlertFactory alertFactory) {
        this.alertFactory = alertFactory;
    }

    @Override
    public List<Alert> checkAlert(List<PatientRecord> records) {
        List<Alert> alerts = new ArrayList<>();
        evaluateEcg(records, alerts);
        evaluateHeartRate(records, alerts);
        return alerts;
    }

    private void evaluateEcg(List<PatientRecord> records, List<Alert> alerts) {
        List<PatientRecord> ecgRecords = filterByType(records, ECG);

        for (int index = 1; index < ecgRecords.size(); index++) {
            int windowStart = Math.max(0, index - ECG_WINDOW_SIZE);
            double sum = 0.0;
            for (int windowIndex = windowStart; windowIndex < index; windowIndex++) {
                sum += ecgRecords.get(windowIndex).getMeasurementValue();
            }

            double average = sum / (index - windowStart);
            PatientRecord current = ecgRecords.get(index);
            if (current.getMeasurementValue() > average + ECG_PEAK_OFFSET) {
                alerts.add(createAlert(current, AlertGenerator.ABNORMAL_ECG_PEAK));
            }
        }
    }

    private void evaluateHeartRate(List<PatientRecord> records, List<Alert> alerts) {
        for (PatientRecord record : filterByType(records, HEART_RATE)) {
            double value = record.getMeasurementValue();
            if (value < LOW_HEART_RATE_THRESHOLD || value > HIGH_HEART_RATE_THRESHOLD) {
                alerts.add(createAlert(record, AlertGenerator.ABNORMAL_HEART_RATE));
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
