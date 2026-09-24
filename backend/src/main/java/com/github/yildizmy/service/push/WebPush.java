package com.github.yildizmy.service.push;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

/**
 * Web Push without an SDK, with the JDK only: RFC 8291 encrypts the body so that only the subscribed
 * browser can read it (not even Google, Mozilla or Apple), and RFC 8292 (VAPID) signs a JWT that proves
 * the message comes from the key the browser subscribed with. The test checks the RFC 8291 example byte by byte.
 */
public final class WebPush {

    static final int RECORD_SIZE = 4096;
    private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ECParameterSpec P256 = p256();

    private WebPush() {
    }

    /** Body for {@code Content-Encoding: aes128gcm}, with a fresh salt and server key. */
    public static byte[] encrypt(String clientPublicB64, String authB64, byte[] plaintext) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return encrypt(clientPublicB64, authB64, plaintext, salt, generateKeyPair());
    }

    /** Salt and server key are injectable only for the RFC example; in real use they are always random. */
    static byte[] encrypt(String clientPublicB64, String authB64, byte[] plaintext, byte[] salt, KeyPair server) {
        try {
            byte[] clientPublic = B64D.decode(clientPublicB64);
            byte[] auth = B64D.decode(authB64);
            byte[] serverPublic = uncompressed((ECPublicKey) server.getPublic());

            KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(server.getPrivate());
            agreement.doPhase(publicKey(clientPublic), true);
            byte[] shared = agreement.generateSecret();

            byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), clientPublic, serverPublic);
            byte[] ikm = hkdf(auth, shared, keyInfo, 32);
            byte[] cek = hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
            byte[] nonce = hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

            // One record, closed by the 0x02 "last record" delimiter.
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
            byte[] body = cipher.doFinal(concat(plaintext, new byte[]{2}));

            ByteBuffer header = ByteBuffer.allocate(21);
            header.put(salt).putInt(RECORD_SIZE).put((byte) serverPublic.length);
            return concat(header.array(), serverPublic, body);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("web push encryption failed", exception);
        }
    }

    /** {@code Authorization} header value: a 12-hour JWT for the push service's origin, signed with ES256. */
    public static String vapidAuthorization(String endpoint, String publicKeyB64, String privateKeyB64,
                                            String subject, Instant now) {
        try {
            URI uri = URI.create(endpoint);
            String audience = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
            String header = b64("{\"typ\":\"JWT\",\"alg\":\"ES256\"}");
            String claims = b64("{\"aud\":\"%s\",\"exp\":%d,\"sub\":\"%s\"}"
                    .formatted(audience, now.getEpochSecond() + 12 * 60 * 60, subject));
            Signature signer = Signature.getInstance("SHA256withECDSAinP1363Format");
            signer.initSign(privateKey(B64D.decode(privateKeyB64)));
            signer.update((header + "." + claims).getBytes(StandardCharsets.US_ASCII));
            return "vapid t=%s.%s.%s, k=%s".formatted(header, claims, B64U.encodeToString(signer.sign()), publicKeyB64);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("vapid signature failed", exception);
        }
    }

    /** A new VAPID pair as {publicKey, privateKey} in base64url (65-byte point, 32-byte scalar). */
    public static String[] newVapidKeys() {
        KeyPair pair = generateKeyPair();
        byte[] d = ((java.security.interfaces.ECPrivateKey) pair.getPrivate()).getS().toByteArray();
        return new String[]{B64U.encodeToString(uncompressed((ECPublicKey) pair.getPublic())),
                B64U.encodeToString(fixed(d, 32))};
    }

    static KeyPair keyPair(byte[] privateScalar, byte[] publicPoint) {
        try {
            return new KeyPair(publicKey(publicPoint), privateKey(privateScalar));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    static byte[] uncompressed(ECPublicKey key) {
        ECPoint point = key.getW();
        return concat(new byte[]{4}, fixed(point.getAffineX().toByteArray(), 32), fixed(point.getAffineY().toByteArray(), 32));
    }

    private static PublicKey publicKey(byte[] point) throws GeneralSecurityException {
        if (point.length != 65 || point[0] != 4) throw new GeneralSecurityException("not an uncompressed P-256 point");
        ECPoint w = new ECPoint(new BigInteger(1, Arrays.copyOfRange(point, 1, 33)), new BigInteger(1, Arrays.copyOfRange(point, 33, 65)));
        return KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(w, P256));
    }

    private static PrivateKey privateKey(byte[] scalar) throws GeneralSecurityException {
        return KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, scalar), P256));
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** HKDF-SHA256 extract and expand; every length used here fits one block. */
    private static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        return Arrays.copyOf(mac.doFinal(concat(info, new byte[]{1})), length);
    }

    /** BigInteger bytes drop leading zeros or add a sign byte; points and scalars are always 32 bytes. */
    private static byte[] fixed(byte[] value, int size) {
        byte[] out = new byte[size];
        int copy = Math.min(value.length, size);
        System.arraycopy(value, value.length - copy, out, size - copy, copy);
        return out;
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) out.writeBytes(part);
        return out.toByteArray();
    }

    private static String b64(String json) {
        return B64U.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static ECParameterSpec p256() {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
