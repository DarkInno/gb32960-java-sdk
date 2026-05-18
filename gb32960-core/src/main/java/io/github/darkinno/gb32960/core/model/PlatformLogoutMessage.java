package io.github.darkinno.gb32960.core.model;

import java.time.LocalDateTime;

public class PlatformLogoutMessage {

    private LocalDateTime collectTime;
    private int serialNumber;
    private RawMessage raw;

    public PlatformLogoutMessage() {}

    public LocalDateTime getCollectTime() { return collectTime; }
    public void setCollectTime(LocalDateTime collectTime) { this.collectTime = collectTime; }

    public int getSerialNumber() { return serialNumber; }
    public void setSerialNumber(int serialNumber) { this.serialNumber = serialNumber; }

    public RawMessage getRaw() { return raw; }
    public void setRaw(RawMessage raw) { this.raw = raw; }

    @Override
    public String toString() {
        return "PlatformLogoutMessage{" +
                "vin='" + (raw != null ? raw.getVin() : "null") + '\'' +
                ", time=" + collectTime +
                ", sn=" + serialNumber +
                '}';
    }
}
