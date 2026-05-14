package io.github.xxx.gb32960.core.util;

import io.github.xxx.gb32960.core.constant.CommandFlag;
import io.github.xxx.gb32960.core.constant.InfoType;
import io.github.xxx.gb32960.core.constant.ResponseFlag;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class TestMessageBuilder {

    private TestMessageBuilder() {}

    public static byte[] buildVehicleLogin(String vin, int serialNumber, String iccid,
                                            int batteryCount, int codeLength) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        writeBcdTime(data);
        data.write((serialNumber >> 8) & 0xFF);
        data.write(serialNumber & 0xFF);
        writeFixedString(data, iccid, 20);
        data.write(batteryCount & 0xFF);
        data.write((codeLength >> 8) & 0xFF);
        data.write(codeLength & 0xFF);
        return buildMessage(CommandFlag.VEHICLE_LOGIN, ResponseFlag.COMMAND, vin, data.toByteArray());
    }

    public static byte[] buildVehicleLogout(String vin, int serialNumber) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        writeBcdTime(data);
        data.write((serialNumber >> 8) & 0xFF);
        data.write(serialNumber & 0xFF);
        return buildMessage(CommandFlag.VEHICLE_LOGOUT, ResponseFlag.COMMAND, vin, data.toByteArray());
    }

    public static byte[] buildHeartbeat(String vin) {
        return buildMessage(CommandFlag.HEARTBEAT, ResponseFlag.COMMAND, vin, new byte[0]);
    }

    public static byte[] buildRealtimeData(String vin, double speed, int soc, double lng, double lat) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        writeBcdTime(data);
        data.write(InfoType.VEHICLE_DATA);
        writeVehicleData(data, speed, soc);
        data.write(InfoType.POSITION_DATA);
        writePositionData(data, lng, lat);
        return buildMessage(CommandFlag.REALTIME_REPORT, ResponseFlag.COMMAND, vin, data.toByteArray());
    }

    public static byte[] buildRealtimeDataFull(String vin, double speed, int soc, double odometer,
                                                double voltage, double current, double lng, double lat,
                                                int motorSpeed, int motorTemp) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        writeBcdTime(data);
        data.write(InfoType.VEHICLE_DATA);
        writeVehicleDataFull(data, speed, soc, odometer, voltage, current);
        data.write(InfoType.DRIVE_MOTOR_DATA);
        writeDriveMotorData(data, motorSpeed, motorTemp);
        data.write(InfoType.POSITION_DATA);
        writePositionData(data, lng, lat);
        data.write(InfoType.EXTREMUM_DATA);
        writeExtremumData(data);
        data.write(InfoType.ALARM_DATA);
        writeAlarmData(data);
        return buildMessage(CommandFlag.REALTIME_REPORT, ResponseFlag.COMMAND, vin, data.toByteArray());
    }

    public static byte[] buildPlatformLogin(String username, String password) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        writeBcdTime(data);
        data.write(0x00);
        data.write(0x01);
        writeFixedString(data, username, 12);
        writeFixedString(data, password, 20);
        data.write(0x01);
        return buildMessage(CommandFlag.PLATFORM_LOGIN, ResponseFlag.COMMAND, "platform        ", data.toByteArray());
    }

    public static byte[] buildTimingResponse(String vin) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        writeBcdTime(data);
        return buildMessage(CommandFlag.TERMINAL_TIMING, ResponseFlag.SUCCESS, vin, data.toByteArray());
    }

    public static byte[] buildMessage(byte cmd, byte resp, String vin, byte[] dataUnit) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(0x23);
        baos.write(0x23);
        baos.write(cmd);
        baos.write(resp);
        byte[] vinBytes = padVin(vin);
        baos.write(vinBytes, 0, vinBytes.length);
        baos.write(0x01);
        baos.write((dataUnit.length >> 8) & 0xFF);
        baos.write(dataUnit.length & 0xFF);
        baos.write(dataUnit, 0, dataUnit.length);
        byte[] full = baos.toByteArray();
        byte bcc = BccUtil.calculate(full, 2, full.length - 2);
        baos.write(bcc);
        return baos.toByteArray();
    }

    private static void writeBcdTime(ByteArrayOutputStream baos) {
        LocalDateTime now = LocalDateTime.now();
        baos.write(intToBcd(now.getYear() % 100));
        baos.write(intToBcd(now.getMonthValue()));
        baos.write(intToBcd(now.getDayOfMonth()));
        baos.write(intToBcd(now.getHour()));
        baos.write(intToBcd(now.getMinute()));
        baos.write(intToBcd(now.getSecond()));
    }

    private static void writeVehicleData(ByteArrayOutputStream baos, double speed, int soc) {
        writeVehicleDataFull(baos, speed, soc, 0, 400, -50);
    }

    private static void writeVehicleDataFull(ByteArrayOutputStream baos, double speed, int soc,
                                              double odometer, double voltage, double current) {
        int speedRaw = (int) (speed * 10);
        long odoRaw = (long) (odometer * 10);
        int voltRaw = (int) voltage;
        int currRaw = (int) (current + 1000);

        baos.write(0x01);
        baos.write(0x01);
        baos.write(0x01);
        baos.write((speedRaw >> 8) & 0xFF);
        baos.write(speedRaw & 0xFF);
        baos.write((int) ((odoRaw >> 24) & 0xFF));
        baos.write((int) ((odoRaw >> 16) & 0xFF));
        baos.write((int) ((odoRaw >> 8) & 0xFF));
        baos.write((int) (odoRaw & 0xFF));
        baos.write((voltRaw >> 8) & 0xFF);
        baos.write(voltRaw & 0xFF);
        baos.write((currRaw >> 8) & 0xFF);
        baos.write(currRaw & 0xFF);
        baos.write(soc & 0xFF);
        baos.write(0x01);
        baos.write(0x00);
        baos.write(0x27); baos.write(0x10);
        baos.write(0x00);
        baos.write(0x00);
        baos.write(0x00); baos.write(0x00);
    }

    private static void writeDriveMotorData(ByteArrayOutputStream baos, int speed, int temp) {
        baos.write(0x01);
        baos.write(0x01);
        baos.write(0x01);
        baos.write((temp + 40) & 0xFF);
        baos.write((speed >> 8) & 0xFF);
        baos.write(speed & 0xFF);
        baos.write(0x07); baos.write((byte) 0xD0);
        baos.write((temp + 40) & 0xFF);
        baos.write(0x00); baos.write((byte) 0xC8);
        baos.write((byte) 0xFC); baos.write(0x18);
    }

    private static void writePositionData(ByteArrayOutputStream baos, double lng, double lat) {
        int lngRaw = (int) (lng * 1_000_000);
        int latRaw = (int) (lat * 1_000_000);
        baos.write((lngRaw >> 24) & 0xFF);
        baos.write((lngRaw >> 16) & 0xFF);
        baos.write((lngRaw >> 8) & 0xFF);
        baos.write(lngRaw & 0xFF);
        baos.write((latRaw >> 24) & 0xFF);
        baos.write((latRaw >> 16) & 0xFF);
        baos.write((latRaw >> 8) & 0xFF);
        baos.write(latRaw & 0xFF);
        baos.write(0x00); baos.write(0x46); baos.write(0x00); baos.write(0x5A); baos.write(0x00);
    }

    private static void writeExtremumData(ByteArrayOutputStream baos) {
        baos.write(0x01); baos.write(0x01);
        baos.write(0x0B); baos.write((byte) 0xB8);
        baos.write(0x01); baos.write(0x01);
        baos.write(0x0B); baos.write(0x54);
        baos.write(0x01); baos.write(0x01);
        baos.write(0x32);
        baos.write(0x01); baos.write(0x01);
        baos.write(0x19);
    }

    private static void writeAlarmData(ByteArrayOutputStream baos) {
        baos.write(0x00);
        baos.write(0); baos.write(0); baos.write(0); baos.write(0);
        baos.write(0x00);
        baos.write(0); baos.write(0); baos.write(0); baos.write(0);
        baos.write(0x00);
        baos.write(0); baos.write(0); baos.write(0); baos.write(0);
        baos.write(0x00);
        baos.write(0); baos.write(0); baos.write(0); baos.write(0);
        baos.write(0x00);
        baos.write(0); baos.write(0); baos.write(0); baos.write(0);
    }

    private static byte[] padVin(String vin) {
        byte[] result = new byte[17];
        byte[] bytes = (vin != null ? vin : "").getBytes(StandardCharsets.US_ASCII);
        int len = Math.min(bytes.length, 17);
        System.arraycopy(bytes, 0, result, 0, len);
        return result;
    }

    private static void writeFixedString(ByteArrayOutputStream baos, String s, int length) {
        byte[] result = new byte[length];
        byte[] bytes = (s != null ? s : "").getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, result, 0, Math.min(bytes.length, length));
        baos.write(result, 0, result.length);
    }

    private static byte intToBcd(int value) {
        return (byte) (((value / 10) << 4) | (value % 10));
    }
}
