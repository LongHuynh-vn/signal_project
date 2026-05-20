package com.alerts;

import java.util.ArrayList;
import java.util.List;

import com.data_management.PatientRecord;

/**
 * Preserves existing bedside alert event handling.
 */
public class TriggeredAlertStrategy implements AlertStrategy {
    private static final String ALERT = "Alert";

    @Override
    public List<Alert> checkAlert(List<PatientRecord> records) {
        List<Alert> alerts = new ArrayList<>();
        for (PatientRecord record : records) {
            if (ALERT.equals(record.getRecordType()) && record.getMeasurementValue() == 1.0) {
                alerts.add(new GenericAlert(String.valueOf(record.getPatientId()),
                        AlertGenerator.TRIGGERED_ALERT, record.getTimestamp()));
            }
        }
        return alerts;
    }
}
