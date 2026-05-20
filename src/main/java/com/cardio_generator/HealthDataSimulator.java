package com.cardio_generator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.cardio_generator.generators.AlertGenerator;

import com.cardio_generator.generators.BloodPressureDataGenerator;
import com.cardio_generator.generators.BloodSaturationDataGenerator;
import com.cardio_generator.generators.BloodLevelsDataGenerator;
import com.cardio_generator.generators.ECGDataGenerator;
import com.cardio_generator.outputs.ConsoleOutputStrategy;
import com.cardio_generator.outputs.FileOutputStrategy;
import com.cardio_generator.outputs.OutputStrategy;
import com.cardio_generator.outputs.TcpOutputStrategy;
import com.cardio_generator.outputs.WebSocketOutputStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Launches the cardiovascular data simulator and schedules periodic generators
 * for each configured patient.
 * The simulator supports multiple output strategies and can be configured from
 * the command line.
 */
public class HealthDataSimulator {

    private static final int DEFAULT_PATIENT_COUNT = 50;
    private static final HealthDataSimulator INSTANCE = new HealthDataSimulator();

    private final Random random = new Random();
    private int patientCount; // Default number of patients
    private ScheduledExecutorService scheduler;
    private OutputStrategy outputStrategy; // Default output strategy

    private HealthDataSimulator() {
        resetConfiguration();
    }

    /**
     * Returns the single shared simulator instance.
     *
     * @return singleton HealthDataSimulator instance
     */
    public static HealthDataSimulator getInstance() {
        return INSTANCE;
    }

    /**
     * Starts the simulator and schedules all recurring patient-data generation
     * tasks.
     *
     * @param args command-line options such as {@code -h},
     *             {@code --patient-count <count>}, and
     *             {@code --output <console|file:dir|websocket:port|tcp:port>}
     * @throws IOException if a file output directory cannot be created while
     *                     parsing the selected output strategy
     */
    public static void main(String[] args) throws IOException {
        getInstance().start(args);
    }

    /**
     * Starts the singleton simulator instance.
     *
     * @param args command-line options such as {@code -h},
     *             {@code --patient-count <count>}, and
     *             {@code --output <console|file:dir|websocket:port|tcp:port>}
     * @throws IOException if a file output directory cannot be created while
     *                     parsing the selected output strategy
     */
    public void start(String[] args) throws IOException {
        resetConfiguration();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        parseArguments(args);

        scheduler = Executors.newScheduledThreadPool(patientCount * 4);

        List<Integer> patientIds = initializePatientIds(patientCount);
        Collections.shuffle(patientIds); // Randomize the order of patient IDs

        scheduleTasksForPatients(patientIds);
    }

    /**
     * Restores default simulator configuration before a new run starts.
     */
    private void resetConfiguration() {
        patientCount = DEFAULT_PATIENT_COUNT;
        outputStrategy = new ConsoleOutputStrategy();
    }

