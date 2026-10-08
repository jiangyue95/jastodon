package com.yue.jastodon.federation;

import com.yue.jastodon.TestKeys;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class HttpSignatureSignerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);
    private static final KeyPair KEY_PAIR = TestKeys.generateRsaKeyPair();

    private final FederationProperties properties = new FederationProperties(
            "yue",
            URI.create("https://jastodon.test"),
            new ByteArrayResource(TestKeys.toPem(KEY_PAIR.getPrivate()).getBytes(StandardCharsets.UTF_8)));
    private final HttpSignatureSigner signer = new HttpSignatureSigner(new ActorKeys(properties), properties, CLOCK);

    @Test
    void formatsDateAsImfFixdate() {
        SignatureHeaders headers = signer.sign(HttpMethod.POST, URI.create("https://example.test/inbox"), new byte[0]);

        assertThat(headers.date()).isEqualTo("Wed, 07 Oct 2026 10:00:00 GMT");
    }

    @Test
    void computesSha256DigestOfBody() {
        byte[] body = "{\"type\":\"Accept\"}".getBytes(StandardCharsets.UTF_8);
        SignatureHeaders headers = signer.sign(HttpMethod.POST, URI.create("https://example.test/inbox"), body);
        assertThat(headers.digest()).isEqualTo("SHA-256=HWKqeTJnS/L2x7rjx755Vv5ri6Ld95yKxd3htgM0d6Y=");
    }

    @Test
    void buildsSigningStringInCavageFormat() {
        String signingString = HttpSignatureSigner.signingString(
                HttpMethod.POST,
                URI.create("https://mastodon.example/users/alice/inbox"),
                "Wed, 07 Oct 2026 10:00:00 GMT",
                "SHA-256=HWKqeTJnS/L2x7rjx755Vv5ri6Ld95yKxd3htgM0d6Y=");

        assertThat(signingString).isEqualTo(
                "(request-target): post /users/alice/inbox\n"
                + "host: mastodon.example\n"
                + "date: Wed, 07 Oct 2026 10:00:00 GMT\n"
                + "digest: SHA-256=HWKqeTJnS/L2x7rjx755Vv5ri6Ld95yKxd3htgM0d6Y=");
    }

    @Test
    void includesExplicitPortAndQueryInSigningString() {
        String signingString = HttpSignatureSigner.signingString(
                HttpMethod.POST,
                URI.create("http://localhost:8080/inbox?x=1"),
                "Wed, 07 Oct 2026 10:00:00 GMT",
                "SHA-256=abc");

        assertThat(signingString)
                .startsWith("(request-target): post /inbox?x=1\nhost: localhost:8080\n");
    }

    @Test
    void signatureVerifiesWithTheActorPublicKey() throws Exception {
        byte[] body = "{\"type\":\"Accept\"}".getBytes(StandardCharsets.UTF_8);
        SignatureHeaders headers = signer.sign(
                HttpMethod.POST, URI.create("https://mastodon.example/users/alice/inbox"), body);

        assertThat(headers.signature()).startsWith(
                "keyId=\"https://jastodon.test/users/yue#main-key\","
                + "algorithm=\"rsa-sha256\","
                + "headers=\"(request-target) host date digest\","
                + "signature=\"");

        String signatureValue = headers.signature().replaceAll(".*signature=\"([^\"]+)\".*", "$1");
        String expectedSigningString = "(request-target): post /users/alice/inbox\n"
                + "host: mastodon.example\n"
                + "date: Wed, 07 Oct 2026 10:00:00 GMT\n"
                + "digest: SHA-256=HWKqeTJnS/L2x7rjx755Vv5ri6Ld95yKxd3htgM0d6Y=";

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(KEY_PAIR.getPublic());
        verifier.update(expectedSigningString.getBytes(StandardCharsets.UTF_8));
        assertThat(verifier.verify(Base64.getDecoder().decode(signatureValue))).isTrue();
    }
}
