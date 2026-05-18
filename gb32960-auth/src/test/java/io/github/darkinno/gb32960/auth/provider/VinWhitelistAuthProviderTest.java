package io.github.darkinno.gb32960.auth.provider;

import io.github.darkinno.gb32960.auth.api.AuthProvider;
import io.github.darkinno.gb32960.core.model.RawMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VinWhitelistAuthProviderTest {

    @Test
    void shouldPassForWhitelistedVin() {
        VinWhitelistAuthProvider provider = new VinWhitelistAuthProvider();
        provider.add("TESTVIN00000001");

        RawMessage msg = new RawMessage();
        msg.setVin("TESTVIN00000001");

        AuthProvider.AuthResult result = provider.authenticate(msg);
        assertTrue(result.isPassed());
    }

    @Test
    void shouldFailForNonWhitelistedVin() {
        VinWhitelistAuthProvider provider = new VinWhitelistAuthProvider();
        provider.add("TESTVIN00000001");

        RawMessage msg = new RawMessage();
        msg.setVin("TESTVIN00000002");

        AuthProvider.AuthResult result = provider.authenticate(msg);
        assertFalse(result.isPassed());
        assertTrue(result.getReason().contains("VIN not in whitelist"));
    }

    @Test
    void shouldFailForEmptyVin() {
        VinWhitelistAuthProvider provider = new VinWhitelistAuthProvider();

        RawMessage msg = new RawMessage();
        msg.setVin("");

        AuthProvider.AuthResult result = provider.authenticate(msg);
        assertFalse(result.isPassed());
        assertTrue(result.getReason().contains("VIN is empty"));
    }

    @Test
    void shouldFailForNullVin() {
        VinWhitelistAuthProvider provider = new VinWhitelistAuthProvider();

        RawMessage msg = new RawMessage();

        AuthProvider.AuthResult result = provider.authenticate(msg);
        assertFalse(result.isPassed());
    }

    @Test
    void shouldRemoveVinFromWhitelist() {
        VinWhitelistAuthProvider provider = new VinWhitelistAuthProvider();
        provider.add("TESTVIN00000001");
        assertTrue(provider.contains("TESTVIN00000001"));

        provider.remove("TESTVIN00000001");
        assertFalse(provider.contains("TESTVIN00000001"));
    }

    @Test
    void shouldClearWhitelist() {
        VinWhitelistAuthProvider provider = new VinWhitelistAuthProvider();
        provider.add("VIN1");
        provider.add("VIN2");
        assertTrue(provider.contains("VIN1"));
        assertTrue(provider.contains("VIN2"));

        provider.clear();
        assertFalse(provider.contains("VIN1"));
        assertFalse(provider.contains("VIN2"));
    }

    @Test
    void shouldTrimVinOnAdd() {
        VinWhitelistAuthProvider provider = new VinWhitelistAuthProvider();
        provider.add("  TESTVIN00000001 ");

        assertTrue(provider.contains("TESTVIN00000001"));
    }
}
