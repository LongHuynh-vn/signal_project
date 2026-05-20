package com.alerts;

/**
 * Alert used for existing non-clinical alert events such as bedside triggers.
 */
public class GenericAlert extends AbstractAlert {

    public GenericAlert(String patientId, String condition, long timestamp) {
        super(patientId, condition, timestamp);
    }
}
