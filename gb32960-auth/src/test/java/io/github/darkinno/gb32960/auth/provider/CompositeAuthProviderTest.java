package io.github.darkinno.gb32960.auth.provider;

import io.github.darkinno.gb32960.auth.api.AuthProvider;
import io.github.darkinno.gb32960.core.model.RawMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositeAuthProviderTest {

    @Test
    void shouldPassWhenAllProvidersPass() {
        CompositeAuthProvider provider = new CompositeAuthProvider(List.of(
                new NoopAuthProvider(),
                new NoopAuthProvider()
        ));

        RawMessage msg = new RawMessage();
        msg.setVin("TESTVIN00000001");

        AuthProvider.AuthResult result = provider.authenticate(msg);
        assertTrue(result.isPassed());
    }

    @Test
    void shouldFailOnFirstFailingProvider() {
        VinWhitelistAuthProvider whitelist = new VinWhitelistAuthProvider();
        whitelist.add("ALLOWED");

        CompositeAuthProvider provider = new CompositeAuthProvider(List.of(
                whitelist,
                new NoopAuthProvider()
        ));

        RawMessage msg = new RawMessage();
        msg.setVin("BLOCKED");

        AuthProvider.AuthResult result = provider.authenticate(msg);
        assertFalse(result.isPassed());
        assertTrue(result.getReason().contains("VIN not in whitelist"));
    }

    @Test
    void shouldChainProvidersInOrder() {
        VinWhitelistAuthProvider first = new VinWhitelistAuthProvider();
        first.add("VIN001");
        VinWhitelistAuthProvider second = new VinWhitelistAuthProvider();

        CompositeAuthProvider provider = new CompositeAuthProvider(List.of(first, second));

        RawMessage msg = new RawMessage();
        msg.setVin("VIN001");

        AuthProvider.AuthResult result = provider.authenticate(msg);
        assertFalse(result.isPassed());
        assertTrue(result.getReason().contains("VIN not in whitelist"));
    }

    @Test
    void emptyProvidersShouldPass() {
        CompositeAuthProvider provider = new CompositeAuthProvider(List.of());

        RawMessage msg = new RawMessage();
        msg.setVin("ANYVIN");

        AuthProvider.AuthResult result = provider.authenticate(msg);
        assertTrue(result.isPassed());
    }
}
