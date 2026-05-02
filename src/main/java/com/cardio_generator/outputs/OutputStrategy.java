package com.cardio_generator.outputs;

/**
 * Defines how generated patient data is delivered to a consumer.
 * Implementations may write to the console, files, sockets, or other transport
 * mechanisms.
 */
public interface OutputStrategy {

    /**
     * Emits one generated measurement to the configured output destination.
     *
     * @param patientId unique identifier of the patient that produced the data
     * @param timestamp measurement timestamp in milliseconds since the Unix epoch
     * @param label measurement category, such as ECG or Saturation
     * @param data formatted measurement value that should be delivered
     */
    void output(int patientId, long timestamp, String label, String data);
}
