package com.portfolio.authserver.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public final class RsaKeyGenerator {

    private RsaKeyGenerator() {}

    public static KeyPair generate() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot generate RSA pair", ex);
        }
    }
}