package com.yue.jastodon.federation;

import org.springframework.boot.ssl.pem.PemContent;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;

@Component
public class ActorKeys {

    public ActorKeys(FederationProperties federationProperties) {
        RSAPrivateCrtKey privateKey = loadPrivateKey(federationProperties.privateKeyLocation());
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
