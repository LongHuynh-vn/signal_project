package com.cardio_generator.outputs;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes generated patient data to text files grouped by measurement label.
 * Each label is mapped to a file inside the configured base directory, and new
 * output is appended as it is generated.
 */
public class FileOutputStrategy implements OutputStrategy {

    // Google Java Style Guide: member names use lowerCamelCase, so this field is
    // named baseDirectory instead of BaseDirectory.
    private final String baseDirectory;

    // Google Java Style Guide: non-constant fields use lowerCamelCase and should
    // remain private when they are implementation details.
    private final ConcurrentHashMap<String, String> fileMap = new ConcurrentHashMap<>();

    /**
     * Creates a file-based output strategy rooted at the provided directory.
     *
     * @param baseDirectory directory where label-specific output files are stored
     */
    public FileOutputStrategy(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    /**
     * Appends one generated measurement to the file associated with its label.
     *
     * @param patientId unique identifier of the patient that produced the data
     * @param timestamp measurement timestamp in milliseconds since the Unix epoch
     * @param label measurement label used to select the output file
     * @param data string representation of the generated measurement value
     */
    @Override
    public void output(int patientId, long timestamp, String label, String data) {
        try {
            // Create the directory
            Files.createDirectories(Paths.get(baseDirectory));
        } catch (IOException exception) {
            System.err.println("Error creating base directory: " + exception.getMessage());
            return;
        }

        // Google Java Style Guide: local variable names use lowerCamelCase.
        String filePath = fileMap.computeIfAbsent(label,
                ignoredLabel -> Paths.get(baseDirectory, label + ".txt").toString());

        // Write the data to the file
        // Google Java Style Guide: catch the specific IOException instead of the
        // overly broad Exception type.
        try (PrintWriter out = new PrintWriter(
                Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
            out.printf("Patient ID: %d, Timestamp: %d, Label: %s, Data: %s%n", patientId, timestamp, label, data);
        } catch (IOException exception) {
            System.err.println("Error writing to file " + filePath + ": " + exception.getMessage());
        }
    }
}