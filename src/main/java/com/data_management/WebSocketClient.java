package com.data_management;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.java_websocket.handshake.ServerHandshake;

/**
 * Connects to the simulator WebSocket output and stores incoming patient data in
 * {@link DataStorage}.
 */
public class WebSocketClient extends org.java_websocket.client.WebSocketClient implements DataReader {
    private static final long DEFAULT_CONNECTION_TIMEOUT_MILLIS = 5000L;

    private final long connectionTimeoutMillis;
    private final CountDownLatch closeLatch = new CountDownLatch(1);
    private volatile DataStorage dataStorage;

    public WebSocketClient(URI serverUri) {
        this(serverUri, DEFAULT_CONNECTION_TIMEOUT_MILLIS);
    }

    public WebSocketClient(URI serverUri, long connectionTimeoutMillis) {
        super(serverUri);
        if (connectionTimeoutMillis <= 0L) {
            throw new IllegalArgumentException("Connection timeout must be positive.");
        }
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    /**
     * Establishes the WebSocket connection. Message handling happens asynchronously
     * in {@link #onMessage(String)} after the connection is open.
     */
    @Override
    public void readData(DataStorage dataStorage) throws IOException {
        if (dataStorage == null) {
            throw new IOException("DataStorage is required for WebSocket streaming.");
        }
        this.dataStorage = dataStorage;

        try {
            boolean connected = connectBlocking(connectionTimeoutMillis, TimeUnit.MILLISECONDS);
            if (!connected) {
                throw new IOException("Timed out connecting to WebSocket server: " + getURI());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while connecting to WebSocket server: " + getURI(), exception);
        } catch (RuntimeException exception) {
            throw new IOException("Unable to connect to WebSocket server: " + getURI(), exception);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to WebSocket data stream: " + getURI());
    }

    @Override
    public void onMessage(String message) {
        DataStorage activeStorage = dataStorage;
        if (activeStorage == null) {
            System.err.println("Skipping WebSocket message because no DataStorage is configured.");
            return;
        }

        try {
            ParsedRecord parsedRecord = parseMessage(message);
            activeStorage.addPatientData(parsedRecord.patientId, parsedRecord.measurementValue,
                    parsedRecord.recordType, parsedRecord.timestamp);
        } catch (IllegalArgumentException exception) {
            System.err.println("Skipping malformed WebSocket message: " + exception.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.err.println("WebSocket data stream closed. Code: " + code + ", reason: " + reason
                + ", remote: " + remote);
        closeLatch.countDown();
    }

    @Override
    public void onError(Exception exception) {
        System.err.println("WebSocket data stream error: " + exception.getMessage());
    }

    public void waitUntilClosed() throws InterruptedException {
        closeLatch.await();
    }

    public boolean waitUntilClosed(long timeout, TimeUnit unit) throws InterruptedException {
        return closeLatch.await(timeout, unit);
    }

    @Override
    public void close() {
        super.close();
        if (isClosed()) {
            closeLatch.countDown();
        }
    }

    static ParsedRecord parseMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("message is empty");
        }

        String[] parts = message.split(",", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("expected patientId,timestamp,label,data");
        }

        int patientId = parsePatientId(parts[0].trim());
        long timestamp = parseTimestamp(parts[1].trim());
        String recordType = parts[2].trim();
        if (recordType.isEmpty()) {
            throw new IllegalArgumentException("record label is empty");
        }
        double measurementValue = parseMeasurementValue(parts[3].trim());

        return new ParsedRecord(patientId, timestamp, recordType, measurementValue);
    }

    private static int parsePatientId(String rawPatientId) {
        try {
            int patientId = Integer.parseInt(rawPatientId);
            if (patientId <= 0) {
                throw new IllegalArgumentException("patient ID must be positive");
            }
            return patientId;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("patient ID is not a number", exception);
        }
    }

    private static long parseTimestamp(String rawTimestamp) {
        try {
            long timestamp = Long.parseLong(rawTimestamp);
            if (timestamp < 0L) {
                throw new IllegalArgumentException("timestamp must be non-negative");
            }
            return timestamp;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("timestamp is not a number", exception);
        }
    }

    private static double parseMeasurementValue(String rawValue) {
        try {
            double value;
            if (rawValue.endsWith("%")) {
                value = Double.parseDouble(rawValue.substring(0, rawValue.length() - 1));
            } else if ("triggered".equalsIgnoreCase(rawValue)) {
                value = 1.0;
            } else if ("resolved".equalsIgnoreCase(rawValue)) {
                value = 0.0;
            } else {
                value = Double.parseDouble(rawValue);
            }
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("measurement value must be finite");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("measurement value is not valid", exception);
        }
    }

    static final class ParsedRecord {
        final int patientId;
        final long timestamp;
        final String recordType;
        final double measurementValue;

        private ParsedRecord(int patientId, long timestamp, String recordType, double measurementValue) {
            this.patientId = patientId;
            this.timestamp = timestamp;
            this.recordType = recordType;
            this.measurementValue = measurementValue;
        }
    }
}
