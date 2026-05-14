package io.github.xxx.gb32960.core.model;

import java.time.LocalDateTime;

public class VehicleLoginMessage {

    private LocalDateTime collectTime;
    private int serialNumber;
    private String iccid;
    private int batterySubsystemCount;
    private int batterySubsystemCodeLength;
    private RawMessage raw;

    public VehicleLoginMessage() {}

    public LocalDateTime getCollectTime() { return collectTime; }
    public void setCollectTime(LocalDateTime collectTime) { this.collectTime = collectTime; }

    public int getSerialNumber() { return serialNumber; }
    public void setSerialNumber(int serialNumber) { this.serialNumber = serialNumber; }

    public String getIccid() { return iccid; }
    public void setIccid(String iccid) { this.iccid = iccid; }

    public int getBatterySubsystemCount() { return batterySubsystemCount; }
    public void setBatterySubsystemCount(int batterySubsystemCount) { this.batterySubsystemCount = batterySubsystemCount; }

    public int getBatterySubsystemCodeLength() { return batterySubsystemCodeLength; }
    public void setBatterySubsystemCodeLength(int batterySubsystemCodeLength) { this.batterySubsystemCodeLength = batterySubsystemCodeLength; }

    public RawMessage getRaw() { return raw; }
    public void setRaw(RawMessage raw) { this.raw = raw; }

    @Override
    public String toString() {
        return "VehicleLoginMessage{" +
                "vin='" + (raw != null ? raw.getVin() : "null") + '\'' +
                ", time=" + collectTime +
                ", sn=" + serialNumber +
                ", iccid='" + iccid + '\'' +
                '}';
    }
}
