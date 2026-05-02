package com.cardio_generator.generators;

import java.util.Random;

import com.cardio_generator.outputs.OutputStrategy;

/**
 * Generates simulated blood oxygen saturation measurements for patients.
 * Values begin near a healthy baseline and are then varied slightly while
 * remaining within a realistic range.
 */
public class BloodSaturationDataGenerator implements PatientDataGenerator {
    private static final Random random = new Random();
    private final int[] lastSaturationValues;

    /**
     * Creates a saturation generator and initializes a starting value for each
     * patient.
     *
     * @param patientCount total number of simulated patients; valid patient IDs are
     *                     expected in the range 1..patientCount
     */
    public BloodSaturationDataGenerator(int patientCount) {
        lastSaturationValues = new int[patientCount + 1];

        // Initialize with baseline saturation values for each patient
        for (int i = 1; i <= patientCount; i++) {
            lastSaturationValues[i] = 95 + random.nextInt(6); // Initializes with a value between 95 and 100
        }
    }

    /**
     * Generates the next blood saturation measurement for a patient and forwards it
     * to the configured output strategy.
     *
     * @param patientId identifier of the patient receiving the generated reading
     * @param outputStrategy destination that receives the generated saturation
     *                       value
     */
    @Override
    public void generate(int patientId, OutputStrategy outputStrategy) {
        try {
            // Simulate blood saturation values
            int variation = random.nextInt(3) - 1; // -1, 0, or 1 to simulate small fluctuations
            int newSaturationValue = lastSaturationValues[patientId] + variation;

            // Ensure the saturation stays within a realistic and healthy range
            newSaturationValue = Math.min(Math.max(newSaturationValue, 90), 100);
            lastSaturationValues[patientId] = newSaturationValue;
            outputStrategy.output(patientId, System.currentTimeMillis(), "Saturation",
                    Double.toString(newSaturationValue) + "%");
        } catch (RuntimeException exception) {
            System.err.println("An error occurred while generating blood saturation data for patient " + patientId);
            System.err.println(exception.getMessage());
        }
    }
}
