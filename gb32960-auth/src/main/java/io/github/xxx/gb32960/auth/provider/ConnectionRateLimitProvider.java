package io.github.xxx.gb32960.auth.provider;

import io.github.xxx.gb32960.auth.api.AuthProvider;
import io.github.xxx.gb32960.core.model.RawMessage;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionRateLimitProvider implements AuthProvider {

    private final int maxAttemptsPerSecond;
    private final int banDurationSeconds;

    private final ConcurrentHashMap<String, BanEntry> banMap = new ConcurrentHashMap<>();

    public ConnectionRateLimitProvider() {
        this(3, 60);
    }

    public ConnectionRateLimitProvider(int maxAttemptsPerSecond, int banDurationSeconds) {
        this.maxAttemptsPerSecond = maxAttemptsPerSecond;
        this.banDurationSeconds = banDurationSeconds;
    }

    @Override
    public AuthResult authenticate(RawMessage raw) {
        String vin = raw.getVin();
        if (vin == null || vin.isEmpty()) {
            return AuthResult.fail("VIN is empty");
        }
        Instant now = Instant.now();

        BanEntry entry = banMap.get(vin);
        if (entry != null) {
            boolean banExpired = entry.bannedUntil == null || !now.isBefore(entry.bannedUntil);
            long elapsedMs = java.time.Duration.between(entry.windowStart, now).toMillis();
            if (banExpired && elapsedMs > 1000) {
                banMap.remove(vin);
            } else if (entry.bannedUntil != null && now.isBefore(entry.bannedUntil)) {
                return AuthResult.fail("Rate limit exceeded for VIN: " + vin);
            }
        }

        entry = banMap.compute(vin, (k, v) -> {
            if (v == null) {
                return new BanEntry(now);
            }
            if (v.bannedUntil != null) {
                if (now.isBefore(v.bannedUntil)) {
                    return v;
                }
                return new BanEntry(now);
            }
            long elapsedMs = java.time.Duration.between(v.windowStart, now).toMillis();
            if (elapsedMs > 1000) {
                return new BanEntry(now);
            }
            int currentCount = v.count.incrementAndGet();
            if (currentCount > maxAttemptsPerSecond) {
                v.bannedUntil = now.plusSeconds(banDurationSeconds);
            }
            return v;
        });

        if (entry != null && entry.bannedUntil != null && now.isBefore(entry.bannedUntil)) {
            return AuthResult.fail("Rate limit exceeded for VIN: " + vin);
        }
        return AuthResult.success();
    }

    private static class BanEntry {
        final Instant windowStart;
        final AtomicInteger count;
        volatile Instant bannedUntil;

        BanEntry(Instant windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
            this.bannedUntil = null;
        }
    }
}