    /**
     * Parses command-line arguments and updates the simulator configuration.
     *
     * @param args command-line options passed to the simulator
     * @throws IOException if file output is selected and its directory cannot be
     *                     created
     */
    private void parseArguments(String[] args) throws IOException {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                    printHelp();
                    System.exit(0);
                    break;
                case "--patient-count":
                    if (i + 1 < args.length) {
                        try {
                            patientCount = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err
                                    .println("Error: Invalid number of patients. Using default value: " + patientCount);
                        }
                    }
                    break;
                case "--output":
                    if (i + 1 < args.length) {
                        String outputArg = args[++i];
                        if (outputArg.equals("console")) {
                            outputStrategy = new ConsoleOutputStrategy();
                        } else if (outputArg.startsWith("file:")) {
                            String baseDirectory = outputArg.substring(5);
                            Path outputPath = Paths.get(baseDirectory);
                            if (!Files.exists(outputPath)) {
                                Files.createDirectories(outputPath);
                            }
                            outputStrategy = new FileOutputStrategy(baseDirectory);
                        } else if (outputArg.startsWith("websocket:")) {
                            try {
                                int port = Integer.parseInt(outputArg.substring(10));
                                // Initialize your WebSocket output strategy here
                                outputStrategy = new WebSocketOutputStrategy(port);
                                System.out.println("WebSocket output will be on port: " + port);
                            } catch (NumberFormatException e) {
                                System.err.println(
                                        "Invalid port for WebSocket output. Please specify a valid port number.");
                            }
                        } else if (outputArg.startsWith("tcp:")) {
                            try {
                                int port = Integer.parseInt(outputArg.substring(4));
                                // Initialize your TCP socket output strategy here
                                outputStrategy = new TcpOutputStrategy(port);
                                System.out.println("TCP socket output will be on port: " + port);
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid port for TCP output. Please specify a valid port number.");
                            }
                        } else {
                            System.err.println("Unknown output type. Using default (console).");
                        }
                    }
                    break;
                default:
                    System.err.println("Unknown option '" + args[i] + "'");
                    printHelp();
                    System.exit(1);
            }
        }
    }

    /**
     * Prints a short usage summary describing the supported simulator options.
     */
    private void printHelp() {
        System.out.println("Usage: java HealthDataSimulator [options]");
        System.out.println("Options:");
        System.out.println("  -h                       Show help and exit.");
        System.out.println(
                "  --patient-count <count>  Specify the number of patients to simulate data for (default: 50).");
        System.out.println("  --output <type>          Define the output method. Options are:");
        System.out.println("                             'console' for console output,");
        System.out.println("                             'file:<directory>' for file output,");
        System.out.println("                             'websocket:<port>' for WebSocket output,");
        System.out.println("                             'tcp:<port>' for TCP socket output.");
        System.out.println("Example:");
        System.out.println("  java HealthDataSimulator --patient-count 100 --output websocket:8080");
        System.out.println(
                "  This command simulates data for 100 patients and sends the output to WebSocket clients connected to port 8080.");
    }

    /**
     * Creates the list of patient identifiers used by the simulator.
     *
     * @param patientCount number of patient IDs to create; IDs start at 1 and end
     *                     at {@code patientCount}
     * @return a list containing sequential patient IDs from 1 to
     *         {@code patientCount}
     */
    private List<Integer> initializePatientIds(int patientCount) {
        List<Integer> patientIds = new ArrayList<>();
        for (int i = 1; i <= patientCount; i++) {
            patientIds.add(i);
        }
        return patientIds;
    }

    /**
     * Schedules each generator at its configured interval for every patient.
     *
     * @param patientIds identifiers for the patients whose data should be simulated
     */
    private void scheduleTasksForPatients(List<Integer> patientIds) {
        ECGDataGenerator ecgDataGenerator = new ECGDataGenerator(patientCount);
        BloodSaturationDataGenerator bloodSaturationDataGenerator = new BloodSaturationDataGenerator(patientCount);
        BloodPressureDataGenerator bloodPressureDataGenerator = new BloodPressureDataGenerator(patientCount);
        BloodLevelsDataGenerator bloodLevelsDataGenerator = new BloodLevelsDataGenerator(patientCount);
        AlertGenerator alertGenerator = new AlertGenerator(patientCount);

        for (int patientId : patientIds) {
            scheduleTask(() -> ecgDataGenerator.generate(patientId, outputStrategy), 1, TimeUnit.SECONDS);
            scheduleTask(() -> bloodSaturationDataGenerator.generate(patientId, outputStrategy), 1, TimeUnit.SECONDS);
            scheduleTask(() -> bloodPressureDataGenerator.generate(patientId, outputStrategy), 1, TimeUnit.MINUTES);
            scheduleTask(() -> bloodLevelsDataGenerator.generate(patientId, outputStrategy), 2, TimeUnit.MINUTES);
            scheduleTask(() -> alertGenerator.generate(patientId, outputStrategy), 20, TimeUnit.SECONDS);
        }
    }

    /**
     * Registers a recurring simulator task with a small randomized initial delay.
     *
     * @param task unit of work that generates one category of patient data
     * @param period fixed interval between consecutive executions
     * @param timeUnit unit used to interpret {@code period}
     */
    private void scheduleTask(Runnable task, long period, TimeUnit timeUnit) {
        scheduler.scheduleAtFixedRate(task, random.nextInt(5), period, timeUnit);
    }
}
