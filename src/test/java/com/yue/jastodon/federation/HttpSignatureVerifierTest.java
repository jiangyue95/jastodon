package com.yue.jastodon.federation;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HttpSignatureVerifierTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);
    private static final SignatureParameters COVERS_REQUIRED_HEADS = new SignatureParameters(
            URI.create("https://mastodon.example/users/alice/#main-key"),
            "rsa-sha256",
            List.of("(request-target)", "host", "date", "digest"),
            new byte[0]);

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

    @Test
    void rejectsSignatureThatDoesNotCoverDigest() {
        IncomingRequest request = new IncomingRequest(
                "POST", "/users/yue/inbox", Map.of(), new byte[0]);
        SignatureParameters params = new SignatureParameters(
                URI.create("https://mastodon.example/users/alice#main-key"),
                "rsa-sha256",
                List.of("(request-target)", "host", "date"),
                new byte[0]);

        assertThatThrownBy(() -> verifier.verify(request, params, null))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("required header: digest");
    }

    @Test
    void rejectsMissingDate() {
        IncomingRequest request = new IncomingRequest(
                "POST", "/users/yue/inbox", Map.of(), new byte[0]);

        assertThatThrownBy(() -> verifier.verify(request, COVERS_REQUIRED_HEADS, null))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Missing Date header");
    }

    @Test
    void rejectsUnparseableDate() {
        IncomingRequest request = requestWithDate("yesterday");

        assertThatThrownBy(() -> verifier.verify(request, COVERS_REQUIRED_HEADS, null))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Invalid Date header");
    }

    @Test
    void rejectsDateTooFarInThePast() {
        IncomingRequest request = requestWithDate("Wed, 07 Oct 2026 08:59:59 GMT");

        assertThatThrownBy(() -> verifier.verify(request, COVERS_REQUIRED_HEADS, null))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Date is outside the allowed window");
    }

    @Test
    void rejectsDateTooFarInTheFuture() {
        IncomingRequest request = requestWithDate("Wed, 07 Oct 2026 11:00:01 GMT");

        assertThatThrownBy(() -> verifier.verify(request, COVERS_REQUIRED_HEADS, null))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Date is outside the allowed window");
    }

    @Test
    void rejectsDigestThatDoesNotMatchBody() {
        IncomingRequest request = request(
                Map.of("Date", "Wed, 07 Oct 2026 10:00:00 GMT",
                        "Digest", "SHA-256=HWKqeTJnS/L2x7rjx755Vv5ri6Ld95yKxd3htgM0d6Y="),
                "{\"type\":\"Delete\"}".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> verifier.verify(request, COVERS_REQUIRED_HEADS, null))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Digest does not match the body");
    }

    @Test
    void rejectsMissingDigest() {
        IncomingRequest request = request(
                Map.of("Date", "Wed, 07 Oct 2026 10:00:00 GMT"),
                new byte[0]);

        assertThatThrownBy(() -> verifier.verify(request, COVERS_REQUIRED_HEADS, null))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Missing Digest header");
    }

    @Test
    void rejectsUnsupportedDigestAlgorithm() {
        IncomingRequest request = request(
                Map.of("Date", "Wed, 07 Oct 2026 10:00:00 GMT",
                        "Digest", "SHA-512=abc"),
                new byte[0]);

        assertThatThrownBy(() -> verifier.verify(request, COVERS_REQUIRED_HEADS, null))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Unsupported Digest algorithm");
    }

    private static IncomingRequest request(Map<String, String> headers, byte[] body) {
        return new IncomingRequest("POST", "/users/yue/inbox", headers, body);
    }

    private static IncomingRequest requestWithDate(String date) {
        return new IncomingRequest(
                "POST", "/users/yue/inbox", Map.of("Date", date), new byte[0]);
    }
}
