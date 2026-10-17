package com.github.yildizmy.service.push;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebPushTest {

    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();

    /** RFC 8291, Appendix A. */
    private static final String EXPECTED = "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN";

    @Test
    void theEncryptedBodyMatchesTheRfcExampleByteForByte() {
        byte[] expected = B64D.decode(EXPECTED);
        // The server public key is carried in the body itself, right after the 21-byte header.
        byte[] serverPublic = Arrays.copyOfRange(expected, 21, 86);
        byte[] body = WebPush.encrypt(
                "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4",
                "BTBZMqHH6r4Tts7J_aSIgg",
                "When I grow up, I want to be a watermelon".getBytes(StandardCharsets.UTF_8),
                B64D.decode("DGv6ra1nlYgDCS1FRnbzlw"),
                WebPush.keyPair(B64D.decode("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw"), serverPublic));

        assertEquals(EXPECTED, B64U.encodeToString(body));
    }

    @Test
    void twoMessagesWithTheSameTextNeverLookAlike() {
        String[] client = WebPush.newVapidKeys();
        String auth = B64U.encodeToString(new byte[16]);
        byte[] text = "{\"title\":\"oi\"}".getBytes(StandardCharsets.UTF_8);

        assertFalse(Arrays.equals(WebPush.encrypt(client[0], auth, text), WebPush.encrypt(client[0], auth, text)));
    }

    @Test
    void theVapidTokenIsSignedForThePushServiceOriginAndVerifiesWithThePublicKey() throws Exception {
        String[] vapid = WebPush.newVapidKeys();
        Instant now = Instant.parse("2026-09-24T12:00:00Z");

        String header = WebPush.vapidAuthorization("https://fcm.googleapis.com/fcm/send/abc", vapid[0], vapid[1],
                "mailto:ops@wallia.local", now);

        String token = header.substring("vapid t=".length(), header.indexOf(", k="));
        assertEquals(vapid[0], header.substring(header.indexOf(", k=") + 4));
        String[] parts = token.split("\\.");
        String claims = new String(B64D.decode(parts[1]), StandardCharsets.UTF_8);
        assertTrue(claims.contains("\"aud\":\"https://fcm.googleapis.com\""));
        assertTrue(claims.contains("\"exp\":" + (now.getEpochSecond() + 43200)));

        byte[] point = B64D.decode(vapid[0]);
        ECPublicKey key = (ECPublicKey) WebPush.keyPair(B64D.decode(vapid[1]), point).getPublic();
        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(KeyFactory.getInstance("EC").generatePublic(new java.security.spec.X509EncodedKeySpec(key.getEncoded())));
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertTrue(verifier.verify(B64D.decode(parts[2])));
    }
}
