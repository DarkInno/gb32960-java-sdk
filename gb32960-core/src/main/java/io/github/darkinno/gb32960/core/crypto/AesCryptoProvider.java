package io.github.darkinno.gb32960.core.crypto;

import io.github.darkinno.gb32960.core.constant.EncryptionType;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class AesCryptoProvider implements CryptoProvider {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final byte[] key;

    public AesCryptoProvider(byte[] key) {
        if (key.length != 16) {
            throw new IllegalArgumentException("AES key must be 16 bytes (128-bit)");
        }
        this.key = key.clone();
    }

    @Override
    public byte[] decrypt(byte encryptionType, byte[] data) {
        if (encryptionType != EncryptionType.AES128) {
            return data;
        }
        if (data == null || data.length < IV_LENGTH) {
            return data;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, IV_LENGTH);

            byte[] ciphertext = new byte[data.length - IV_LENGTH];
            System.arraycopy(data, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new IvParameterSpec(iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new CryptoException("AES decryption failed", e);
        }
    }

    @Override
    public byte[] encrypt(byte encryptionType, byte[] data) {
        if (encryptionType != EncryptionType.AES128) {
            return data;
        }
        if (data == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new IvParameterSpec(iv));
            byte[] ciphertext = cipher.doFinal(data);

            byte[] result = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, result, IV_LENGTH, ciphertext.length);
            return result;
        } catch (Exception e) {
            throw new CryptoException("AES encryption failed", e);
        }
    }

    @Override
    public boolean supports(byte encryptionType) {
        return encryptionType == EncryptionType.AES128
                || encryptionType == EncryptionType.NONE;
    }
}
