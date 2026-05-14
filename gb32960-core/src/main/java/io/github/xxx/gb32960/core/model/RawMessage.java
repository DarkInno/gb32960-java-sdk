package io.github.xxx.gb32960.core.model;

import java.util.Arrays;

public class RawMessage {

    public static final byte[] START_MARKER = {0x23, 0x23};

    private byte commandFlag;
    private byte responseFlag;
    private String vin;
    private byte encryptionType;
    private int dataLength;
    private byte[] dataUnit;
    private byte bcc;
    private byte[] rawBytes;

    public RawMessage() {}

    private RawMessage(Builder builder) {
        this.commandFlag = builder.commandFlag;
        this.responseFlag = builder.responseFlag;
        this.vin = builder.vin;
        this.encryptionType = builder.encryptionType;
        this.dataLength = builder.dataLength;
        this.dataUnit = builder.dataUnit;
        this.bcc = builder.bcc;
        this.rawBytes = builder.rawBytes;
    }

    public boolean isCommand() {
        return (responseFlag & 0xFF) == 0xFE;
    }

    public boolean isSuccess() {
        return (responseFlag & 0xFF) == 0x01;
    }

    public boolean isError() {
        return (responseFlag & 0xFF) == 0x02;
    }

    public byte getCommandFlag() { return commandFlag; }
    public void setCommandFlag(byte commandFlag) { this.commandFlag = commandFlag; }

    public byte getResponseFlag() { return responseFlag; }
    public void setResponseFlag(byte responseFlag) { this.responseFlag = responseFlag; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public byte getEncryptionType() { return encryptionType; }
    public void setEncryptionType(byte encryptionType) { this.encryptionType = encryptionType; }

    public int getDataLength() { return dataLength; }
    public void setDataLength(int dataLength) { this.dataLength = dataLength; }

    public byte[] getDataUnit() { return dataUnit; }
    public void setDataUnit(byte[] dataUnit) { this.dataUnit = dataUnit; }

    public byte getBcc() { return bcc; }
    public void setBcc(byte bcc) { this.bcc = bcc; }

    public byte[] getRawBytes() { return rawBytes; }
    public void setRawBytes(byte[] rawBytes) { this.rawBytes = rawBytes; }

    @Override
    public String toString() {
        return "RawMessage{" +
                "cmd=" + String.format("0x%02X", commandFlag) +
                ", resp=" + String.format("0x%02X", responseFlag) +
                ", vin='" + vin + '\'' +
                ", enc=" + String.format("0x%02X", encryptionType) +
                ", len=" + dataLength +
                ", bcc=" + String.format("0x%02X", bcc) +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private byte commandFlag;
        private byte responseFlag;
        private String vin;
        private byte encryptionType;
        private int dataLength;
        private byte[] dataUnit;
        private byte bcc;
        private byte[] rawBytes;

        public Builder commandFlag(byte commandFlag) { this.commandFlag = commandFlag; return this; }
        public Builder responseFlag(byte responseFlag) { this.responseFlag = responseFlag; return this; }
        public Builder vin(String vin) { this.vin = vin; return this; }
        public Builder encryptionType(byte encryptionType) { this.encryptionType = encryptionType; return this; }
        public Builder dataLength(int dataLength) { this.dataLength = dataLength; return this; }
        public Builder dataUnit(byte[] dataUnit) { this.dataUnit = dataUnit; return this; }
        public Builder bcc(byte bcc) { this.bcc = bcc; return this; }
        public Builder rawBytes(byte[] rawBytes) { this.rawBytes = rawBytes; return this; }

        public RawMessage build() {
            return new RawMessage(this);
        }
    }
}
