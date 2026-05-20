package com.data_management;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.alerts.Alert;
import com.alerts.AlertGenerator;

/**
 * Manages storage and retrieval of patient data within a healthcare monitoring
 * system.
 * This class serves as a repository for all patient records, organized by
 * patient IDs.
 */
public class DataStorage {
    private static final DataStorage INSTANCE = new DataStorage();

    private final Map<Integer, Patient> patientMap; // Stores patient objects indexed by their unique patient ID.

    /**
     * Constructs the singleton instance of DataStorage.
     */
    private DataStorage() {
        this.patientMap = new ConcurrentHashMap<>();
    }

    /**
     * Returns the single shared DataStorage instance.
     *
     * @return singleton DataStorage instance
     */
    public static DataStorage getInstance() {
        return INSTANCE;
    }

    /**
     * Removes all stored patients and records.
     */
    public void clear() {
        patientMap.clear();
    }

    /**
     * Adds or updates patient data in the storage.
     * If the patient does not exist, a new Patient object is created and added to
     * the storage.
     * Otherwise, the new data is added to the existing patient's records.
     *
     * @param patientId        the unique identifier of the patient
     * @param measurementValue the value of the health metric being recorded
     * @param recordType       the type of record, e.g., "HeartRate",
     *                         "BloodPressure"
     * @param timestamp        the time at which the measurement was taken, in
     *                         milliseconds since the Unix epoch
     */
    public void addPatientData(int patientId, double measurementValue, String recordType, long timestamp) {
        Patient patient = patientMap.computeIfAbsent(patientId, Patient::new);
        patient.addRecord(measurementValue, recordType, timestamp);
    }

    /**
     * Retrieves a list of PatientRecord objects for a specific patient, filtered by
     * a time range.
     *
     * @param patientId the unique identifier of the patient whose records are to be
     *                  retrieved
     * @param startTime the start of the time range, in milliseconds since the Unix
     *                  epoch
     * @param endTime   the end of the time range, in milliseconds since the Unix
     *                  epoch
     * @return a list of PatientRecord objects that fall within the specified time
     *         range
     */
    public List<PatientRecord> getRecords(int patientId, long startTime, long endTime) {
        Patient patient = patientMap.get(patientId);
        if (patient != null) {
            return patient.getRecords(startTime, endTime);
        }
        return new ArrayList<>(); // return an empty list if no patient is found
    }

    /**
     * Retrieves a collection of all patients stored in the data storage.
     *
     * @return a list of all patients
     */
    public List<Patient> getAllPatients() {
        return new ArrayList<>(patientMap.values());
    }

    /**
     * The main method for the DataStorage class.
     * Initializes the system, reads data into storage, and continuously monitors
     * and evaluates patient data.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        DataStorage storage = DataStorage.getInstance();
        storage.clear();

        if (args.length > 0) {
            DataReader reader = null;
            try {
                reader = createReader(args[0]);
                reader.readData(storage);
                if (reader instanceof WebSocketClient) {
                    System.out.println("Streaming WebSocket data. Stop the server or interrupt this process to exit.");
                    ((WebSocketClient) reader).waitUntilClosed();
                }
            } catch (IOException exception) {
                System.err.println("Unable to read patient data: " + exception.getMessage());
                return;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while waiting for WebSocket data.");
                return;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException exception) {
                        System.err.println("Unable to close data reader: " + exception.getMessage());
                    }
                }
            }
        } else {
            System.out.println("No data directory provided. Running with empty storage.");
        }

        // Example of using DataStorage to retrieve and print records for a patient
        List<PatientRecord> records = storage.getRecords(1, 1700000000000L, 1800000000000L);
        for (PatientRecord record : records) {
            System.out.println("Record for Patient ID: " + record.getPatientId() +
                    ", Type: " + record.getRecordType() +
                    ", Data: " + record.getMeasurementValue() +
                    ", Timestamp: " + record.getTimestamp());
        }

        // Initialize the AlertGenerator with the storage
        AlertGenerator alertGenerator = new AlertGenerator(storage);

        alertGenerator.evaluateAllPatients();

        for (Alert alert : alertGenerator.getAlerts()) {
            System.out.println(alert);
        }
    }

    private static DataReader createReader(String source) throws IOException {
        if (source.startsWith("websocket:")) {
            String portText = source.substring("websocket:".length());
            try {
                int port = Integer.parseInt(portText);
                return new WebSocketClient(new URI("ws://localhost:" + port));
            } catch (NumberFormatException | URISyntaxException exception) {
                throw new IOException("Invalid WebSocket source: " + source, exception);
            }
        }
        if (source.startsWith("ws://") || source.startsWith("wss://")) {
            try {
                return new WebSocketClient(new URI(source));
            } catch (URISyntaxException exception) {
                throw new IOException("Invalid WebSocket URI: " + source, exception);
            }
        }
        return new FileDataReader(source);
    }
}
