package com.yue.jastodon.federation;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HttpSignatureVerifier {

    private static final Pattern PARAMETER = Pattern.compile("(\\w+)=\"([^\"]*)\"");
    private static final Set<String> SUPPORTED_ALGORITHMS = Set.of("rsa-sha256", "hs2019");
    private static final List<String> REQUIRED_HEADERS = List.of("(request-target)", "host", "date", "digest");
    private static final Duration MAX_CLOCK_SKEW = Duration.ofHours(1);

    private final Clock clock;

    public HttpSignatureVerifier(Clock clock) {
        this.clock = clock;
    }

    public SignatureParameters parse(String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new SignatureVerificationException("Missing Signature header");
        }

        Map<String, String> parameters = new HashMap<>();
        Matcher matcher = PARAMETER.matcher(signatureHeader);
        while (matcher.find()) {
            parameters.put(matcher.group(1), matcher.group(2));
        }

        String keyIdParameter = parameters.get("keyId");
        if (keyIdParameter == null) {
            throw new SignatureVerificationException("Signature header is missing keyId");
        }

        String signatureParameter = parameters.get("signature");
        if (signatureParameter == null) {
            throw new SignatureVerificationException("Signature header is missing signature");
        }

        URI keyId;
        try {
            keyId = URI.create(keyIdParameter);
        } catch (IllegalArgumentException e) {
            throw new SignatureVerificationException("Invalid keyId");
        }

        String algorithm = parameters.get("algorithm");
        if (algorithm != null && !SUPPORTED_ALGORITHMS.contains(algorithm)) {
            throw new SignatureVerificationException("Unsupported signature algorithm: " + algorithm);
        }

        String headersParameter = parameters.get("headers");
        List<String> headers = headersParameter == null
                ? List.of()
                : List.of(headersParameter.toLowerCase(Locale.ROOT).trim().split("\\s+"));

        byte[] signature;
        try {
            signature = Base64.getDecoder().decode(signatureParameter);
        } catch (IllegalArgumentException e) {
            throw new SignatureVerificationException("Signature is not valid Base64");
        }

        return new SignatureParameters(keyId, algorithm, headers, signature);
    }

    public void verify(IncomingRequest request, SignatureParameters params, PublicKey publicKey) {
        for (String required : REQUIRED_HEADERS) {
            if (!params.headers().contains(required)) {
                throw new SignatureVerificationException("Signature does not cover required header: " + required);
            }
        }

        String dateHeader = request.headers().get("date");
        if (dateHeader == null) {
            throw new SignatureVerificationException("Missing Date header");
        }

        Instant date;
        try {
            date = ZonedDateTime.parse(dateHeader, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException e) {
            throw new SignatureVerificationException("Invalid Date header");
        }

        if (Duration.between(date, clock.instant()).abs().compareTo(MAX_CLOCK_SKEW) > 0) {
            throw new SignatureVerificationException("Date is outside the allowed window");
        }

        String digestHeader = request.headers().get("digest");
        if (digestHeader == null) {
            throw new SignatureVerificationException("Missing Digest header");
        }

        int separator = digestHeader.indexOf('=');
        if (separator < 0 || !digestHeader.substring(0, separator).equalsIgnoreCase("sha-256")) {
            throw new SignatureVerificationException("Unsupported Digest algorithm");
        }

        byte[] expectedDigest;
        try {
            expectedDigest = Base64.getDecoder().decode(digestHeader.substring(separator + 1));
        } catch (IllegalArgumentException e) {
            throw new SignatureVerificationException("Invalid Digest header");
        }

        if (!MessageDigest.isEqual(expectedDigest, sha256(request.body()))) {
            throw new SignatureVerificationException("Digest does not match the body");
        }
    }

    private static byte[] sha256(byte[] body) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(body);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
