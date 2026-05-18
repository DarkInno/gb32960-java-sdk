package io.github.darkinno.gb32960.core.codec;

import io.github.darkinno.gb32960.core.constant.CommandFlag;
import io.github.darkinno.gb32960.core.constant.ResponseFlag;
import io.github.darkinno.gb32960.core.model.RawMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageEncoderTest {

    private static final String VIN = "LSVAM40E7GA000001";

    @Test
    void shouldEncodeAndDecodeRoundtrip() {
        RawMessage original = RawMessage.builder()
                .commandFlag(CommandFlag.HEARTBEAT)
                .responseFlag(ResponseFlag.COMMAND)
                .vin(VIN)
                .encryptionType((byte) 0x01)
                .dataLength(0)
                .dataUnit(null)
                .build();

        byte[] encoded = MessageEncoder.encode(original);
        RawMessage decoded = MessageDecoder.decodeRaw(encoded);

        assertEquals(original.getCommandFlag(), decoded.getCommandFlag());
        assertEquals(original.getResponseFlag(), decoded.getResponseFlag());
        assertEquals(original.getVin(), decoded.getVin());
        assertEquals(original.getEncryptionType(), decoded.getEncryptionType());
    }

    @Test
    void shouldBuildSuccessResponse() {
        RawMessage request = RawMessage.builder()
                .commandFlag(CommandFlag.VEHICLE_LOGIN)
                .responseFlag(ResponseFlag.COMMAND)
                .vin(VIN)
                .encryptionType((byte) 0x01)
                .build();

        byte[] response = MessageEncoder.buildResponse(request, ResponseFlag.SUCCESS, null);
        RawMessage decoded = MessageDecoder.decodeRaw(response);

        assertEquals(CommandFlag.VEHICLE_LOGIN, decoded.getCommandFlag());
        assertEquals(ResponseFlag.SUCCESS, decoded.getResponseFlag());
        assertEquals(VIN, decoded.getVin());
    }

    @Test
    void shouldBuildErrorResponse() {
        RawMessage request = RawMessage.builder()
                .commandFlag(CommandFlag.HEARTBEAT)
                .responseFlag(ResponseFlag.COMMAND)
                .vin(VIN)
                .encryptionType((byte) 0x01)
                .build();

        byte[] data = new byte[]{0x01};
        byte[] response = MessageEncoder.buildResponse(request, ResponseFlag.ERROR, data);
        RawMessage decoded = MessageDecoder.decodeRaw(response);

        assertEquals(ResponseFlag.ERROR, decoded.getResponseFlag());
        assertEquals(1, decoded.getDataLength());
    }

    @Test
    void shouldBuildPlatformLogin() {
        byte[] encoded = MessageEncoder.buildPlatformLogin("admin", "pass123", (byte) 0x01);
        RawMessage decoded = MessageDecoder.decodeRaw(encoded);

        assertEquals(CommandFlag.PLATFORM_LOGIN, decoded.getCommandFlag());
        assertEquals(ResponseFlag.COMMAND, decoded.getResponseFlag());
        assertTrue(decoded.getDataLength() > 0);
    }

    @Test
    void shouldHandleVinsPadAndTruncate() {
        RawMessage msg = RawMessage.builder()
                .commandFlag(CommandFlag.HEARTBEAT)
                .responseFlag(ResponseFlag.COMMAND)
                .vin("SHORT")
                .encryptionType((byte) 0x01)
                .dataLength(0)
                .build();

        byte[] encoded = MessageEncoder.encode(msg);
        RawMessage decoded = MessageDecoder.decodeRaw(encoded);

        assertEquals("SHORT", decoded.getVin());
    }

    @Test
    void shouldBuildMultipleMessageTypes() {
        for (byte cmd : new byte[]{CommandFlag.VEHICLE_LOGIN, CommandFlag.VEHICLE_LOGOUT,
                CommandFlag.HEARTBEAT, CommandFlag.REALTIME_REPORT}) {
            RawMessage msg = RawMessage.builder()
                    .commandFlag(cmd)
                    .responseFlag(ResponseFlag.COMMAND)
                    .vin(VIN)
                    .encryptionType((byte) 0x01)
                    .dataLength(0)
                    .build();

            byte[] encoded = MessageEncoder.encode(msg);
            RawMessage decoded = MessageDecoder.decodeRaw(encoded);

            assertEquals(cmd, decoded.getCommandFlag());
        }
    }
}
