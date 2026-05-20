package com.data_management;

import java.io.IOException;

public interface DataReader extends AutoCloseable {
    /**
     * Reads data from a specified source and stores it in the data storage. File
     * readers finish after importing their input, while streaming readers may keep
     * receiving data after their connection is established.
     * 
     * @param dataStorage the storage where data will be stored
     * @throws IOException if there is an error reading the data
     */
    void readData(DataStorage dataStorage) throws IOException;

    /**
     * Closes the data source when the reader owns an open stream or connection.
     *
     * @throws IOException if the source cannot be closed
     */
    @Override
    default void close() throws IOException {
        // File-based readers have no persistent resource to close by default.
    }
}
