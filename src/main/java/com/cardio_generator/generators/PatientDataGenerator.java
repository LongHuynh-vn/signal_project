package com.cardio_generator.generators;

import com.cardio_generator.outputs.OutputStrategy;

/**
 * Defines a generator that produces one category of simulated patient data.
 * Implementations create a measurement for a patient and send it to an
 * {@link OutputStrategy}.
 */
public interface PatientDataGenerator {

    /**
     * Generates one simulated data point for the specified patient.
     *
     * @param patientId identifier of the patient receiving the generated reading
     * @param outputStrategy destination that receives the formatted output
     */
    void generate(int patientId, OutputStrategy outputStrategy);
}
