package io.github.xxx.gb32960.core.codec;

import io.github.xxx.gb32960.core.constant.CommandFlag;
import io.github.xxx.gb32960.core.constant.InfoType;
import io.github.xxx.gb32960.core.model.*;
import io.github.xxx.gb32960.core.util.BccUtil;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class MessageDecoder {

    private MessageDecoder() {}

    public static RawMessage decodeRaw(byte[] bytes) {
        if (bytes == null || bytes.length < 25) {
            throw new DecodeException("Message too short, minimum 25 bytes");
        }
        if (bytes[0] != 0x23 || bytes[1] != 0x23) {
            throw new DecodeException("Invalid start marker");
        }

        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.position(2);

        byte cmd = buf.get();
        byte resp = buf.get();

        byte[] vinBytes = new byte[17];
        buf.get(vinBytes);
        String vin = new String(vinBytes, StandardCharsets.US_ASCII).trim();

        byte enc = buf.get();
        int dataLen = buf.getShort() & 0xFFFF;

        if (dataLen > 0xFFFF) {
            throw new DecodeException("Data length overflow: " + dataLen);
        }

        int headerEnd = 24;
        int totalExpected = headerEnd + dataLen + 1;
        if (bytes.length < totalExpected) {
            throw new DecodeException("Incomplete message: expected " + totalExpected + " bytes, got " + bytes.length);
        }

        byte[] dataUnit = null;
        if (dataLen > 0) {
            dataUnit = new byte[dataLen];
            buf.get(dataUnit);
        }

        byte bcc = buf.get();
        byte expectedBcc = BccUtil.calculate(bytes, 2, headerEnd - 2 + dataLen);
        if (bcc != expectedBcc) {
            throw new DecodeException("BCC mismatch: expected 0x" + String.format("%02X", expectedBcc)
                    + ", got 0x" + String.format("%02X", bcc));
        }

        return RawMessage.builder()
                .commandFlag(cmd)
                .responseFlag(resp)
                .vin(vin)
                .encryptionType(enc)
                .dataLength(dataLen)
                .dataUnit(dataUnit)
                .bcc(bcc)
                .rawBytes(bytes)
                .build();
    }

    public static Object decode(RawMessage raw) {
        return switch (raw.getCommandFlag()) {
            case CommandFlag.VEHICLE_LOGIN   -> decodeVehicleLogin(raw);
            case CommandFlag.REALTIME_REPORT -> decodeRealtimeData(raw);
            case CommandFlag.REISSUE_REPORT  -> decodeRealtimeData(raw);
            case CommandFlag.VEHICLE_LOGOUT  -> decodeVehicleLogout(raw);
            case CommandFlag.HEARTBEAT       -> decodeHeartbeat(raw);
            case CommandFlag.PLATFORM_LOGIN  -> decodePlatformLogin(raw);
            case CommandFlag.PLATFORM_LOGOUT -> decodePlatformLogout(raw);
            case CommandFlag.TERMINAL_TIMING -> decodeTimingResponse(raw);
            default -> throw new DecodeException("Unknown command flag: 0x" + String.format("%02X", raw.getCommandFlag()));
        };
    }

    public static VehicleLoginMessage decodeVehicleLogin(RawMessage raw) {
        if (raw.getDataUnit() == null) {
            throw new DecodeException("Empty data unit for vehicle login");
        }
        ByteBuffer buf = ByteBuffer.wrap(raw.getDataUnit());

        VehicleLoginMessage msg = new VehicleLoginMessage();
        msg.setCollectTime(readBcdTime(buf));
        msg.setSerialNumber(buf.getShort() & 0xFFFF);

        byte[] iccidBytes = new byte[20];
        buf.get(iccidBytes);
        msg.setIccid(new String(iccidBytes, StandardCharsets.US_ASCII).trim());

        msg.setBatterySubsystemCount(buf.get() & 0xFF);
        msg.setBatterySubsystemCodeLength(buf.getShort() & 0xFFFF);
        msg.setRaw(raw);
        return msg;
    }

    public static VehicleLogoutMessage decodeVehicleLogout(RawMessage raw) {
        if (raw.getDataUnit() == null) {
            throw new DecodeException("Empty data unit for vehicle logout");
        }
        ByteBuffer buf = ByteBuffer.wrap(raw.getDataUnit());

        VehicleLogoutMessage msg = new VehicleLogoutMessage();
        msg.setCollectTime(readBcdTime(buf));
        msg.setSerialNumber(buf.getShort() & 0xFFFF);
        msg.setRaw(raw);
        return msg;
    }

    public static HeartbeatMessage decodeHeartbeat(RawMessage raw) {
        HeartbeatMessage msg = new HeartbeatMessage();
        msg.setRaw(raw);
        return msg;
    }

    public static PlatformLoginMessage decodePlatformLogin(RawMessage raw) {
        if (raw.getDataUnit() == null) {
            throw new DecodeException("Empty data unit for platform login");
        }
        ByteBuffer buf = ByteBuffer.wrap(raw.getDataUnit());

        PlatformLoginMessage msg = new PlatformLoginMessage();
        msg.setCollectTime(readBcdTime(buf));
        msg.setSerialNumber(buf.getShort() & 0xFFFF);

        byte[] userBytes = new byte[12];
        buf.get(userBytes);
        msg.setUsername(new String(userBytes, StandardCharsets.US_ASCII).trim());

        byte[] passBytes = new byte[20];
        buf.get(passBytes);
        msg.setPassword(new String(passBytes, StandardCharsets.US_ASCII).trim());

        msg.setEncryptionType(buf.get() & 0xFF);
        msg.setRaw(raw);
        return msg;
    }

    public static PlatformLogoutMessage decodePlatformLogout(RawMessage raw) {
        if (raw.getDataUnit() == null) {
            throw new DecodeException("Empty data unit for platform logout");
        }
        ByteBuffer buf = ByteBuffer.wrap(raw.getDataUnit());

        PlatformLogoutMessage msg = new PlatformLogoutMessage();
        msg.setCollectTime(readBcdTime(buf));
        msg.setSerialNumber(buf.getShort() & 0xFFFF);
        msg.setRaw(raw);
        return msg;
    }

    public static TimingResponseMessage decodeTimingResponse(RawMessage raw) {
        TimingResponseMessage msg = new TimingResponseMessage();
        msg.setRaw(raw);
        if (raw.getDataUnit() != null) {
            ByteBuffer buf = ByteBuffer.wrap(raw.getDataUnit());
            msg.setCollectTime(readBcdTime(buf));
        }
        return msg;
    }

    public static RealtimeDataMessage decodeRealtimeData(RawMessage raw) {
        if (raw.getDataUnit() == null) {
            throw new DecodeException("Empty data unit for realtime data");
        }
        ByteBuffer buf = ByteBuffer.wrap(raw.getDataUnit());

        RealtimeDataMessage msg = new RealtimeDataMessage();
        msg.setCollectTime(readBcdTime(buf));
        msg.setRaw(raw);

        while (buf.hasRemaining()) {
            int infoType = buf.get() & 0xFF;
            switch (infoType) {
                case InfoType.VEHICLE_DATA:
                    msg.setVehicleData(decodeVehicleData(buf));
                    break;
                case InfoType.DRIVE_MOTOR_DATA:
                    msg.getDriveMotorDataList().add(decodeDriveMotorData(buf));
                    break;
                case InfoType.FUEL_CELL_DATA:
                    msg.setFuelCellData(decodeFuelCellData(buf));
                    break;
                case InfoType.ENGINE_DATA:
                    msg.setEngineData(decodeEngineData(buf));
                    break;
                case InfoType.POSITION_DATA:
                    msg.setPositionData(decodePositionData(buf));
                    break;
                case InfoType.EXTREMUM_DATA:
                    msg.setExtremumData(decodeExtremumData(buf));
                    break;
                case InfoType.ALARM_DATA:
                    msg.setAlarmData(decodeAlarmData(buf));
                    break;
                case InfoType.BATTERY_VOLTAGE_DATA:
                    msg.getBatteryVoltageDataList().add(decodeBatteryVoltageData(buf));
                    break;
                case InfoType.BATTERY_TEMPERATURE_DATA:
                    msg.getBatteryTemperatureDataList().add(decodeBatteryTemperatureData(buf));
                    break;
                default:
                    break;
            }
        }
        return msg;
    }

    private static VehicleData decodeVehicleData(ByteBuffer buf) {
        VehicleData vd = new VehicleData();
        vd.setStatus(buf.get() & 0xFF);
        vd.setChargeStatus(buf.get() & 0xFF);
        vd.setRunMode(buf.get() & 0xFF);
        vd.setSpeed((buf.getShort() & 0xFFFF) / 10.0);
        vd.setOdometer((buf.getInt() & 0xFFFFFFFFL) / 10.0);
        vd.setTotalVoltage((buf.getShort() & 0xFFFF) / 1.0);
        vd.setTotalCurrent(((buf.getShort() & 0xFFFF) - 1000.0) / 1.0);
        vd.setSoc(buf.get() & 0xFF);
        vd.setDcDcStatus(buf.get() & 0xFF);
        vd.setGearPosition(buf.get() & 0xFF);
        vd.setInsulationResistance(buf.getShort() & 0xFFFF);
        vd.setAcceleratorPedal(buf.get() & 0xFF);
        vd.setBrakePedal(buf.get() & 0xFF);
        buf.getShort(); // reserved
        return vd;
    }

    private static DriveMotorData decodeDriveMotorData(ByteBuffer buf) {
        DriveMotorData dmd = new DriveMotorData();
        int motorCount = buf.get() & 0xFF;
        dmd.setMotorCount(motorCount);

        for (int i = 0; i < motorCount; i++) {
            DriveMotorData.MotorInfo mi = new DriveMotorData.MotorInfo();
            mi.setSerial(buf.get() & 0xFF);
            mi.setStatus(buf.get() & 0xFF);
            mi.setControllerTemperature((buf.get() & 0xFF) - 40);
            mi.setSpeed((buf.getShort() & 0xFFFF));
            mi.setTorque(((buf.getShort() & 0xFFFF) - 2000.0) / 10.0);
            mi.setMotorTemperature((buf.get() & 0xFF) - 40);
            mi.setControllerInputVoltage((buf.getShort() & 0xFFFF) / 1.0);
            mi.setDcBusCurrent(((buf.getShort() & 0xFFFF) - 1000.0) / 1.0);
            dmd.getMotors().add(mi);
        }
        return dmd;
    }

    private static FuelCellData decodeFuelCellData(ByteBuffer buf) {
        FuelCellData fcd = new FuelCellData();
        fcd.setVoltage((buf.getShort() & 0xFFFF) / 10.0);
        fcd.setCurrent((buf.getShort() & 0xFFFF) / 10.0);
        fcd.setFuelConsumptionRate((buf.getShort() & 0xFFFF) / 100.0);

        int probeCount = buf.getShort() & 0xFFFF;
        fcd.setProbeCount(probeCount);
        for (int i = 0; i < probeCount; i++) {
            fcd.getProbeTemperatures().add((buf.get() & 0xFF) - 40);
        }

        fcd.setHydrogenMaxPressure(buf.getShort() & 0xFFFF);
        fcd.setHydrogenPressure(buf.getShort() & 0xFFFF);
        fcd.setHydrogenMaxTemperature((buf.getShort() & 0xFFFF) - 40);
        fcd.setHydrogenTemperature((buf.getShort() & 0xFFFF) - 40);
        return fcd;
    }

    private static EngineData decodeEngineData(ByteBuffer buf) {
        EngineData ed = new EngineData();
        ed.setStatus(buf.get() & 0xFF);
        ed.setSpeed(buf.getShort() & 0xFFFF);
        ed.setFuelRate((buf.getShort() & 0xFFFF) / 100.0);
        return ed;
    }

    private static PositionData decodePositionData(ByteBuffer buf) {
        PositionData pd = new PositionData();
        pd.setLongitude((buf.getInt() & 0xFFFFFFFFL) / 1_000_000.0);
        pd.setLatitude((buf.getInt() & 0xFFFFFFFFL) / 1_000_000.0);
        pd.setSpeed((buf.getShort() & 0xFFFF) / 10.0);
        pd.setDirection(buf.getShort() & 0xFFFF);
        pd.setValid(buf.get() == 0);
        return pd;
    }

    private static ExtremumData decodeExtremumData(ByteBuffer buf) {
        ExtremumData ed = new ExtremumData();
        ed.setMaxBatteryVoltageSubsystem(buf.get() & 0xFF);
        ed.setMaxBatteryVoltageCell(buf.get() & 0xFF);
        ed.setMaxBatteryVoltage(buf.getShort() & 0xFFFF);
        ed.setMinBatteryVoltageSubsystem(buf.get() & 0xFF);
        ed.setMinBatteryVoltageCell(buf.get() & 0xFF);
        ed.setMinBatteryVoltage(buf.getShort() & 0xFFFF);
        ed.setMaxBatteryTemperatureSubsystem(buf.get() & 0xFF);
        ed.setMaxBatteryTemperatureProbe(buf.get() & 0xFF);
        ed.setMaxBatteryTemperature((buf.get() & 0xFF) - 40);
        ed.setMinBatteryTemperatureSubsystem(buf.get() & 0xFF);
        ed.setMinBatteryTemperatureProbe(buf.get() & 0xFF);
        ed.setMinBatteryTemperature((buf.get() & 0xFF) - 40);
        return ed;
    }

    private static AlarmData decodeAlarmData(ByteBuffer buf) {
        AlarmData ad = new AlarmData();
        ad.setMaxAlarmLevel(buf.get() & 0xFF);
        ad.setGeneralAlarmFlags(buf.getInt() & 0xFFFFFFFFL);

        int generalCount = buf.get() & 0xFF;
        for (int i = 0; i < generalCount; i++) {
            ad.getGeneralAlarmCodes().add(buf.getInt() & 0xFFFFFFFFL);
        }

        ad.setBatteryAlarmFlags(buf.getInt() & 0xFFFFFFFFL);
        int batteryCount = buf.get() & 0xFF;
        for (int i = 0; i < batteryCount; i++) {
            ad.getBatteryAlarmCodes().add(buf.getInt() & 0xFFFFFFFFL);
        }

        ad.setDriveMotorAlarmFlags(buf.getInt() & 0xFFFFFFFFL);
        int motorCount = buf.get() & 0xFF;
        for (int i = 0; i < motorCount; i++) {
            ad.getDriveMotorAlarmCodes().add(buf.getInt() & 0xFFFFFFFFL);
        }

        ad.setEngineAlarmFlags(buf.getInt() & 0xFFFFFFFFL);
        int engineCount = buf.get() & 0xFF;
        for (int i = 0; i < engineCount; i++) {
            ad.getEngineAlarmCodes().add(buf.getInt() & 0xFFFFFFFFL);
        }

        ad.setOtherAlarmFlags(buf.getInt() & 0xFFFFFFFFL);
        return ad;
    }

    private static BatteryVoltageData decodeBatteryVoltageData(ByteBuffer buf) {
        BatteryVoltageData bvd = new BatteryVoltageData();
        int subsystemCount = buf.get() & 0xFF;
        bvd.setSubsystemCount(subsystemCount);

        for (int i = 0; i < subsystemCount; i++) {
            BatteryVoltageData.Subsystem sub = new BatteryVoltageData.Subsystem();
            sub.setSubsystemNumber(buf.get() & 0xFF);
            int cellCount = buf.getShort() & 0xFFFF;
            sub.setCellCount(cellCount);
            for (int j = 0; j < cellCount; j++) {
                sub.getCellVoltages().add((buf.getShort() & 0xFFFF) / 1.0);
            }
            bvd.getSubsystems().add(sub);
        }
        return bvd;
    }

    private static BatteryTemperatureData decodeBatteryTemperatureData(ByteBuffer buf) {
        BatteryTemperatureData btd = new BatteryTemperatureData();
        int subsystemCount = buf.get() & 0xFF;
        btd.setSubsystemCount(subsystemCount);

        for (int i = 0; i < subsystemCount; i++) {
            BatteryTemperatureData.Subsystem sub = new BatteryTemperatureData.Subsystem();
            sub.setSubsystemNumber(buf.get() & 0xFF);
            int probeCount = buf.getShort() & 0xFFFF;
            sub.setProbeCount(probeCount);
            for (int j = 0; j < probeCount; j++) {
                sub.getProbeTemperatures().add((buf.get() & 0xFF) - 40);
            }
            btd.getSubsystems().add(sub);
        }
        return btd;
    }

    private static LocalDateTime readBcdTime(ByteBuffer buf) {
        int year = bcdToInt(buf.get()) + 2000;
        int month = bcdToInt(buf.get());
        int day = bcdToInt(buf.get());
        int hour = bcdToInt(buf.get());
        int minute = bcdToInt(buf.get());
        int second = bcdToInt(buf.get());
        try {
            return LocalDateTime.of(year, month, day, hour, minute, second);
        } catch (Exception e) {
            throw new DecodeException("Invalid BCD time: " + year + "-" + month + "-" + day
                    + " " + hour + ":" + minute + ":" + second);
        }
    }

    private static int bcdToInt(byte bcd) {
        return ((bcd >> 4) & 0x0F) * 10 + (bcd & 0x0F);
    }

    public static class DecodeException extends RuntimeException {
        public DecodeException(String message) {
            super(message);
        }
    }
}
