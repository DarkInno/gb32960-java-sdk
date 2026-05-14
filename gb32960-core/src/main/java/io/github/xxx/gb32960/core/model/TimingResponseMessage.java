package io.github.xxx.gb32960.core.model;

import java.time.LocalDateTime;

public class TimingResponseMessage {

    private LocalDateTime collectTime;
    private RawMessage raw;

    public TimingResponseMessage() {}

    public LocalDateTime getCollectTime() { return collectTime; }
    public void setCollectTime(LocalDateTime collectTime) { this.collectTime = collectTime; }

    public RawMessage getRaw() { return raw; }
    public void setRaw(RawMessage raw) { this.raw = raw; }

    @Override
    public String toString() {
        return "TimingResponseMessage{" +
                "vin='" + (raw != null ? raw.getVin() : "null") + '\'' +
                ", collectTime=" + collectTime +
                '}';
    }
}
