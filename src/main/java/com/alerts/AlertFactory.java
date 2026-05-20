package com.alerts;

/**
 * Factory method base type for creating alerts without exposing concrete alert
 * classes to monitoring algorithms.
 */
public abstract class AlertFactory {

    public abstract Alert createAlert(String patientId, String condition, long timestamp);
}
