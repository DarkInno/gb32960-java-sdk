package io.github.darkinno.gb32960.transport.benchmark;

import io.github.darkinno.gb32960.transport.server.Gb32960Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("benchmark")
class ConnectionFloodTest {

    private static final int VEHICLE_COUNT = 500;
    private static final int MESSAGES_PER_VEHICLE = 11;
    private static final int TIMEOUT_SECONDS = 60;

    private Gb32960Server server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        server = Gb32960Server.create(c -> c.port(0).workerThreads(4).maxConnections(VEHICLE_COUNT + 10));
        server.start();
        port = server.getActualPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    @Tag("benchmark")
    void testConnectionFlood() throws Exception {
        var completedVehicles = new AtomicInteger();
        var errors = new AtomicInteger();
        var latch = new CountDownLatch(VEHICLE_COUNT);
        var executor = Executors.newFixedThreadPool(50);

        long startNs = System.nanoTime();

        for (int v = 0; v < VEHICLE_COUNT; v++) {
            String vin = String.format("TESTVIN%011d", v);
            String iccid = String.format("89860000000%08d", v);
            executor.submit(() -> {
                try {
                    vehicleSimulation(vin, iccid);
                    completedVehicles.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                    System.err.println("Vehicle error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean allDone = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        long elapsedNs = System.nanoTime() - startNs;
        long serverMsgs = server.getMessagesReceived();

        printResult("Connection Flood Test", elapsedNs, completedVehicles.get(),
                errors.get(), serverMsgs);

        assertEquals(0, errors.get(), "Some vehicles had errors");
        assertTrue(allDone, "Test timed out: " + completedVehicles.get() + "/" + VEHICLE_COUNT + " completed");
    }

    private void vehicleSimulation(String vin, String iccid) throws Exception {
        Socket socket = null;
        int retries = 5;
        while (retries > 0) {
            try {
                socket = new Socket("127.0.0.1", port);
                break;
            } catch (Exception e) {
                retries--;
                if (retries == 0) throw e;
                Thread.sleep(50);
            }
        }
        try {
            socket.setTcpNoDelay(true);
            OutputStream out = socket.getOutputStream();

            byte[] login = buildVehicleLogin(vin, 1, iccid, 1, 4);
            out.write(login);

            for (int i = 0; i < 10; i++) {
                byte[] realtime = buildRealtimeData(vin, 60.0 + (i % 60), 50 + (i % 50),
                        121.0 + i * 0.01, 31.0 + i * 0.01);
                out.write(realtime);
            }

            out.flush();
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static byte[] buildVehicleLogin(String vin, int serialNumber, String iccid,
                                            int batteryCount, int codeLength) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        writeBcdTime(data);
        data.write((serialNumber >> 8) & 0xFF);
        data.write(serialNumber & 0xFF);
        writeFixedString(data, iccid, 20);
        data.write(batteryCount & 0xFF);
        data.write((codeLength >> 8) & 0xFF);
        data.write(codeLength & 0xFF);
        return buildMessage((byte) 0x01, (byte) 0xFE, vin, data.toByteArray());
    }

    private static byte[] buildRealtimeData(String vin, double speed, int soc, double lng, double lat) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        writeBcdTime(data);
        data.write(0x01);
        writeVehicleData(data, speed, soc);
        data.write(0x05);
        writePositionData(data, lng, lat);
        return buildMessage((byte) 0x02, (byte) 0xFE, vin, data.toByteArray());
    }

    private static byte[] buildMessage(byte cmd, byte resp, String vin, byte[] dataUnit) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(0x23);
        baos.write(0x23);
        baos.write(cmd);
        baos.write(resp);
        byte[] vinBytes = padVin(vin);
        baos.write(vinBytes, 0, vinBytes.length);
        baos.write(0x01);
        baos.write((dataUnit.length >> 8) & 0xFF);
        baos.write(dataUnit.length & 0xFF);
        baos.write(dataUnit, 0, dataUnit.length);
        byte[] full = baos.toByteArray();
        byte bcc = calculateBcc(full, 2, full.length - 2);
        baos.write(bcc);
        return baos.toByteArray();
    }

    private static void writeBcdTime(ByteArrayOutputStream baos) {
        LocalDateTime now = LocalDateTime.now();
        baos.write(intToBcd(now.getYear() % 100));
        baos.write(intToBcd(now.getMonthValue()));
        baos.write(intToBcd(now.getDayOfMonth()));
        baos.write(intToBcd(now.getHour()));
        baos.write(intToBcd(now.getMinute()));
        baos.write(intToBcd(now.getSecond()));
    }

    private static void writeVehicleData(ByteArrayOutputStream baos, double speed, int soc) {
        int speedRaw = (int) (speed * 10);
        baos.write(0x01);
        baos.write(0x01);
        baos.write(0x01);
        baos.write((speedRaw >> 8) & 0xFF);
        baos.write(speedRaw & 0xFF);
        for (int i = 0; i < 4; i++) baos.write(0x00);
        baos.write(0x01); baos.write(0xF4);
        baos.write((byte) 0xFF); baos.write((byte) 0xCE);
        baos.write(soc & 0xFF);
        baos.write(0x01);
        baos.write(0x00);
        baos.write(0x27); baos.write(0x10);
        baos.write(0x00);
        baos.write(0x00);
        baos.write(0x00); baos.write(0x00);
    }

    private static void writePositionData(ByteArrayOutputStream baos, double lng, double lat) {
        int lngRaw = (int) (lng * 1_000_000);
        int latRaw = (int) (lat * 1_000_000);
        baos.write((lngRaw >> 24) & 0xFF);
        baos.write((lngRaw >> 16) & 0xFF);
        baos.write((lngRaw >> 8) & 0xFF);
        baos.write(lngRaw & 0xFF);
        baos.write((latRaw >> 24) & 0xFF);
        baos.write((latRaw >> 16) & 0xFF);
        baos.write((latRaw >> 8) & 0xFF);
        baos.write(latRaw & 0xFF);
        baos.write(0x00); baos.write(0x46); baos.write(0x00); baos.write(0x5A); baos.write(0x00);
    }

    private static byte[] padVin(String vin) {
        byte[] result = new byte[17];
        byte[] bytes = (vin != null ? vin : "").getBytes(StandardCharsets.US_ASCII);
        int len = Math.min(bytes.length, 17);
        System.arraycopy(bytes, 0, result, 0, len);
        return result;
    }

    private static void writeFixedString(ByteArrayOutputStream baos, String s, int length) {
        byte[] result = new byte[length];
        byte[] bytes = (s != null ? s : "").getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, result, 0, Math.min(bytes.length, length));
        baos.write(result, 0, result.length);
    }

    private static byte calculateBcc(byte[] data, int offset, int length) {
        byte bcc = data[offset];
        for (int i = offset + 1; i < offset + length; i++) {
            bcc ^= data[i];
        }
        return bcc;
    }

    private static byte intToBcd(int value) {
        return (byte) (((value / 10) << 4) | (value % 10));
    }

    private static void printResult(String title, long elapsedNs, int completedVehicles,
                                     int errors, long serverMsgs) {
        double elapsedMs = elapsedNs / 1_000_000.0;
        double elapsedSec = elapsedNs / 1_000_000_000.0;
        int totalMsgs = VEHICLE_COUNT * MESSAGES_PER_VEHICLE;

        System.out.println();
        System.out.println("+--------------------------------------------------------+");
        System.out.println("| " + padRight(title, 54) + "|");
        System.out.println("+--------------------------------------------------------+");
        System.out.println("| " + padRight("Vehicles", 30) + " | " + padLeft(String.valueOf(VEHICLE_COUNT), 20) + " |");
        System.out.println("| " + padRight("Messages per vehicle", 30) + " | " + padLeft(String.valueOf(MESSAGES_PER_VEHICLE), 20) + " |");
        System.out.println("| " + padRight("Total messages sent", 30) + " | " + padLeft(String.format("%,d", totalMsgs), 20) + " |");
        System.out.println("| " + padRight("Completed vehicles", 30) + " | " + padLeft(String.valueOf(completedVehicles), 20) + " |");
        System.out.println("| " + padRight("Errors", 30) + " | " + padLeft(String.valueOf(errors), 20) + " |");
        System.out.println("| " + padRight("Total time", 30) + " | " + padLeft(String.format("%,.2f ms (%.3f s)", elapsedMs, elapsedSec), 20) + " |");
        System.out.println("| " + padRight("Connections/sec", 30) + " | " + padLeft(String.format("%.1f", completedVehicles / elapsedSec), 20) + " |");
        System.out.println("| " + padRight("Messages/sec (sent)", 30) + " | " + padLeft(String.format("%,.0f", totalMsgs / elapsedSec), 20) + " |");
        System.out.println("| " + padRight("Server msgs received", 30) + " | " + padLeft(String.format("%,d", serverMsgs), 20) + " |");
        System.out.println("+--------------------------------------------------------+");
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static String padLeft(String s, int n) {
        return String.format("%" + n + "s", s);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
