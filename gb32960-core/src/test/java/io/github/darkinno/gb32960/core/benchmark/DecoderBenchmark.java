package io.github.darkinno.gb32960.core.benchmark;

import io.github.darkinno.gb32960.core.codec.MessageDecoder;
import io.github.darkinno.gb32960.core.model.RawMessage;
import io.github.darkinno.gb32960.core.util.TestMessageBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("benchmark")
class DecoderBenchmark {

    private static final int MESSAGE_COUNT = 100_000;
    private static final int THREAD_COUNT = 8;
    private static final double MIN_SINGLE_THREADED_TPS = 50_000;
    private static final double MIN_MULTI_THREADED_TPS = 200_000;

    @Test
    @Tag("benchmark")
    void benchmarkSingleThreaded() {
        var messages = generateMessages(MESSAGE_COUNT);

        long beforeMem = usedMemory();
        long start = System.nanoTime();

        for (byte[] msg : messages) {
            RawMessage raw = MessageDecoder.decodeRaw(msg);
            MessageDecoder.decode(raw);
        }

        long elapsedNs = System.nanoTime() - start;
        long afterMem = usedMemory();
        double elapsedSec = elapsedNs / 1_000_000_000.0;
        double tps = MESSAGE_COUNT / elapsedSec;

        printResult("Decoder Benchmark - Single Threaded", MESSAGE_COUNT, elapsedNs, tps,
                beforeMem, afterMem);

        assertTrue(tps > MIN_SINGLE_THREADED_TPS,
                "Single-threaded throughput " + formatTps(tps) + " below minimum " + formatTps(MIN_SINGLE_THREADED_TPS));
    }

    @Test
    @Tag("benchmark")
    void benchmarkMultiThreaded() throws Exception {
        var messages = generateMessages(MESSAGE_COUNT);
        var counter = new AtomicLong();
        var latch = new CountDownLatch(THREAD_COUNT);
        int perThread = MESSAGE_COUNT / THREAD_COUNT;

        long beforeMem = usedMemory();
        long start = System.nanoTime();

        for (int t = 0; t < THREAD_COUNT; t++) {
            int startIdx = t * perThread;
            int endIdx = (t == THREAD_COUNT - 1) ? MESSAGE_COUNT : startIdx + perThread;
            new Thread(() -> {
                try {
                    for (int i = startIdx; i < endIdx; i++) {
                        RawMessage raw = MessageDecoder.decodeRaw(messages[i]);
                        MessageDecoder.decode(raw);
                        counter.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        long elapsedNs = System.nanoTime() - start;
        long afterMem = usedMemory();
        double elapsedSec = elapsedNs / 1_000_000_000.0;
        double tps = counter.get() / elapsedSec;

        printResult("Decoder Benchmark - Multi Threaded (" + THREAD_COUNT + " threads)",
                (int) counter.get(), elapsedNs, tps, beforeMem, afterMem);

        assertTrue(tps > MIN_MULTI_THREADED_TPS,
                "Multi-threaded throughput " + formatTps(tps) + " below minimum " + formatTps(MIN_MULTI_THREADED_TPS));
    }

    private static byte[][] generateMessages(int count) {
        byte[][] messages = new byte[count][];
        for (int i = 0; i < count; i++) {
            String vin = String.format("LSVAM40E%08d", i);
            messages[i] = TestMessageBuilder.buildRealtimeData(
                    vin, 60.0 + (i % 60), 50 + (i % 50), 121.0 + (i % 1000) * 0.001, 31.0 + (i % 1000) * 0.001);
        }
        return messages;
    }

    private static long usedMemory() {
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private static String formatTps(double tps) {
        if (tps >= 1_000_000) return String.format("%.2fM msg/s", tps / 1_000_000);
        if (tps >= 1_000) return String.format("%.2fK msg/s", tps / 1_000);
        return String.format("%.0f msg/s", tps);
    }

    private static void printResult(String title, int count, long elapsedNs, double tps,
                                     long memBefore, long memAfter) {
        double elapsedMs = elapsedNs / 1_000_000.0;
        double elapsedSec = elapsedNs / 1_000_000_000.0;
        long memDelta = memAfter - memBefore;

        System.out.println();
        System.out.println("+--------------------------------------------------------+");
        System.out.println("| " + padRight(title, 54) + "|");
        System.out.println("+--------------------------------------------------------+");
        System.out.println("| " + padRight("Messages processed", 30) + " | " + padLeft(String.format("%,d", count), 20) + " |");
        System.out.println("| " + padRight("Total time", 30) + " | " + padLeft(String.format("%,.2f ms (%.3f s)", elapsedMs, elapsedSec), 20) + " |");
        System.out.println("| " + padRight("Throughput", 30) + " | " + padLeft(formatTps(tps), 20) + " |");
        System.out.println("| " + padRight("Memory before", 30) + " | " + padLeft(formatBytes(memBefore), 20) + " |");
        System.out.println("| " + padRight("Memory after", 30) + " | " + padLeft(formatBytes(memAfter), 20) + " |");
        System.out.println("| " + padRight("Memory delta", 30) + " | " + padLeft(formatBytes(memDelta), 20) + " |");
        System.out.println("| " + padRight("Avg time per msg", 30) + " | " + padLeft(String.format("%,.0f ns", elapsedNs / (double) count), 20) + " |");
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
