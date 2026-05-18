package io.github.darkinno.gb32960.auth.provider;

import io.github.darkinno.gb32960.auth.api.AuthProvider;
import io.github.darkinno.gb32960.core.model.RawMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoopAuthProviderTest {

    @Test
    void shouldAlwaysPass() {
        NoopAuthProvider provider = new NoopAuthProvider();

        RawMessage msg = new RawMessage();
        msg.setVin("TESTVIN00000001");

        AuthProvider.AuthResult result = provider.authenticate(msg);

        assertTrue(result.isPassed());
        assertNull(result.getReason());
    }

    @Test
    void shouldPassEvenWithNullVin() {
        NoopAuthProvider provider = new NoopAuthProvider();
        RawMessage msg = new RawMessage();

        AuthProvider.AuthResult result = provider.authenticate(msg);

        assertTrue(result.isPassed());
    }
}
