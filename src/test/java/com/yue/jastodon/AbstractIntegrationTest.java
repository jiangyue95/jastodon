package com.yue.jastodon;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;

@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final Path TEST_PRIVATE_KEY = writeTestPrivateKey();

    @DynamicPropertySource
    static void testPrivateKey(DynamicPropertyRegistry registry) {
        registry.add("jastodon.federation.private-key-location", () -> TEST_PRIVATE_KEY.toUri().toString());
    }

    private static Path writeTestPrivateKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            PrivateKey key = generator.generateKeyPair().getPrivate();

            String pem = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(key.getEncoded())
                    + "\n-----END PRIVATE KEY-----\n";

            Path file = Files.createTempFile("jastodon-test-key", ".pem");
            file.toFile().deleteOnExit();
            Files.writeString(file, pem);
            return file;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Cannot create the test private key", e);
        }
    }
}
