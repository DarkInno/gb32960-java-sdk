package io.github.darkinno.gb32960.core.constant;

public final class EncryptionType {

    public static final byte NONE   = 0x01;
    public static final byte RSA    = 0x02;
    public static final byte AES128 = 0x03;
    public static final byte ABNORMAL = (byte) 0xFE;
    public static final byte INVALID = (byte) 0xFF;

    private EncryptionType() {}

    public static String name(byte enc) {
        return switch (enc) {
            case NONE      -> "NONE";
            case RSA       -> "RSA";
            case AES128    -> "AES128";
            case ABNORMAL  -> "ABNORMAL";
            case INVALID   -> "INVALID";
            default        -> "UNKNOWN(" + String.format("0x%02X", enc) + ")";
        };
    }
}
