package io.github.darkinno.gb32960.core.crypto;

import io.github.darkinno.gb32960.core.constant.EncryptionType;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RsaCryptoProvider implements CryptoProvider {

    private static final String ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final String KEY_ALGORITHM = "RSA";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public RsaCryptoProvider(String privateKeyPem, String publicKeyPem) {
        this.privateKey = parsePrivateKey(privateKeyPem);
        this.publicKey = parsePublicKey(publicKeyPem);
    }

    @Override
    public byte[] decrypt(byte encryptionType, byte[] data) {
        if (encryptionType != EncryptionType.RSA) {
            return data;
        }
        if (data == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new CryptoException("RSA decryption failed", e);
        }
    }

    @Override
    public byte[] encrypt(byte encryptionType, byte[] data) {
        if (encryptionType != EncryptionType.RSA) {
            return data;
        }
        if (data == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new CryptoException("RSA encryption failed", e);
        }
    }

    @Override
    public boolean supports(byte encryptionType) {
        return encryptionType == EncryptionType.RSA
                || encryptionType == EncryptionType.NONE;
    }

    private static PrivateKey parsePrivateKey(String pem) {
        try {
            String key = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new CryptoException("Failed to parse RSA private key", e);
        }
    }

    private static PublicKey parsePublicKey(String pem) {
        try {
            String key = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new CryptoException("Failed to parse RSA public key", e);
        }
    }
}
