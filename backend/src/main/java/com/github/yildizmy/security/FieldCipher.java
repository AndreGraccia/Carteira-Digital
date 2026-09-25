package com.github.yildizmy.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption at rest for free text that names people and companies (ADR 0014): AES-256-GCM, a random 96-bit IV
 * per value, stored as {@code enc:v1:<base64(iv | ciphertext | tag)>}. Values without the prefix were written
 * before the key existed and are read as they are, so no data migration is needed.
 * <p>
 * The key comes from {@code data_encryption_key} (Base64, 32 bytes). Without it, text is stored as is and a warning
 * is logged; the production compose file refuses to start without it.
 * <p>
 * ponytail: static key holder, because Hibernate creates attribute converters itself. One key, no rotation; add a
 * key id to the prefix (enc:v2:<id>:) when rotation is needed.
 */
@Slf4j
@Component
public class FieldCipher {

    static final String PREFIX = "enc:v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile SecretKeySpec key;

    public FieldCipher(@Value("${app.security.dataEncryptionKey:}") String base64Key) {
        use(base64Key);
    }

    /** Fails at startup on a malformed key; a blank key only turns encryption off, with a warning. */
    static void use(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            key = null;
            log.warn("data_encryption_key is not set: imported descriptions are stored without encryption");
            return;
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("data_encryption_key must be Base64", exception);
        }
        if (bytes.length != 32) throw new IllegalStateException("data_encryption_key must have 32 bytes (256 bits)");
        key = new SecretKeySpec(bytes, "AES");
    }

    public static String encrypt(String plain) {
        SecretKeySpec current = key;
        if (plain == null || current == null) return plain;
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, current, new GCMParameterSpec(TAG_BITS, iv));
            byte[] sealed = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + sealed.length)
                    .put(iv).put(sealed).array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("could not encrypt a field", exception);
        }
    }

    public static String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) return stored;
        SecretKeySpec current = key;
        if (current == null) throw new IllegalStateException("encrypted data found but data_encryption_key is not set");
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, current, new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            return new String(cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            // Tampered, truncated or written with another key: never return garbage as if it were the text.
            throw new IllegalStateException("could not decrypt a field", exception);
        }
    }
}
