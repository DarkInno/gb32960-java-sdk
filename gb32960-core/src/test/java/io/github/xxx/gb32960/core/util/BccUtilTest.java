package io.github.xxx.gb32960.core.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BccUtilTest {

    @Test
    void shouldCalculateSimpleXor() {
        byte[] data = {0x01, 0x02, 0x03};
        byte result = BccUtil.calculate(data, 0, 3);
        assertEquals(0x00, result, "0x01 ^ 0x02 ^ 0x03 = 0x00");
    }

    @Test
    void shouldCalculateWithOffset() {
        byte[] data = {0x00, 0x0F, 0x00, 0x0F};
        byte result = BccUtil.calculate(data, 1, 3);
        assertEquals(0x00, result, "0x0F ^ 0x00 ^ 0x0F = 0x00");
    }

    @Test
    void shouldVerifyCorrectBcc() {
        byte[] data = {0x01, 0x02, 0x03, 0x00};
        assertTrue(BccUtil.verify(data, 0, 3, data[3]));
    }

    @Test
    void shouldDetectWrongBcc() {
        byte[] data = {0x01, 0x02, 0x03, 0x01};
        assertFalse(BccUtil.verify(data, 0, 3, data[3]));
    }

    @Test
    void shouldHandleSingleByte() {
        assertEquals((byte) 0x01, BccUtil.calculate(new byte[]{0x01}, 0, 1));
    }

    @Test
    void shouldBeCommutative() {
        byte[] d1 = {0x01, 0x02, 0x03};
        byte[] d2 = {0x01, 0x03, 0x02};
        byte r1 = BccUtil.calculate(d1, 0, 3);
        byte r2 = BccUtil.calculate(d2, 0, 3);
        assertEquals(r1, r2, "XOR is commutative");
    }
}
