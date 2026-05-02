package com.cardio_generator.outputs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

/**
 * Streams generated patient data to a TCP client connected to a listening
 * server socket.
 * The strategy accepts one client connection asynchronously and then forwards
 * each generated measurement as a comma-separated line.
 */
public class TcpOutputStrategy implements OutputStrategy {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;

    /**
     * Starts a TCP server that waits for a client to connect.
     *
     * @param port port on which the server should listen for incoming TCP
     *             connections
     */
    public TcpOutputStrategy(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("TCP Server started on port " + port);

            // Accept clients in a new thread to not block the main thread
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    clientSocket = serverSocket.accept();
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    System.out.println("Client connected: " + clientSocket.getInetAddress());
                } catch (IOException exception) {
                    System.err.println("Failed to accept a TCP client connection: " + exception.getMessage());
                }
            });
        } catch (IOException exception) {
            System.err.println("Failed to start the TCP server on port " + port + ": "
                    + exception.getMessage());
        }
    }

    /**
     * Sends one generated measurement to the connected TCP client if a client
     * session is active.
     *
     * @param patientId unique identifier of the patient that produced the data
     * @param timestamp measurement timestamp in milliseconds since the Unix epoch
     * @param label measurement category, such as Alert or Saturation
     * @param data formatted measurement value that will be transmitted
     */
    @Override
    public void output(int patientId, long timestamp, String label, String data) {
        if (out != null) {
            String message = String.format("%d,%d,%s,%s", patientId, timestamp, label, data);
            out.println(message);
        }
    }
}
