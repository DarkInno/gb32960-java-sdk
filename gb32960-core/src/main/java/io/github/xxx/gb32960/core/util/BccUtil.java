package io.github.xxx.gb32960.core.util;

public final class BccUtil {

    private BccUtil() {}

    public static byte calculate(byte[] data, int offset, int length) {
        byte bcc = data[offset];
        for (int i = offset + 1; i < offset + length; i++) {
            bcc ^= data[i];
        }
        return bcc;
    }

    public static boolean verify(byte[] data, int offset, int length, byte expected) {
        return calculate(data, offset, length) == expected;
    }
}
