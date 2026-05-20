package com.alerts;

/**
 * Alert generated from blood oxygen monitoring.
 */
public class BloodOxygenAlert extends AbstractAlert {

    public BloodOxygenAlert(String patientId, String condition, long timestamp) {
        super(patientId, condition, timestamp);
    }
}
