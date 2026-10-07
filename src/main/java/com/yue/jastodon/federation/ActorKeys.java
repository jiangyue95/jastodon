package com.yue.jastodon.federation;

import org.springframework.boot.ssl.pem.PemContent;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

@Component
public class ActorKeys {

    private final String publicKeyPem;

    public ActorKeys(FederationProperties federationProperties) {
        RSAPrivateCrtKey privateKey = loadPrivateKey(federationProperties.privateKeyLocation());
        this.publicKeyPem = toPem(derivePublicKey(privateKey));
    }

    public String publicKeyPem() {
        return publicKeyPem;
    }

    private static PublicKey derivePublicKey(RSAPrivateCrtKey privateKey) {
        try {
            RSAPublicKeySpec spec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot derive the public key", e);
        }
    }

    private static String toPem(PublicKey publicKey) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(publicKey.getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
    }

    private static RSAPrivateCrtKey loadPrivateKey(Resource location) {
        PrivateKey key = readPrivateKey(location);
        if (key instanceof RSAPrivateCrtKey rsaKey) {
            return rsaKey;
        }
        throw new IllegalStateException("jastodon.federation.private-key-location must point to an RSA private key: "
                + location.getDescription());
    }

    private static PrivateKey readPrivateKey(Resource location) {
        try (InputStream in = location.getInputStream()) {
            return PemContent.load(in).getPrivateKey();
        } catch (IOException | IllegalStateException e) {
            throw new IllegalStateException("Cannot read private key from " + location.getDescription(), e);
        }
    }
}
