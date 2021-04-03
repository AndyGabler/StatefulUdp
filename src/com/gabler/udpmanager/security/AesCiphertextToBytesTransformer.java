package com.gabler.udpmanager.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.function.BiFunction;

/**
 * Transformer that uses AES to turn bytes ciphertext to bytes plaintext.
 *
 * @author Andy Gabler
 */
public class AesCiphertextToBytesTransformer implements BiFunction<byte[], byte[], byte[]> {

    @Override
    public byte[] apply(byte[] cipherText, byte[] key) {
        final SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
        final IvParameterSpec stubIv = new IvParameterSpec(AesBytesToCiphertextTransformer.staticIvBytes());

        final byte[] plainText;
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, stubIv);
            plainText = cipher.doFinal(cipherText);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        return plainText;
    }
}
