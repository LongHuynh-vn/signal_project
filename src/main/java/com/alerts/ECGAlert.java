package com.alerts;

/**
 * Alert generated from ECG or heart-rate monitoring.
 */
public class ECGAlert extends AbstractAlert {

    public ECGAlert(String patientId, String condition, long timestamp) {
        super(patientId, condition, timestamp);
    }
}
