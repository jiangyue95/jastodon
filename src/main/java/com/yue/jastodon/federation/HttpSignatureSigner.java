package com.yue.jastodon.federation;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

@Component
public class HttpSignatureSigner {

    private static final DateTimeFormatter HTTP_DATE =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH)
                    .withZone(ZoneOffset.UTC);
    private static final String SIGNED_HEADERS = "(request-target) host date digest";

    private final ActorKeys actorKeys;
    private final FederationProperties federationProperties;
    private final Clock clock;

    public HttpSignatureSigner(ActorKeys actorKeys, FederationProperties federationProperties, Clock clock) {
        this.actorKeys = actorKeys;
        this.federationProperties = federationProperties;
        this.clock = clock;
    }

    public SignatureHeaders sign(HttpMethod method, URI target, byte[] body) {
        String date = HTTP_DATE.format(clock.instant());
        String digest = "SHA-256=" + Base64.getEncoder().encodeToString(sha256(body));
        String signingString = signingString(method, target, date, digest);
        byte[] signatureBytes = actorKeys.sign(signingString.getBytes(StandardCharsets.UTF_8));
        String signature = "keyId=\"" + federationProperties.keyId() + "\","
                + "algorithm=\"rsa-sha256\","
                + "headers=\"" + SIGNED_HEADERS + "\","
                + "signature=\"" + Base64.getEncoder().encodeToString(signatureBytes) + "\"";
        return new SignatureHeaders(date, digest, signature);
    }

    static String signingString(HttpMethod method, URI target, String date, String digest) {
        return String.join("\n",
                "(request-target): " + method.name().toLowerCase(Locale.ROOT) + " " + requestTarget(target),
                "host: " + host(target),
                "date: " + date,
                "digest: " + digest);
    }

    private static String requestTarget(URI target) {
        String path = target.getRawPath().isEmpty() ? "/" : target.getRawPath();
        return target.getRawQuery() == null ? path : path + "?" + target.getRawQuery();
    }

    private static String host(URI target) {
        return target.getPort() == -1 ? target.getHost() : target.getHost() + ":" + target.getPort();
    }

    private static byte[] sha256(byte[] body) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(body);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
