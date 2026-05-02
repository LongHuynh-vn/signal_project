package com.alerts;

/**
 * Represents one alert generated for a patient.
 * An alert records the patient identifier, the condition that was detected, and
 * the time of the triggering measurement.
 */
public class Alert {
    private final String patientId;
    private final String condition;
    private final long timestamp;

    /**
     * Creates an alert with patient, condition, and timestamp details.
     *
     * @param patientId identifier of the patient related to this alert
     * @param condition human-readable condition that triggered the alert
     * @param timestamp time of the triggering measurement in milliseconds since epoch
     */
    public Alert(String patientId, String condition, long timestamp) {
        this.patientId = patientId;
        this.condition = condition;
        this.timestamp = timestamp;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getCondition() {
        return condition;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Alert{patientId='" + patientId + "', condition='" + condition
                + "', timestamp=" + timestamp + "}";
    }
}
