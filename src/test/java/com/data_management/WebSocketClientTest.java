package com.data_management;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alerts.Alert;
import com.alerts.AlertGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketClientTest {
    private DataStorage storage;
    private TestServer server;

    @BeforeEach
    void setUp() {
        storage = DataStorage.getInstance();
        storage.clear();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (server != null) {
            server.stop();
        }
        storage.clear();
    }

    @Test
    void testParseNumericMessage() {
        WebSocketClient.ParsedRecord parsedRecord = WebSocketClient.parseMessage("1,1000,ECG,0.25");

        assertEquals(1, parsedRecord.patientId);
        assertEquals(1000L, parsedRecord.timestamp);
        assertEquals("ECG", parsedRecord.recordType);
        assertEquals(0.25, parsedRecord.measurementValue);
    }

    @Test
    void testParsePercentageAndAlertValues() {
        assertEquals(91.0, WebSocketClient.parseMessage("1,1000,Saturation,91%").measurementValue);
        assertEquals(1.0, WebSocketClient.parseMessage("1,1000,Alert,triggered").measurementValue);
        assertEquals(0.0, WebSocketClient.parseMessage("1,1000,Alert,resolved").measurementValue);
    }

    @Test
    void testParseRejectsMalformedMessages() {
        assertThrows(IllegalArgumentException.class, () -> WebSocketClient.parseMessage("not csv"));
        assertThrows(IllegalArgumentException.class, () -> WebSocketClient.parseMessage("abc,1000,ECG,0.1"));
        assertThrows(IllegalArgumentException.class, () -> WebSocketClient.parseMessage("1,bad,ECG,0.1"));
        assertThrows(IllegalArgumentException.class, () -> WebSocketClient.parseMessage("1,1000,,0.1"));
        assertThrows(IllegalArgumentException.class, () -> WebSocketClient.parseMessage("1,1000,ECG,bad"));
        assertThrows(IllegalArgumentException.class, () -> WebSocketClient.parseMessage("0,1000,ECG,0.1"));
        assertThrows(IllegalArgumentException.class, () -> WebSocketClient.parseMessage("1,-1,ECG,0.1"));
    }

    @Test
    void testReadDataRequiresStorage() throws Exception {
        WebSocketClient client = new WebSocketClient(new URI("ws://localhost:1"), 100L);

        assertThrows(IOException.class, () -> client.readData(null));
    }

    @Test
    void testReadDataThrowsWhenConnectionFails() throws Exception {
        WebSocketClient client = new WebSocketClient(new URI("ws://localhost:" + freePort()), 200L);

        try {
            assertThrows(IOException.class, () -> client.readData(storage));
        } finally {
            client.close();
        }
    }

    @Test
    void testClientReceivesStoresAndSkipsMessages() throws Exception {
        startServer();
        WebSocketClient client = connectClient();

        server.sendToClient("1,1000,ECG,0.25");
        server.sendToClient("bad message");
        server.sendToClient("1,2000,Alert,triggered");

        assertTrue(waitForRecordCount(1, 2));
        List<PatientRecord> records = storage.getRecords(1, 0L, 3000L);
        assertEquals(2, records.size());
        assertEquals(0.25, records.get(0).getMeasurementValue());
        assertEquals(1.0, records.get(1).getMeasurementValue());

        server.stop();
        assertTrue(client.waitUntilClosed(2, TimeUnit.SECONDS));
    }

    @Test
    void testClientDataWorksWithAlertGeneration() throws Exception {
        startServer();
        WebSocketClient client = connectClient();

        server.sendToClient("2,2000,Saturation,91%");

        assertTrue(waitForRecordCount(2, 1));
        AlertGenerator alertGenerator = new AlertGenerator(storage);
        alertGenerator.evaluateAllPatients();
        List<Alert> alerts = alertGenerator.getAlerts();

        assertTrue(alerts.stream()
                .anyMatch(alert -> AlertGenerator.LOW_SATURATION.equals(alert.getCondition())));

        server.stop();
        assertTrue(client.waitUntilClosed(2, TimeUnit.SECONDS));
    }

    private void startServer() throws InterruptedException, IOException {
        server = new TestServer(new InetSocketAddress("localhost", freePort()));
        server.start();
        assertTrue(server.awaitStart());
    }

    private WebSocketClient connectClient() throws Exception {
        URI uri = new URI("ws://localhost:" + server.getPort());
        WebSocketClient client = new WebSocketClient(uri, 1000L);
        client.readData(storage);
        assertTrue(server.awaitConnection());
        return client;
    }

    private boolean waitForRecordCount(int patientId, int expectedCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000L;
        while (System.currentTimeMillis() < deadline) {
            if (storage.getRecords(patientId, 0L, Long.MAX_VALUE).size() == expectedCount) {
                return true;
            }
            Thread.sleep(10L);
        }
        return false;
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static class TestServer extends WebSocketServer {
        private final CountDownLatch startLatch = new CountDownLatch(1);
        private final CountDownLatch connectionLatch = new CountDownLatch(1);
        private volatile WebSocket connection;

        TestServer(InetSocketAddress address) {
            super(address);
        }

        boolean awaitStart() throws InterruptedException {
            return startLatch.await(2, TimeUnit.SECONDS);
        }

        boolean awaitConnection() throws InterruptedException {
            return connectionLatch.await(2, TimeUnit.SECONDS);
        }

        void sendToClient(String message) {
            connection.send(message);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            connection = conn;
            connectionLatch.countDown();
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            // The client observes the close event; no server-side action is needed.
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            // The test server only sends messages to the client.
        }

        @Override
        public void onError(WebSocket conn, Exception exception) {
            // Test assertions observe failures through connection and message timeouts.
        }

        @Override
        public void onStart() {
            startLatch.countDown();
        }
    }
}
