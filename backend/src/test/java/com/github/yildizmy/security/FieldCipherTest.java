package com.github.yildizmy.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @AfterEach
    void noKey() {
        FieldCipher.use("");
    }

    @Test
    void aDescriptionGoesInEncryptedAndComesBackTheSame() {
        FieldCipher.use(KEY);
        String stored = FieldCipher.encrypt("Pix recebido Marina Souza");

        assertTrue(stored.startsWith(FieldCipher.PREFIX));
        assertFalse(stored.contains("Marina"));
        assertEquals("Pix recebido Marina Souza", FieldCipher.decrypt(stored));
    }

    @Test
    void theSameTextNeverProducesTheSameCiphertext() {
        FieldCipher.use(KEY);

        assertNotEquals(FieldCipher.encrypt("Aluguel"), FieldCipher.encrypt("Aluguel"),
                "a fixed IV would let anyone spot repeated payees");
    }

    @Test
    void rowsWrittenBeforeTheKeyAreReadAsTheyAre() {
        FieldCipher.use(KEY);

        assertEquals("Concentrix S.A.", FieldCipher.decrypt("Concentrix S.A."));
        assertNull(FieldCipher.decrypt(null));
    }

    @Test
    void aChangedCiphertextOrAnotherKeyIsAnErrorNeverGarbage() {
        FieldCipher.use(KEY);
        String stored = FieldCipher.encrypt("Netflix");
        char last = stored.charAt(stored.length() - 3);
        String tampered = stored.substring(0, stored.length() - 3) + (last == 'A' ? 'B' : 'A') + stored.substring(stored.length() - 2);

        assertThrows(IllegalStateException.class, () -> FieldCipher.decrypt(tampered));
        FieldCipher.use(OTHER_KEY);
        assertThrows(IllegalStateException.class, () -> FieldCipher.decrypt(stored));
    }

    @Test
    void withoutAKeyTextIsStoredAsIsButEncryptedDataIsNeverSilentlyLost() {
        FieldCipher.use(KEY);
        String stored = FieldCipher.encrypt("Escola");
        FieldCipher.use("");

        assertEquals("Escola", FieldCipher.encrypt("Escola"));
        assertThrows(IllegalStateException.class, () -> FieldCipher.decrypt(stored));
    }

    @Test
    void newValuesCarryTheIdOfTheKeyThatWroteThem() {
        FieldCipher.use(KEY);
        String stored = FieldCipher.encrypt("Aluguel");

        assertTrue(stored.startsWith("enc:v2:"), stored);
        assertTrue(FieldCipher.isCurrent(stored));
        assertFalse(FieldCipher.isCurrent("Aluguel"), "clear text still needs encrypting");
    }

    @Test
    void afterARotationOldValuesStillReadAndAreFlaggedForRewriting() {
        FieldCipher.use(KEY);
        String underOldKey = FieldCipher.encrypt("Pix recebido Marina Souza");

        FieldCipher.use(OTHER_KEY, KEY);

        assertEquals("Pix recebido Marina Souza", FieldCipher.decrypt(underOldKey));
        assertFalse(FieldCipher.isCurrent(underOldKey));
        String rewritten = FieldCipher.encrypt(FieldCipher.decrypt(underOldKey));
        assertTrue(FieldCipher.isCurrent(rewritten));
        FieldCipher.use(OTHER_KEY);
        assertEquals("Pix recebido Marina Souza", FieldCipher.decrypt(rewritten), "the old key can now be retired");
    }

    @Test
    void v1ValuesFromBeforeKeyIdsAreReadWithAnyConfiguredKey() {
        String v1 = FieldCipher.PREFIX_V1 + "AAAAAAAAAAAAAAAA";
        FieldCipher.use(KEY);
        // Same payload as v2 (iv | ciphertext | tag), only without the key id: what V30 wrote.
        String v2 = FieldCipher.encrypt("Netflix");
        String realV1 = FieldCipher.PREFIX_V1 + v2.substring(v2.lastIndexOf(':') + 1);

        FieldCipher.use(OTHER_KEY, KEY);

        assertEquals("Netflix", FieldCipher.decrypt(realV1));
        assertFalse(FieldCipher.isCurrent(realV1));
        assertThrows(IllegalStateException.class, () -> FieldCipher.decrypt(v1), "no key opens it: error, not garbage");
    }

    @Test
    void aValueFromAnUnknownKeyIsAnError() {
        FieldCipher.use(KEY);
        String stored = FieldCipher.encrypt("Escola");
        FieldCipher.use(OTHER_KEY);

        assertThrows(IllegalStateException.class, () -> FieldCipher.decrypt(stored));
    }

    @Test
    void aMalformedPreviousKeyStopsTheStartup() {
        assertThrows(IllegalStateException.class, () -> FieldCipher.use(KEY, "não é base64"));
    }

    @Test
    void aMalformedKeyStopsTheStartup() {
        assertThrows(IllegalStateException.class, () -> FieldCipher.use("não é base64"));
        assertThrows(IllegalStateException.class, () -> FieldCipher.use(Base64.getEncoder().encodeToString(new byte[16])));
    }
}
