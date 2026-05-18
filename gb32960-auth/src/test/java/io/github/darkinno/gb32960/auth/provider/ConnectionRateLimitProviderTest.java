package io.github.darkinno.gb32960.auth.provider;

import io.github.darkinno.gb32960.auth.api.AuthProvider;
import io.github.darkinno.gb32960.core.model.RawMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionRateLimitProviderTest {

    @Test
    void shouldPassForFirstRequest() {
        ConnectionRateLimitProvider provider = new ConnectionRateLimitProvider(3, 60);

        AuthProvider.AuthResult result = provider.authenticate(vinMessage("VIN001"));
        assertTrue(result.isPassed());
    }

    @Test
    void shouldFailForEmptyVin() {
        ConnectionRateLimitProvider provider = new ConnectionRateLimitProvider();

        RawMessage msg = new RawMessage();
        msg.setVin("");

        AuthProvider.AuthResult result = provider.authenticate(msg);
        assertFalse(result.isPassed());
    }

    @Test
    void shouldFailForNullVin() {
        ConnectionRateLimitProvider provider = new ConnectionRateLimitProvider();

        AuthProvider.AuthResult result = provider.authenticate(new RawMessage());
        assertFalse(result.isPassed());
    }

    @Test
    void shouldPassUpToLimit() {
        ConnectionRateLimitProvider provider = new ConnectionRateLimitProvider(3, 60);

        assertTrue(provider.authenticate(vinMessage("VIN001")).isPassed());
        assertTrue(provider.authenticate(vinMessage("VIN001")).isPassed());
        assertTrue(provider.authenticate(vinMessage("VIN001")).isPassed());
    }

    @Test
    void shouldBanAfterExceedingLimit() {
        ConnectionRateLimitProvider provider = new ConnectionRateLimitProvider(2, 60);

        assertTrue(provider.authenticate(vinMessage("VIN001")).isPassed());
        assertTrue(provider.authenticate(vinMessage("VIN001")).isPassed());

        AuthProvider.AuthResult result = provider.authenticate(vinMessage("VIN001"));
        assertFalse(result.isPassed());
        assertTrue(result.getReason().contains("Rate limit exceeded"));
    }

    @Test
    void shouldAllowDifferentVinsIndependently() {
        ConnectionRateLimitProvider provider = new ConnectionRateLimitProvider(1, 60);

        assertTrue(provider.authenticate(vinMessage("VIN001")).isPassed());
        assertTrue(provider.authenticate(vinMessage("VIN002")).isPassed());
    }

    @Test
    @Timeout(5)
    void shouldBeThreadSafe() throws InterruptedException {
        ConnectionRateLimitProvider provider = new ConnectionRateLimitProvider(50, 60);
        int threads = 10;
        int requestsPerThread = 100;
        java.util.concurrent.atomic.AtomicInteger passes = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger fails = new java.util.concurrent.atomic.AtomicInteger();

        Thread[] threadArray = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            threadArray[i] = new Thread(() -> {
                String vin = "VIN" + String.format("%03d", idx);
                for (int j = 0; j < requestsPerThread; j++) {
                    AuthProvider.AuthResult r = provider.authenticate(vinMessage(vin));
                    if (r.isPassed()) {
                        passes.incrementAndGet();
                    } else {
                        fails.incrementAndGet();
                    }
                }
            });
            threadArray[i].start();
        }

        for (Thread t : threadArray) {
            t.join();
        }

        assertEquals(threads * requestsPerThread, passes.get() + fails.get());
    }

    @Test
    void customLimitsShouldWork() {
        ConnectionRateLimitProvider provider = new ConnectionRateLimitProvider(5, 10);

        for (int i = 0; i < 5; i++) {
            assertTrue(provider.authenticate(vinMessage("VIN001")).isPassed());
        }

        AuthProvider.AuthResult result = provider.authenticate(vinMessage("VIN001"));
        assertFalse(result.isPassed());
    }

    private RawMessage vinMessage(String vin) {
        RawMessage msg = new RawMessage();
        msg.setVin(vin);
        return msg;
    }
}
