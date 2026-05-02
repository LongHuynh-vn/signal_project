package com.cardio_generator.generators;

import java.util.Random;

import com.cardio_generator.outputs.OutputStrategy;

/**
 * Simulates alert button activity for patients in the monitoring system.
 * An alert alternates between triggered and resolved states based on simple
 * probability rules to mimic intermittent clinical alarms.
 */
public class AlertGenerator implements PatientDataGenerator {

    // Google Java Style Guide: implementation details should be private rather
    // than public when they are not part of the class API.
    private static final Random randomGenerator = new Random();

    // Google Java Style Guide: non-constant field names use lowerCamelCase.
    private final boolean[] alertStates; // false = resolved, true = pressed

    /**
     * Creates an alert generator with per-patient alert state tracking.
     *
     * @param patientCount total number of simulated patients; valid patient IDs are
     *                     expected in the range 1..patientCount
     */
    public AlertGenerator(int patientCount) {
        alertStates = new boolean[patientCount + 1];
    }

    /**
     * Generates the next alert event for a patient and forwards it to the selected
     * output strategy.
     *
     * @param patientId identifier of the patient whose alert state is being updated
     * @param outputStrategy output destination for the generated alert event
     */
    @Override
    public void generate(int patientId, OutputStrategy outputStrategy) {
        try {
            if (alertStates[patientId]) {
                if (randomGenerator.nextDouble() < 0.9) { // 90% chance to resolve
                    alertStates[patientId] = false;
                    // Output the alert
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "resolved");
                }
            } else {
                // Google Java Style Guide: local variable names use lowerCamelCase.
                double lambda = 0.1; // Average rate (alerts per period), adjust based on desired frequency
                double p = -Math.expm1(-lambda); // Probability of at least one alert in the period
                boolean alertTriggered = randomGenerator.nextDouble() < p;

                if (alertTriggered) {
                    alertStates[patientId] = true;
                    // Output the alert
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "triggered");
                }
            }
        // Google Java Style Guide: catch the specific RuntimeException instead of
        // the overly broad Exception type.
        } catch (RuntimeException exception) {
            System.err.println("An error occurred while generating alert data for patient "
                    + patientId + ": " + exception.getMessage());
        }
    }
}
