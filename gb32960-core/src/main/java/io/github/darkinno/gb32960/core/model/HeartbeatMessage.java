package io.github.darkinno.gb32960.core.model;

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
