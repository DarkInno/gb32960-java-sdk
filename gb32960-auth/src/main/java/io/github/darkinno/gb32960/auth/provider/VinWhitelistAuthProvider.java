package io.github.darkinno.gb32960.auth.provider;

import io.github.darkinno.gb32960.auth.api.AuthProvider;
import io.github.darkinno.gb32960.core.model.RawMessage;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class VinWhitelistAuthProvider implements AuthProvider {

    private final Set<String> whitelist = new CopyOnWriteArraySet<>();

    public void add(String vin) { whitelist.add(vin.trim()); }
    public void remove(String vin) { whitelist.remove(vin.trim()); }
    public void clear() { whitelist.clear(); }
    public boolean contains(String vin) { return whitelist.contains(vin.trim()); }

    @Override
    public AuthResult authenticate(RawMessage raw) {
        String vin = raw.getVin();
        if (vin == null || vin.isEmpty()) {
            return AuthResult.fail("VIN is empty");
        }
        if (!whitelist.contains(vin.trim())) {
            return AuthResult.fail("VIN not in whitelist: " + vin);
        }
        return AuthResult.success();
    }
}
