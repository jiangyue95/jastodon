package com.yue.jastodon.federation;

import com.yue.jastodon.TestKeys;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicKeyPemTest {

    @Test
    void parsesPemPublicKey() {
        KeyPair keyPair = TestKeys.generateRsaKeyPair();

        PublicKey parsed = PublicKeyPem.parse(TestKeys.toPem(keyPair.getPublic()));

        assertThat(parsed.getEncoded()).isEqualTo(keyPair.getPublic().getEncoded());
    }

    @Test
    void rejectsTextWithoutPemMarkers() {
        assertThatThrownBy(() -> PublicKeyPem.parse("not a key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected a PEM public key");
    }

    @Test
    void rejectsInvalidBase64() {
        assertThatThrownBy(() -> PublicKeyPem.parse("-----BEGIN PUBLIC KEY-----\nAB=C\n-----END PUBLIC KEY-----"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Public key is not valid Base64");
    }

    @Test
    void rejectsNonRsaKey() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        PublicKey ecPublicKey = generator.generateKeyPair().getPublic();

        assertThatThrownBy(() -> PublicKeyPem.parse(TestKeys.toPem(ecPublicKey)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not an RSA public key");
    }
}
