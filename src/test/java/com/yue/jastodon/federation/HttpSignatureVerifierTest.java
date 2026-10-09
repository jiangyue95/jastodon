package com.yue.jastodon.federation;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HttpSignatureVerifierTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);

    private final HttpSignatureVerifier verifier = new HttpSignatureVerifier(CLOCK);

    @Test
    void parsesAllParameters() {
        SignatureParameters params = verifier.parse(
                "keyId=\"https://mastodon.example/users/alice#main-key\","
                + "algorithm=\"rsa-sha256\","
                + "headers=\"(request-target) host date digest\","
                + "signature=\"c2lnbmF0dXJl\"");

        assertThat(params.keyId()).isEqualTo(URI.create("https://mastodon.example/users/alice#main-key"));
        assertThat(params.algorithm()).isEqualTo("rsa-sha256");
        assertThat(params.headers()).containsExactly("(request-target)", "host", "date", "digest");
        assertThat(params.signature()).isEqualTo("signature".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void acceptsHs2019Algorithm() {
        SignatureParameters params = verifier.parse(
                "keyId=\"https://mastodon.example/users/alice#main-key\","
                + "algorithm=\"hs2019\","
                + "headers=\"(request-target) host date digest\","
                + "signature=\"c2lhbmF0dXJl\"");

        assertThat(params.algorithm()).isEqualTo("hs2019");
    }

    @Test
    void acceptsMissingAlgorithm() {
        SignatureParameters params = verifier.parse(
                "keyId=\"https://mastodon.example/users/alice#main-key\","
                + "headers=\"(request-target) host date digest\","
                + "signature=\"c2lnbmF0dXJl\"");

        assertThat(params.algorithm()).isNull();
    }

    @Test
    void rejectsMissingHeader() {
        assertThatThrownBy(() -> verifier.parse(null))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Missing Signature header");
    }

    @Test
    void rejectsBlankHeader() {
        assertThatThrownBy(() -> verifier.parse("   "))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Missing Signature header");
    }

    @Test
    void rejectsMissingKeyId() {
        assertThatThrownBy(() -> verifier.parse(
                "algorithm=\"rsa-sha256\","
                        + "headers=\"(request-target) host date digest\","
                        + "signature=\"c2lnbmF0dXJl\""))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("missing keyId");
    }

    @Test
    void rejectsMissingSignature() {
        assertThatThrownBy(() -> verifier.parse(
                "keyId=\"https://mastodon.example/users/alice#main-key\","
                        + "algorithm=\"rsa-sha256\","
                        + "headers=\"(request-target) host date digest\""))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("missing signature");
    }

    @Test
    void rejectsInvalidKeyId() {
        assertThatThrownBy(() -> verifier.parse(
                "keyId=\"not a uri\","
                        + "algorithm=\"rsa-sha256\","
                        + "headers=\"(request-target) host date digest\","
                        + "signature=\"c2lnbmF0dXJl\""))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Invalid keyId");
    }

    @Test
    void rejectsUnsupportedAlgorithm() {
        assertThatThrownBy(() -> verifier.parse(
                "keyId=\"https://mastodon.example/users/alice#main-key\","
                        + "algorithm=\"hmac-sha256\","
                        + "headers=\"(request-target) host date digest\","
                        + "signature=\"c2lnbmF0dXJl\""))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Unsupported signature algorithm");
    }

    @Test
    void rejectsInvalidBase64Signature() {
        assertThatThrownBy(() -> verifier.parse(
                "keyId=\"https://mastodon.example/users/alice#main-key\","
                        + "algorithm=\"rsa-sha256\","
                        + "headers=\"(request-target) host date digest\","
                        + "signature=\"!!!\""))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("not valid Base64");
    }
}
