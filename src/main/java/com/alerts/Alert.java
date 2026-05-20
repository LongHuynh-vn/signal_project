package com.alerts;

/**
 * Represents one alert generated for a patient.
 * Implementations record the patient identifier, the condition that was
 * detected, and the time of the triggering measurement.
 */
public interface Alert {

    String getPatientId();

    String getCondition();

    long getTimestamp();

    default AlertPriority getPriority() {
        return AlertPriority.NORMAL;
    }

    default boolean isRepeated() {
        return false;
    }

    default long getRepeatIntervalMillis() {
        return 0L;
    }
}
