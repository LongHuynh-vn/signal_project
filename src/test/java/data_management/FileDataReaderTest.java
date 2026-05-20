package data_management;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.data_management.DataStorage;
import com.data_management.FileDataReader;
import com.data_management.PatientRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileDataReaderTest {

    @TempDir
    Path tempDirectory;

    @Test
    void testReadDataImportsValidSimulatorLines() throws IOException {
        Files.write(tempDirectory.resolve("Saturation.txt"), List.of(
                "Patient ID: 1, Timestamp: 1000, Label: Saturation, Data: 91%",
                "Patient ID: 1, Timestamp: 2000, Label: ECG, Data: 0.25",
                "Patient ID: 1, Timestamp: 3000, Label: Alert, Data: triggered",
                "Patient ID: 1, Timestamp: 4000, Label: Alert, Data: resolved"));
        Files.write(tempDirectory.resolve("bad.txt"), List.of(
                "not a simulator line",
                "Patient ID: abc, Timestamp: 5000, Label: ECG, Data: 0.1"));

        DataStorage storage = DataStorage.getInstance();
        storage.clear();
        FileDataReader reader = new FileDataReader(tempDirectory);

        reader.readData(storage);

        List<PatientRecord> records = storage.getRecords(1, 0L, 5000L);
        assertEquals(4, records.size());
        assertEquals(91.0, records.get(0).getMeasurementValue());
        assertEquals(0.25, records.get(1).getMeasurementValue());
        assertEquals(1.0, records.get(2).getMeasurementValue());
        assertEquals(0.0, records.get(3).getMeasurementValue());
    }
}
