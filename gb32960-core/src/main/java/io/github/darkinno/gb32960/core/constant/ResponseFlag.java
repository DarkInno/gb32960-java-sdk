package io.github.darkinno.gb32960.core.constant;

public final class ResponseFlag {

    public static final byte SUCCESS            = 0x01;
    public static final byte ERROR              = 0x02;
    public static final byte VIN_DUPLICATE      = 0x03;
    public static final byte COMMAND            = (byte) 0xFE;

    private ResponseFlag() {}

    public static String name(byte resp) {
        return switch (resp) {
            case SUCCESS        -> "SUCCESS";
            case ERROR          -> "ERROR";
            case VIN_DUPLICATE  -> "VIN_DUPLICATE";
            case COMMAND        -> "COMMAND";
            default             -> "UNKNOWN(" + String.format("0x%02X", resp) + ")";
        };
    }
}
