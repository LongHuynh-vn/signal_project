package data_management;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.data_management.DataStorage;
import com.data_management.PatientRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataStorageTest {

    @Test
    void testAddAndGetRecords() {
        DataStorage storage = new DataStorage();
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
        DataStorage storage = new DataStorage();

        List<PatientRecord> records = storage.getRecords(99, 1L, 2L);

        assertTrue(records.isEmpty());
    }

    @Test
    void testGetRecordsReturnsEmptyForInvalidRange() {
        DataStorage storage = new DataStorage();
        storage.addPatientData(1, 100.0, "WhiteBloodCells", 10L);

        List<PatientRecord> records = storage.getRecords(1, 20L, 10L);

        assertTrue(records.isEmpty());
    }
}
