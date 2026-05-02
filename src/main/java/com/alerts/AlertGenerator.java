package com.alerts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;

/**
 * The {@code AlertGenerator} class is responsible for monitoring patient data
 * and generating alerts when certain predefined conditions are met. This class
 * relies on a {@link DataStorage} instance to access patient data and evaluate
 * it against specific health criteria.
 */
public class AlertGenerator {
    public static final String BP_TREND_INCREASE = "Blood pressure increasing trend";
    public static final String BP_TREND_DECREASE = "Blood pressure decreasing trend";
    public static final String BP_CRITICAL = "Critical blood pressure";
    public static final String LOW_SATURATION = "Low blood oxygen saturation";
    public static final String RAPID_SATURATION_DROP = "Rapid blood oxygen saturation drop";
    public static final String HYPOTENSIVE_HYPOXEMIA = "Hypotensive Hypoxemia Alert";
    public static final String ABNORMAL_ECG_PEAK = "Abnormal ECG peak";
    public static final String TRIGGERED_ALERT = "Triggered bedside alert";

    private static final long ALL_RECORDS_START = Long.MIN_VALUE;
    private static final long ALL_RECORDS_END = Long.MAX_VALUE;
    private static final long TEN_MINUTES_IN_MILLIS = 10 * 60 * 1000L;
    private static final double ECG_PEAK_OFFSET = 0.5;
    private static final int ECG_WINDOW_SIZE = 5;

    private final DataStorage dataStorage;
    private final List<Alert> triggeredAlerts;

