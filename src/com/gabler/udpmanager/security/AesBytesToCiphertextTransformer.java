package com.gabler.udpmanager.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.function.BiFunction;

/**
 * Transformer that uses AES to turn bytes plaintext to bytes ciphertext.
 *
 * @author Andy Gabler
 */
public class AesBytesToCiphertextTransformer implements BiFunction<byte[], byte[], byte[]> {

    @Override
    public byte[] apply(byte[] plainText, byte[] key) {
        final SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
        final IvParameterSpec stubIv = new IvParameterSpec(staticIvBytes());

        final byte[] cipherText;
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, stubIv);
            cipherText = cipher.doFinal(plainText);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        return cipherText;
    }

    /**
     * AES requires an initialization vector (IV). No implementation present for this in client nor server so
     * generate a static one and instead recommend that keys be refreshed with new RSA or DHKE.
     *
     * @return The static IV
     */
    static byte[] staticIvBytes() {
        return new byte[]{0x5, 0xd, 0xd, 0x7, 0x6b, 0x9, 0x2, 0x48, 0x6, 0x7e, 0x5e, 0x1b, 0x1d, 0x23, 0x1e, 0x13};
    }
}
