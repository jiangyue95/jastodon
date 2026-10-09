package com.yue.jastodon.federation;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Clock;
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
}
