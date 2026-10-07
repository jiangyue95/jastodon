package com.yue.jastodon;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final KeyPair TEST_KEY_PAIR = generateKeyPair();
    private static final Path TEST_PRIVATE_KEY = writePrivateKey(TEST_KEY_PAIR.getPrivate());

    @DynamicPropertySource
    static void testPrivateKey(DynamicPropertyRegistry registry) {
        registry.add("jastodon.federation.private-key-location", () -> TEST_PRIVATE_KEY.toUri().toString());
    }

    protected static PublicKey testPublicKey() {
        return TEST_KEY_PAIR.getPublic();
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot generate test key pair", e);
        }
    }

    private static Path writePrivateKey(PrivateKey key) {
        try {
            String pem = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(key.getEncoded())
                    + "\n-----END PRIVATE KEY-----\n";

            Path file = Files.createTempFile("jastodon-test-key", ".pem");
            file.toFile().deleteOnExit();
            Files.writeString(file, pem);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write the test private key", e);
        }
    }
}
