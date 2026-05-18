package io.github.darkinno.gb32960.core.codec;

import io.github.darkinno.gb32960.core.constant.CommandFlag;
import io.github.darkinno.gb32960.core.constant.ResponseFlag;
import io.github.darkinno.gb32960.core.model.RawMessage;
import io.github.darkinno.gb32960.core.util.BccUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MessageEncoder {

    private static final byte START1 = 0x23;
    private static final byte START2 = 0x23;

    private MessageEncoder() {}

    public static byte[] encode(RawMessage msg) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(START1);
            baos.write(START2);
            baos.write(msg.getCommandFlag());
            baos.write(msg.getResponseFlag());

            byte[] vinBytes = padOrTruncateVin(msg.getVin());
            baos.write(vinBytes);

            baos.write(msg.getEncryptionType());

            int dataLen = msg.getDataUnit() != null ? msg.getDataUnit().length : 0;
            baos.write((dataLen >> 8) & 0xFF);
            baos.write(dataLen & 0xFF);

            if (dataLen > 0) {
                baos.write(msg.getDataUnit());
            }

            byte[] headerAndBody = baos.toByteArray();
            byte bcc = BccUtil.calculate(headerAndBody, 2, headerAndBody.length - 2);
            baos.write(bcc);

            return baos.toByteArray();
        } catch (IOException e) {
            throw new EncodeException("Failed to encode message", e);
        }
    }

    public static byte[] buildResponse(RawMessage request, byte responseFlag, byte[] dataUnit) {
        return encode(RawMessage.builder()
                .commandFlag(request.getCommandFlag())
                .responseFlag(responseFlag)
                .vin(request.getVin())
                .encryptionType(request.getEncryptionType())
                .dataUnit(dataUnit)
                .dataLength(dataUnit != null ? dataUnit.length : 0)
                .build());
    }

    public static byte[] buildPlatformLogin(String username, String password, byte encryptionType) {
        try {
            ByteArrayOutputStream dataOs = new ByteArrayOutputStream();
            writeBcdTime(dataOs, java.time.LocalDateTime.now());
            dataOs.write((0x0001 >> 8) & 0xFF);
            dataOs.write(0x0001 & 0xFF);

            byte[] userBytes = padTruncate(username, 12);
            dataOs.write(userBytes);

            byte[] passBytes = padTruncate(password, 20);
            dataOs.write(passBytes);

            dataOs.write(encryptionType);

            byte[] dataUnit = dataOs.toByteArray();

            RawMessage msg = RawMessage.builder()
                    .commandFlag(CommandFlag.PLATFORM_LOGIN)
                    .responseFlag(ResponseFlag.COMMAND)
                    .vin("platform        ")
                    .encryptionType((byte) 0x01)
                    .dataUnit(dataUnit)
                    .dataLength(dataUnit.length)
                    .build();
            return encode(msg);
        } catch (IOException e) {
            throw new EncodeException("Failed to build platform login", e);
        }
    }

    private static byte[] padOrTruncateVin(String vin) {
        byte[] result = new byte[17];
        byte[] bytes = (vin != null ? vin : "").getBytes(StandardCharsets.US_ASCII);
        int len = Math.min(bytes.length, 17);
        System.arraycopy(bytes, 0, result, 0, len);
        for (int i = len; i < 17; i++) {
            result[i] = 0x00;
        }
        return result;
    }

    private static byte[] padTruncate(String str, int length) {
        byte[] result = new byte[length];
        byte[] bytes = (str != null ? str : "").getBytes(StandardCharsets.US_ASCII);
        int len = Math.min(bytes.length, length);
        System.arraycopy(bytes, 0, result, 0, len);
        return result;
    }

    private static void writeBcdTime(ByteArrayOutputStream baos, java.time.LocalDateTime time) {
        baos.write(intToBcd(time.getYear() % 100));
        baos.write(intToBcd(time.getMonthValue()));
        baos.write(intToBcd(time.getDayOfMonth()));
        baos.write(intToBcd(time.getHour()));
        baos.write(intToBcd(time.getMinute()));
        baos.write(intToBcd(time.getSecond()));
    }

    private static byte intToBcd(int value) {
        return (byte) (((value / 10) << 4) | (value % 10));
    }

    public static class EncodeException extends RuntimeException {
        public EncodeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
