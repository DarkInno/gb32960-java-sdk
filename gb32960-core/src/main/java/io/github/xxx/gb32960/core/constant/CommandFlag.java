package io.github.xxx.gb32960.core.constant;

public final class CommandFlag {

    public static final byte VEHICLE_LOGIN      = 0x01;
    public static final byte REALTIME_REPORT    = 0x02;
    public static final byte REISSUE_REPORT     = 0x03;
    public static final byte VEHICLE_LOGOUT     = 0x04;
    public static final byte PLATFORM_LOGIN     = 0x05;
    public static final byte PLATFORM_LOGOUT    = 0x06;
    public static final byte HEARTBEAT          = 0x07;
    public static final byte TERMINAL_TIMING    = 0x08;

    private CommandFlag() {}

    public static String name(byte cmd) {
        return switch (cmd) {
            case VEHICLE_LOGIN    -> "VEHICLE_LOGIN";
            case REALTIME_REPORT  -> "REALTIME_REPORT";
            case REISSUE_REPORT   -> "REISSUE_REPORT";
            case VEHICLE_LOGOUT   -> "VEHICLE_LOGOUT";
            case PLATFORM_LOGIN   -> "PLATFORM_LOGIN";
            case PLATFORM_LOGOUT  -> "PLATFORM_LOGOUT";
            case HEARTBEAT        -> "HEARTBEAT";
            case TERMINAL_TIMING  -> "TERMINAL_TIMING";
            default               -> "UNKNOWN(" + String.format("0x%02X", cmd) + ")";
        };
    }
}
