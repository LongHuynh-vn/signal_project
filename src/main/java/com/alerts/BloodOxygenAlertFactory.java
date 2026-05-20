package com.alerts;

/**
 * Creates blood oxygen alerts.
 */
public class BloodOxygenAlertFactory extends AlertFactory {

    @Override
    public Alert createAlert(String patientId, String condition, long timestamp) {
        return new BloodOxygenAlert(patientId, condition, timestamp);
    }
}
