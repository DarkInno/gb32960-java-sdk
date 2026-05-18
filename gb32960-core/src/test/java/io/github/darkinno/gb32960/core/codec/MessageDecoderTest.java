package io.github.darkinno.gb32960.core.codec;

import io.github.darkinno.gb32960.core.constant.CommandFlag;
import io.github.darkinno.gb32960.core.constant.ResponseFlag;
import io.github.darkinno.gb32960.core.model.*;
import io.github.darkinno.gb32960.core.util.TestMessageBuilder;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class MessageDecoderTest {

    private static final String VIN = "LSVAM40E7GA000001";

    @Test
    void shouldDecodeRawVehicleLogin() {
        byte[] bytes = TestMessageBuilder.buildVehicleLogin(VIN, 1, "89860000000000000001", 1, 4);
        RawMessage raw = MessageDecoder.decodeRaw(bytes);

        assertEquals(CommandFlag.VEHICLE_LOGIN, raw.getCommandFlag());
        assertEquals(ResponseFlag.COMMAND, raw.getResponseFlag());
        assertEquals(VIN, raw.getVin());
        assertEquals(0x01, raw.getEncryptionType());
        assertTrue(raw.isCommand());
        assertNotNull(raw.getRawBytes());
    }

    @Test
    void shouldDecodeTypedVehicleLogin() {
        byte[] bytes = TestMessageBuilder.buildVehicleLogin(VIN, 42, "89860000000000000001", 2, 4);
        RawMessage raw = MessageDecoder.decodeRaw(bytes);
        Object msg = MessageDecoder.decode(raw);

        assertInstanceOf(VehicleLoginMessage.class, msg);
        VehicleLoginMessage login = (VehicleLoginMessage) msg;
        assertEquals(42, login.getSerialNumber());
        assertEquals("89860000000000000001", login.getIccid());
        assertEquals(2, login.getBatterySubsystemCount());
        assertEquals(4, login.getBatterySubsystemCodeLength());
        assertNotNull(login.getCollectTime());
        assertSame(raw, login.getRaw());
    }

    @Test
    void shouldDecodeVehicleLogout() {
        byte[] bytes = TestMessageBuilder.buildVehicleLogout(VIN, 99);
        RawMessage raw = MessageDecoder.decodeRaw(bytes);
        Object msg = MessageDecoder.decode(raw);

        assertInstanceOf(VehicleLogoutMessage.class, msg);
        VehicleLogoutMessage logout = (VehicleLogoutMessage) msg;
        assertEquals(99, logout.getSerialNumber());
        assertNotNull(logout.getCollectTime());
    }

    @Test
    void shouldDecodeHeartbeat() {
        byte[] bytes = TestMessageBuilder.buildHeartbeat(VIN);
        RawMessage raw = MessageDecoder.decodeRaw(bytes);
        Object msg = MessageDecoder.decode(raw);

        assertInstanceOf(HeartbeatMessage.class, msg);
        assertEquals(0, raw.getDataLength());
    }

    @Test
    void shouldDecodeRealtimeData() {
        byte[] bytes = TestMessageBuilder.buildRealtimeData(VIN, 60.5, 85, 121.4737, 31.2304);
        RawMessage raw = MessageDecoder.decodeRaw(bytes);
        Object msg = MessageDecoder.decode(raw);

        assertInstanceOf(RealtimeDataMessage.class, msg);
        RealtimeDataMessage rt = (RealtimeDataMessage) msg;

        assertNotNull(rt.getVehicleData());
        assertEquals(85, rt.getVehicleData().getSoc());
        assertEquals(60.5, rt.getVehicleData().getSpeed(), 0.01);

        assertNotNull(rt.getPositionData());
        assertEquals(121.4737, rt.getPositionData().getLongitude(), 0.0001);
        assertEquals(31.2304, rt.getPositionData().getLatitude(), 0.0001);
        assertTrue(rt.getPositionData().isValid());
    }

    @Test
    void shouldDecodeFullRealtimeData() {
        byte[] bytes = TestMessageBuilder.buildRealtimeDataFull(
                VIN, 80.0, 90, 12345.6, 380.0, -50.0,
                121.5, 31.2, 3000, 60);

        RawMessage raw = MessageDecoder.decodeRaw(bytes);
        Object msg = MessageDecoder.decode(raw);
        RealtimeDataMessage rt = (RealtimeDataMessage) msg;

        assertNotNull(rt.getVehicleData());
        assertEquals(90, rt.getVehicleData().getSoc());
        assertEquals(80.0, rt.getVehicleData().getSpeed(), 0.01);
        assertEquals(12345.6, rt.getVehicleData().getOdometer(), 0.01);
        assertEquals(380.0, rt.getVehicleData().getTotalVoltage(), 0.01);

        assertNotNull(rt.getPositionData());
        assertEquals(121.5, rt.getPositionData().getLongitude(), 0.01);

        assertFalse(rt.getDriveMotorDataList().isEmpty());
        assertEquals(1, rt.getDriveMotorDataList().get(0).getMotorCount());

        assertNotNull(rt.getExtremumData());
        assertNotNull(rt.getAlarmData());
    }

    @Test
    void shouldDecodePlatformLogin() {
        byte[] bytes = TestMessageBuilder.buildPlatformLogin("admin", "pass123");
        RawMessage raw = MessageDecoder.decodeRaw(bytes);
        Object msg = MessageDecoder.decode(raw);

        assertInstanceOf(PlatformLoginMessage.class, msg);
        PlatformLoginMessage login = (PlatformLoginMessage) msg;
        assertEquals("admin", login.getUsername());
        assertEquals("pass123", login.getPassword());
    }

    @Test
    void shouldDecodePlatformLogout() {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(0x25); data.write(0x05); data.write(0x14);
        data.write(0x12); data.write(0x00); data.write(0x00);
        data.write(0x00);
        data.write(0x07);
        byte[] built = TestMessageBuilder.buildMessage(
                CommandFlag.PLATFORM_LOGOUT, ResponseFlag.COMMAND,
                "platform        ", data.toByteArray());

        RawMessage raw = MessageDecoder.decodeRaw(built);
        Object msg = MessageDecoder.decode(raw);
        assertInstanceOf(PlatformLogoutMessage.class, msg);
    }

    @Test
    void shouldRejectInvalidStartMarker() {
        byte[] bytes = new byte[30];
        bytes[0] = 0x24;
        bytes[1] = 0x23;
        assertThrows(MessageDecoder.DecodeException.class,
                () -> MessageDecoder.decodeRaw(bytes));
    }

    @Test
    void shouldRejectShortMessage() {
        byte[] bytes = new byte[10];
        assertThrows(MessageDecoder.DecodeException.class,
                () -> MessageDecoder.decodeRaw(bytes));
    }

    @Test
    void shouldRejectNullMessage() {
        assertThrows(MessageDecoder.DecodeException.class,
                () -> MessageDecoder.decodeRaw(null));
    }

    @Test
    void shouldRejectBccMismatch() {
        byte[] bytes = TestMessageBuilder.buildHeartbeat(VIN);
        bytes[bytes.length - 1] ^= 0x01;
        assertThrows(MessageDecoder.DecodeException.class,
                () -> MessageDecoder.decodeRaw(bytes));
    }
}
