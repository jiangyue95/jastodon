package com.yue.jastodon.federation;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class PublicKeyPem {

    private static final String BEGIN = "-----BEGIN PUBLIC KEY-----";
    private static final String END = "-----END PUBLIC KEY-----";

    private PublicKeyPem() {}

    static PublicKey parse(String pem) {
        String trimmed = pem.strip();
        if (!trimmed.startsWith(BEGIN) || !trimmed.endsWith(END)) {
            throw new IllegalArgumentException("Expected a PEM public key");
        }
        String base64 = trimmed.substring(BEGIN.length(), trimmed.length() - END.length());

        byte[] der;
        try {
            der = Base64.getMimeDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Public key is not valid Base64", e);
        }

        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Not an RSA public key", e);
        }
    }
}
