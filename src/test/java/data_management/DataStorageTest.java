package data_management;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.data_management.DataStorage;
import com.data_management.PatientRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataStorageTest {
    private DataStorage storage;

    @BeforeEach
    void setUp() {
        storage = DataStorage.getInstance();
        storage.clear();
    }

    @Test
    void testAddAndGetRecords() {
        storage.addPatientData(1, 100.0, "WhiteBloodCells", 1714376789050L);
        storage.addPatientData(1, 200.0, "WhiteBloodCells", 1714376789051L);
        storage.addPatientData(1, 300.0, "WhiteBloodCells", 1714376789052L);

        List<PatientRecord> records = storage.getRecords(1, 1714376789050L, 1714376789051L);
        assertEquals(2, records.size());
        assertEquals(100.0, records.get(0).getMeasurementValue());
        assertEquals(200.0, records.get(1).getMeasurementValue());
    }

    @Test
    void testGetRecordsReturnsEmptyForUnknownPatient() {
        List<PatientRecord> records = storage.getRecords(99, 1L, 2L);

        assertTrue(records.isEmpty());
    }

    @Test
    void testGetRecordsReturnsEmptyForInvalidRange() {
        storage.addPatientData(1, 100.0, "WhiteBloodCells", 10L);

        List<PatientRecord> records = storage.getRecords(1, 20L, 10L);

        assertTrue(records.isEmpty());
    }

    @Test
    void testGetInstanceReturnsSameDataStorage() {
        assertSame(storage, DataStorage.getInstance());
    }

    @Test
    void testClearRemovesStoredRecords() {
        storage.addPatientData(1, 100.0, "WhiteBloodCells", 10L);

        storage.clear();

        assertTrue(storage.getRecords(1, 0L, 20L).isEmpty());
    }

    @Test
    void testDuplicateRecordsAreIgnored() {
        storage.addPatientData(1, 100.0, "ECG", 10L);
        storage.addPatientData(1, 100.0, "ECG", 10L);
        storage.addPatientData(1, 101.0, "ECG", 10L);

        List<PatientRecord> records = storage.getRecords(1, 0L, 20L);

        assertEquals(2, records.size());
    }

    @Test
    void testGetRecordsReturnsSnapshot() {
        storage.addPatientData(1, 100.0, "ECG", 10L);

        List<PatientRecord> records = storage.getRecords(1, 0L, 20L);
        records.clear();

        assertEquals(1, storage.getRecords(1, 0L, 20L).size());
    }

    @Test
    void testConcurrentUpdatesAreStoredSafely() throws InterruptedException {
        int threadCount = 8;
        int recordsPerThread = 25;
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
            final int offset = threadIndex * recordsPerThread;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int recordIndex = 0; recordIndex < recordsPerThread; recordIndex++) {
                        storage.addPatientData(1, offset + recordIndex, "ECG", offset + recordIndex);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        executorService.shutdown();

        assertTrue(executorService.awaitTermination(2, TimeUnit.SECONDS));
        assertEquals(threadCount * recordsPerThread, storage.getRecords(1, 0L, 1000L).size());
    }
}
