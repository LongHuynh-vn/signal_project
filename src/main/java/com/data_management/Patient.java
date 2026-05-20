package com.data_management;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a patient and manages their medical records.
 * This class stores patient-specific data, allowing for the addition and
 * retrieval
 * of medical records based on specified criteria.
 */
public class Patient {
    private final int patientId;
    private final List<PatientRecord> patientRecords;

    /**
     * Constructs a new Patient with a specified ID.
     * Initializes an empty list of patient records.
     *
     * @param patientId the unique identifier for the patient
     */
    public Patient(int patientId) {
        this.patientId = patientId;
        this.patientRecords = new ArrayList<>();
    }

    /**
     * Returns this patient's identifier.
     *
     * @return the patient identifier
     */
    public int getPatientId() {
        return patientId;
    }

    /**
     * Adds a new record to this patient's list of medical records.
     * Exact duplicates are ignored so a repeated real-time message does not create
     * duplicate patient history entries.
     *
     * @param measurementValue the measurement value to store in the record
     * @param recordType       the type of record, e.g., "HeartRate",
     *                         "BloodPressure"
     * @param timestamp        the time at which the measurement was taken, in
     *                         milliseconds since UNIX epoch
     * @return {@code true} if a new record was stored, otherwise {@code false}
     */
    public synchronized boolean addRecord(double measurementValue, String recordType, long timestamp) {
        if (containsRecord(measurementValue, recordType, timestamp)) {
            return false;
        }
        PatientRecord record = new PatientRecord(this.patientId, measurementValue, recordType, timestamp);
        this.patientRecords.add(record);
        return true;
    }

    /**
     * Retrieves a list of PatientRecord objects for this patient that fall within a
     * specified time range.
     * The method filters records based on the start and end times provided.
     *
     * @param startTime the start of the time range, in milliseconds since UNIX
     *                  epoch
     * @param endTime   the end of the time range, in milliseconds since UNIX epoch
     * @return a list of PatientRecord objects that fall within the specified time
     *         range
     */
    public synchronized List<PatientRecord> getRecords(long startTime, long endTime) {
        List<PatientRecord> matchingRecords = new ArrayList<>();
        if (startTime > endTime) {
            return matchingRecords;
        }

        for (PatientRecord patientRecord : patientRecords) {
            long timestamp = patientRecord.getTimestamp();
            if (timestamp >= startTime && timestamp <= endTime) {
                matchingRecords.add(patientRecord);
            }
        }

        return matchingRecords;
    }

    private boolean containsRecord(double measurementValue, String recordType, long timestamp) {
        for (PatientRecord patientRecord : patientRecords) {
            boolean sameTimestamp = patientRecord.getTimestamp() == timestamp;
            boolean sameType = patientRecord.getRecordType().equals(recordType);
            boolean sameValue = Double.compare(patientRecord.getMeasurementValue(), measurementValue) == 0;
            if (sameTimestamp && sameType && sameValue) {
                return true;
            }
        }
        return false;
    }
}
