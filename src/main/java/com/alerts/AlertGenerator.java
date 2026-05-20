package com.alerts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;

/**
 * Monitors patient data by delegating alert checks to configured strategies.
 */
public class AlertGenerator {
    public static final String BP_TREND_INCREASE = "Blood pressure increasing trend";
    public static final String BP_TREND_DECREASE = "Blood pressure decreasing trend";
    public static final String BP_CRITICAL = "Critical blood pressure";
    public static final String LOW_SATURATION = "Low blood oxygen saturation";
    public static final String RAPID_SATURATION_DROP = "Rapid blood oxygen saturation drop";
    public static final String HYPOTENSIVE_HYPOXEMIA = "Hypotensive Hypoxemia Alert";
    public static final String ABNORMAL_ECG_PEAK = "Abnormal ECG peak";
    public static final String ABNORMAL_HEART_RATE = "Abnormal heart rate";
    public static final String TRIGGERED_ALERT = "Triggered bedside alert";

    private static final long ALL_RECORDS_START = Long.MIN_VALUE;
    private static final long ALL_RECORDS_END = Long.MAX_VALUE;
    private static final long REPEATED_ALERT_INTERVAL_MILLIS = 10 * 60 * 1000L;
    private static final Set<String> HIGH_PRIORITY_CONDITIONS = new HashSet<>(Arrays.asList(
            BP_CRITICAL,
            LOW_SATURATION,
            RAPID_SATURATION_DROP,
            HYPOTENSIVE_HYPOXEMIA,
            ABNORMAL_ECG_PEAK,
            ABNORMAL_HEART_RATE));

    private final DataStorage dataStorage;
    private final List<AlertStrategy> strategies;
    private final List<Alert> triggeredAlerts;

    /**
     * Constructs an alert generator with the default monitoring strategies.
     *
     * @param dataStorage storage that provides patient data
     */
    public AlertGenerator(DataStorage dataStorage) {
        this(dataStorage, createDefaultStrategies());
    }

    /**
     * Constructs an alert generator with explicit monitoring strategies.
     *
     * @param dataStorage storage that provides patient data
     * @param strategies strategies used to check patient records
     */
    public AlertGenerator(DataStorage dataStorage, List<AlertStrategy> strategies) {
        this.dataStorage = dataStorage;
        this.strategies = new ArrayList<>(strategies);
        this.triggeredAlerts = new ArrayList<>();
    }

    /**
     * Returns a copy of the alerts generated so far.
     *
     * @return generated alerts
     */
    public List<Alert> getAlerts() {
        return new ArrayList<>(triggeredAlerts);
    }

    /**
     * Clears all previously generated alerts.
     */
    public void clearAlerts() {
        triggeredAlerts.clear();
    }

    /**
     * Evaluates every patient currently stored in the configured data storage.
     */
    public void evaluateAllPatients() {
        if (dataStorage == null) {
            return;
        }

        for (Patient patient : dataStorage.getAllPatients()) {
            evaluateData(patient);
        }
    }

    /**
     * Evaluates one patient's data with the configured alert strategies.
     *
     * @param patient patient data to evaluate
     */
    public void evaluateData(Patient patient) {
        if (patient == null) {
            return;
        }

        List<PatientRecord> records = patient.getRecords(ALL_RECORDS_START, ALL_RECORDS_END);
        records.sort(Comparator.comparingLong(PatientRecord::getTimestamp));

        for (AlertStrategy strategy : strategies) {
            for (Alert alert : strategy.checkAlert(records)) {
                triggerAlert(alert);
            }
        }
    }

    /**
     * Triggers an alert and decorates it with runtime metadata when applicable.
     *
     * @param alert alert produced by a monitoring strategy
     */
    void triggerAlert(Alert alert) {
        triggeredAlerts.add(decorateAlert(alert));
    }

    private Alert decorateAlert(Alert alert) {
        Alert decoratedAlert = alert;
        if (HIGH_PRIORITY_CONDITIONS.contains(alert.getCondition())) {
            decoratedAlert = new PriorityAlertDecorator(decoratedAlert);
        }

        long repeatInterval = findRepeatInterval(alert);
        if (repeatInterval >= 0L) {
            decoratedAlert = new RepeatedAlertDecorator(decoratedAlert, repeatInterval);
        }

        return decoratedAlert;
    }

    private long findRepeatInterval(Alert alert) {
        for (Alert existingAlert : triggeredAlerts) {
            boolean samePatient = existingAlert.getPatientId().equals(alert.getPatientId());
            boolean sameCondition = existingAlert.getCondition().equals(alert.getCondition());
            long interval = alert.getTimestamp() - existingAlert.getTimestamp();
            if (samePatient && sameCondition && interval >= 0L && interval <= REPEATED_ALERT_INTERVAL_MILLIS) {
                return interval;
            }
        }
        return -1L;
    }

    private static List<AlertStrategy> createDefaultStrategies() {
        return Arrays.asList(
                new BloodPressureStrategy(),
                new OxygenSaturationStrategy(),
                new HeartRateStrategy(),
                new TriggeredAlertStrategy());
    }
}
