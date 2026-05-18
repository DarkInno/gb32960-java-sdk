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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("benchmark")
class StabilityTest {

    private static final int CONNECTION_COUNT = 100;
    private static final int DURATION_SECONDS = 30;

    private Gb32960Server server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        server = Gb32960Server.create(c -> c.port(0).workerThreads(4).maxConnections(CONNECTION_COUNT + 10)
                .idleTimeoutSeconds(60));
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
    void testStability() throws Exception {
        var messagesSent = new AtomicLong();
        var connectionDropped = new AtomicInteger();
        var allStarted = new CountDownLatch(CONNECTION_COUNT);
        var allFinished = new CountDownLatch(CONNECTION_COUNT);

        List<Socket> sockets = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        long beforeMem = usedMemory();
        long startNs = System.nanoTime();

        for (int c = 0; c < CONNECTION_COUNT; c++) {
            String vin = String.format("STABLEVIN%08d", c);
            String iccid = String.format("89860000001%07d", c);
            final int connIndex = c;

            Socket socket = new Socket("127.0.0.1", port);
            socket.setTcpNoDelay(true);
            sockets.add(socket);

            Thread thread = new Thread(() -> {
                try {
                    OutputStream out = socket.getOutputStream();

                    byte[] login = buildVehicleLogin(vin, 1, iccid, 1, 4);
                    out.write(login);
                    out.flush();
                    messagesSent.incrementAndGet();

                    allStarted.countDown();

                    long endTime = System.currentTimeMillis() + DURATION_SECONDS * 1000L;
                    int heartbeatSeq = 1;
                    int realtimeSeq = 1;

                    while (System.currentTimeMillis() < endTime) {
                        long now = System.currentTimeMillis();
                        long remainingMs = endTime - now;
                        if (remainingMs <= 0) break;

                        byte[] hb = buildHeartbeat(vin);
                        out.write(hb);
                        messagesSent.incrementAndGet();

                        if (realtimeSeq % 5 == 0) {
                            byte[] rt = buildRealtimeData(vin, 60.0, 75,
                                    121.0 + connIndex * 0.01, 31.0 + connIndex * 0.01);
                            out.write(rt);
                            messagesSent.incrementAndGet();
                        }

                        out.flush();

                        heartbeatSeq++;
                        realtimeSeq++;

                        long sleepMs = 1000;
                        if (remainingMs < sleepMs) {
                            sleepMs = Math.max(10, remainingMs);
                        }
                        Thread.sleep(sleepMs);
                    }

                    byte[] logout = buildVehicleLogout(vin, 99);
                    out.write(logout);
                    out.flush();
                    messagesSent.incrementAndGet();
                } catch (Exception e) {
                    connectionDropped.incrementAndGet();
                    System.err.println("Stability connection error [" + vin + "]: " + e.getMessage());
                } finally {
                    allFinished.countDown();
                    try { socket.close(); } catch (Exception ignored) {}
                }
            }, "stability-" + vin);
            threads.add(thread);
            thread.start();
        }

        boolean started = allStarted.await(10, TimeUnit.SECONDS);
        assertTrue(started, "Not all connections started: " + (CONNECTION_COUNT - allStarted.getCount()));

        boolean finished = allFinished.await(DURATION_SECONDS + 15, TimeUnit.SECONDS);
        long elapsedNs = System.nanoTime() - startNs;
        long afterMem = usedMemory();
        long serverMsgs = server.getMessagesReceived();
        long serverSent = server.getMessagesSent();

        for (Thread t : threads) {
            t.join(1000);
        }
        for (Socket s : sockets) {
            try { s.close(); } catch (Exception ignored) {}
        }

        double elapsedSec = elapsedNs / 1_000_000_000.0;

        printResult("Long-Running Stability Test", elapsedNs, messagesSent.get(),
                serverMsgs, serverSent, connectionDropped.get(), beforeMem, afterMem);

        assertEquals(0, connectionDropped.get(), "Some connections dropped unexpectedly");
        assertTrue(serverMsgs > 0, "Server should have received messages");
        assertTrue(finished || allFinished.getCount() <= CONNECTION_COUNT * 0.05,
                "Too many connections didn't finish: " + allFinished.getCount());
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

    private static byte[] buildVehicleLogout(String vin, int serialNumber) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        writeBcdTime(data);
        data.write((serialNumber >> 8) & 0xFF);
        data.write(serialNumber & 0xFF);
        return buildMessage((byte) 0x04, (byte) 0xFE, vin, data.toByteArray());
    }

    private static byte[] buildHeartbeat(String vin) {
        return buildMessage((byte) 0x07, (byte) 0xFE, vin, new byte[0]);
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

    private static long usedMemory() {
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private static void printResult(String title, long elapsedNs, long clientMsgs,
                                     long serverMsgs, long serverSent, int dropped,
                                     long memBefore, long memAfter) {
        double elapsedMs = elapsedNs / 1_000_000.0;
        double elapsedSec = elapsedNs / 1_000_000_000.0;
        long memDelta = memAfter - memBefore;

        System.out.println();
        System.out.println("+--------------------------------------------------------+");
        System.out.println("| " + padRight(title, 54) + "|");
        System.out.println("+--------------------------------------------------------+");
        System.out.println("| " + padRight("Connections", 30) + " | " + padLeft(String.valueOf(CONNECTION_COUNT), 20) + " |");
        System.out.println("| " + padRight("Duration", 30) + " | " + padLeft(DURATION_SECONDS + " seconds", 20) + " |");
        System.out.println("| " + padRight("Client messages sent", 30) + " | " + padLeft(String.format("%,d", clientMsgs), 20) + " |");
        System.out.println("| " + padRight("Server messages received", 30) + " | " + padLeft(String.format("%,d", serverMsgs), 20) + " |");
        System.out.println("| " + padRight("Server messages sent", 30) + " | " + padLeft(String.format("%,d", serverSent), 20) + " |");
        System.out.println("| " + padRight("Total time", 30) + " | " + padLeft(String.format("%,.2f ms (%.3f s)", elapsedMs, elapsedSec), 20) + " |");
        System.out.println("| " + padRight("Messages/sec (client)", 30) + " | " + padLeft(String.format("%,.0f", clientMsgs / elapsedSec), 20) + " |");
        System.out.println("| " + padRight("Messages/sec (server)", 30) + " | " + padLeft(String.format("%,.0f", serverMsgs / elapsedSec), 20) + " |");
        System.out.println("| " + padRight("Connections dropped", 30) + " | " + padLeft(String.valueOf(dropped), 20) + " |");
        System.out.println("| " + padRight("Memory before", 30) + " | " + padLeft(formatBytes(memBefore), 20) + " |");
        System.out.println("| " + padRight("Memory after", 30) + " | " + padLeft(formatBytes(memAfter), 20) + " |");
        System.out.println("| " + padRight("Memory delta", 30) + " | " + padLeft(formatBytes(memDelta), 20) + " |");
        System.out.println("+--------------------------------------------------------+");
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static String padLeft(String s, int n) {
        return String.format("%" + n + "s", s);
    }

    private static String formatBytes(long bytes) {
        if (Math.abs(bytes) >= 1024 * 1024 * 1024) return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        if (Math.abs(bytes) >= 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        if (Math.abs(bytes) >= 1024) return String.format("%.2f KB", bytes / 1024.0);
        return bytes + " B";
    }
}
