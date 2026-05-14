package io.github.xxx.gb32960.core.model;

import java.time.LocalDateTime;

public class HeartbeatMessage {

    private RawMessage raw;

    public HeartbeatMessage() {}

    public RawMessage getRaw() { return raw; }
    public void setRaw(RawMessage raw) { this.raw = raw; }

    @Override
    public String toString() {
        return "HeartbeatMessage{vin='" + (raw != null ? raw.getVin() : "null") + "'}";
    }
}
