package io.github.darkinno.gb32960.auth.provider;

import io.github.darkinno.gb32960.auth.api.AuthProvider;
import io.github.darkinno.gb32960.core.model.RawMessage;

import java.util.List;

public class CompositeAuthProvider implements AuthProvider {

    private final List<AuthProvider> providers;

    public CompositeAuthProvider(List<AuthProvider> providers) {
        this.providers = providers;
    }

    @Override
    public AuthResult authenticate(RawMessage raw) {
        for (AuthProvider provider : providers) {
            AuthResult result = provider.authenticate(raw);
            if (!result.isPassed()) {
                return result;
            }
        }
        return AuthResult.success();
    }
}
