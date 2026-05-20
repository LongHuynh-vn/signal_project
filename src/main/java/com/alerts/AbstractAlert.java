package com.alerts;

/**
 * Shared immutable implementation for concrete alert types.
 */
public abstract class AbstractAlert implements Alert {
    private final String patientId;
    private final String condition;
    private final long timestamp;

    protected AbstractAlert(String patientId, String condition, long timestamp) {
        this.patientId = patientId;
        this.condition = condition;
        this.timestamp = timestamp;
    }

    @Override
    public String getPatientId() {
        return patientId;
    }

    @Override
    public String getCondition() {
        return condition;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Alert{patientId='" + patientId + "', condition='" + condition
                + "', timestamp=" + timestamp + "}";
    }
}
