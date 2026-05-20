package com.alerts;

/**
 * Alert generated from blood pressure monitoring.
 */
public class BloodPressureAlert extends AbstractAlert {

    public BloodPressureAlert(String patientId, String condition, long timestamp) {
        super(patientId, condition, timestamp);
    }
}