    /**
     * Constructs an {@code AlertGenerator} with a specified {@code DataStorage}.
     * The {@code DataStorage} is used to retrieve patient data that this class
     * will monitor and evaluate.
     *
     * @param dataStorage the data storage system that provides access to patient
     *                    data
     */
    public AlertGenerator(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
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
     * Evaluates the specified patient's data to determine if any alert conditions
     * are met. If a condition is met, an alert is triggered via the
     * {@link #triggerAlert}
     * method. This method should define the specific conditions under which an
     * alert
     * will be triggered.
     *
     * @param patient the patient data to evaluate for alert conditions
     */
    public void evaluateData(Patient patient) {
        List<PatientRecord> records = patient.getRecords(ALL_RECORDS_START, ALL_RECORDS_END);
        records.sort(Comparator.comparingLong(PatientRecord::getTimestamp));

        evaluateBloodPressureTrends(records);
        evaluateBloodPressureThresholds(records);
        evaluateSaturation(records);
        evaluateHypotensiveHypoxemia(records);
        evaluateEcg(records);
        evaluateTriggeredAlerts(records);
    }

    /**
     * Triggers an alert for the monitoring system. This method can be extended to
     * notify medical staff, log the alert, or perform other actions. The method
     * currently assumes that the alert information is fully formed when passed as
     * an argument.
     *
     * @param alert the alert object containing details about the alert condition
     */
    void triggerAlert(Alert alert) {
        triggeredAlerts.add(alert);
    }

    private void evaluateBloodPressureTrends(List<PatientRecord> records) {
        evaluatePressureTrend(records, "SystolicPressure");
        evaluatePressureTrend(records, "DiastolicPressure");
    }

    private void evaluatePressureTrend(List<PatientRecord> records, String recordType) {
        List<PatientRecord> pressureRecords = filterByType(records, recordType);

        for (int index = 2; index < pressureRecords.size(); index++) {
            PatientRecord first = pressureRecords.get(index - 2);
            PatientRecord second = pressureRecords.get(index - 1);
            PatientRecord third = pressureRecords.get(index);

            double firstChange = second.getMeasurementValue() - first.getMeasurementValue();
            double secondChange = third.getMeasurementValue() - second.getMeasurementValue();

            if (firstChange > 10 && secondChange > 10) {
                triggerAlert(new Alert(String.valueOf(third.getPatientId()), BP_TREND_INCREASE,
                        third.getTimestamp()));
            } else if (firstChange < -10 && secondChange < -10) {
                triggerAlert(new Alert(String.valueOf(third.getPatientId()), BP_TREND_DECREASE,
                        third.getTimestamp()));
            }
        }
    }

    private void evaluateBloodPressureThresholds(List<PatientRecord> records) {
        for (PatientRecord record : records) {
            String type = record.getRecordType();
            double value = record.getMeasurementValue();
            boolean systolicCritical = "SystolicPressure".equals(type) && (value > 180 || value < 90);
            boolean diastolicCritical = "DiastolicPressure".equals(type) && (value > 120 || value < 60);

            if (systolicCritical || diastolicCritical) {
                triggerAlert(new Alert(String.valueOf(record.getPatientId()), BP_CRITICAL, record.getTimestamp()));
            }
        }
    }

    private void evaluateSaturation(List<PatientRecord> records) {
        List<PatientRecord> saturationRecords = filterByType(records, "Saturation");

        for (PatientRecord record : saturationRecords) {
            if (record.getMeasurementValue() < 92) {
                triggerAlert(new Alert(String.valueOf(record.getPatientId()), LOW_SATURATION, record.getTimestamp()));
            }
        }

        for (int olderIndex = 0; olderIndex < saturationRecords.size(); olderIndex++) {
            PatientRecord older = saturationRecords.get(olderIndex);
            for (int newerIndex = olderIndex + 1; newerIndex < saturationRecords.size(); newerIndex++) {
                PatientRecord newer = saturationRecords.get(newerIndex);
                long elapsed = newer.getTimestamp() - older.getTimestamp();
                if (elapsed > TEN_MINUTES_IN_MILLIS) {
                    break;
                }
                if (older.getMeasurementValue() - newer.getMeasurementValue() >= 5) {
                    triggerAlert(new Alert(String.valueOf(newer.getPatientId()), RAPID_SATURATION_DROP,
                            newer.getTimestamp()));
                    return;
                }
            }
        }
    }

    private void evaluateHypotensiveHypoxemia(List<PatientRecord> records) {
        PatientRecord latestSystolic = latestRecord(records, "SystolicPressure");
        PatientRecord latestSaturation = latestRecord(records, "Saturation");

        if (latestSystolic != null && latestSaturation != null
                && latestSystolic.getMeasurementValue() < 90
                && latestSaturation.getMeasurementValue() < 92) {
            long timestamp = Math.max(latestSystolic.getTimestamp(), latestSaturation.getTimestamp());
            triggerAlert(new Alert(String.valueOf(latestSystolic.getPatientId()), HYPOTENSIVE_HYPOXEMIA, timestamp));
        }
    }

    private void evaluateEcg(List<PatientRecord> records) {
        List<PatientRecord> ecgRecords = filterByType(records, "ECG");

        for (int index = 1; index < ecgRecords.size(); index++) {
            int windowStart = Math.max(0, index - ECG_WINDOW_SIZE);
            double sum = 0.0;
            for (int windowIndex = windowStart; windowIndex < index; windowIndex++) {
                sum += ecgRecords.get(windowIndex).getMeasurementValue();
            }

            double average = sum / (index - windowStart);
            PatientRecord current = ecgRecords.get(index);
            if (current.getMeasurementValue() > average + ECG_PEAK_OFFSET) {
                triggerAlert(new Alert(String.valueOf(current.getPatientId()), ABNORMAL_ECG_PEAK,
                        current.getTimestamp()));
            }
        }
    }

    private void evaluateTriggeredAlerts(List<PatientRecord> records) {
        for (PatientRecord record : filterByType(records, "Alert")) {
            if (record.getMeasurementValue() == 1.0) {
                triggerAlert(new Alert(String.valueOf(record.getPatientId()), TRIGGERED_ALERT, record.getTimestamp()));
            }
        }
    }

    private List<PatientRecord> filterByType(List<PatientRecord> records, String recordType) {
        List<PatientRecord> matchingRecords = new ArrayList<>();
        for (PatientRecord record : records) {
            if (recordType.equals(record.getRecordType())) {
                matchingRecords.add(record);
            }
        }
        return matchingRecords;
    }

    private PatientRecord latestRecord(List<PatientRecord> records, String recordType) {
        PatientRecord latest = null;
        for (PatientRecord record : records) {
            if (recordType.equals(record.getRecordType())
                    && (latest == null || record.getTimestamp() > latest.getTimestamp())) {
                latest = record;
            }
        }
        return latest;
    }
}
