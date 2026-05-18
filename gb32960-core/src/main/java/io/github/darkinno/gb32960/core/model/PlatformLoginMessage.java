package io.github.darkinno.gb32960.core.model;

import java.time.LocalDateTime;

public class PlatformLoginMessage {

    private LocalDateTime collectTime;
    private int serialNumber;
    private String username;
    private String password;
    private int encryptionType;
    private RawMessage raw;

    public PlatformLoginMessage() {}

    public LocalDateTime getCollectTime() { return collectTime; }
    public void setCollectTime(LocalDateTime collectTime) { this.collectTime = collectTime; }

    public int getSerialNumber() { return serialNumber; }
    public void setSerialNumber(int serialNumber) { this.serialNumber = serialNumber; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getEncryptionType() { return encryptionType; }
    public void setEncryptionType(int encryptionType) { this.encryptionType = encryptionType; }

    public RawMessage getRaw() { return raw; }
    public void setRaw(RawMessage raw) { this.raw = raw; }

    @Override
    public String toString() {
        return "PlatformLoginMessage{" +
                "vin='" + (raw != null ? raw.getVin() : "null") + '\'' +
                ", time=" + collectTime +
                ", sn=" + serialNumber +
                ", username='" + username + '\'' +
                '}';
    }
}
