package org.example.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public final class CryptoUtil {
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private CryptoUtil() {
    }

    public static String aesDecrypt(String cipherText, String secretKey) {
        try {
            byte[] keyBytes = Arrays.copyOf(secretKey.getBytes(StandardCharsets.UTF_8), 16);
            byte[] encryptedWithIv = Base64.getDecoder().decode(cipherText);

            byte[] iv = Arrays.copyOfRange(encryptedWithIv, 0, GCM_IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(encryptedWithIv, GCM_IV_LENGTH, encryptedWithIv.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decrypt password failed", e);
        }
    }
}
