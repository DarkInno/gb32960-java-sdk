package io.github.xxx.gb32960.auth.api;

import io.github.xxx.gb32960.core.model.RawMessage;

public interface AuthProvider {

    AuthResult authenticate(RawMessage raw);

    class AuthResult {
        private final boolean passed;
        private final String reason;

        private AuthResult(boolean passed, String reason) {
            this.passed = passed;
            this.reason = reason;
        }

        public static AuthResult success() { return new AuthResult(true, null); }
        public static AuthResult fail(String reason) { return new AuthResult(false, reason); }

        public boolean isPassed() { return passed; }
        public String getReason() { return reason; }
    }
}
