package com.cardio_generator;

import java.io.IOException;
import java.util.Arrays;

import com.data_management.DataStorage;

/**
 * Application entry point that can run either the simulator or the data storage
 * workflow.
 */
public class Main {

    /**
     * Starts DataStorage when the first argument is {@code DataStorage}; otherwise
     * starts the health data simulator.
     *
     * @param args command-line arguments
     * @throws IOException if simulator startup fails while creating file output
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 0 && "DataStorage".equalsIgnoreCase(args[0])) {
            DataStorage.main(Arrays.copyOfRange(args, 1, args.length));
            return;
        }

        HealthDataSimulator.main(args);
    }
}
