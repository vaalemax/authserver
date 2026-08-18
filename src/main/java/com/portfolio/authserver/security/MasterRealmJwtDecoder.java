package com.portfolio.authserver.security;

import com.portfolio.authserver.realm.Realm;
import com.portfolio.authserver.realm.RealmJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.*;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@RequiredArgsConstructor
public class MasterRealmJwtDecoder implements JwtDecoder {

    private final RealmJpaRepository realmJpaRepository;
    private volatile JwtDecoder delegate;

    @Override
    public Jwt decode(String token) throws JwtException {
        return getDelegate().decode(token);
    }

    private JwtDecoder getDelegate() {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    delegate = buildDecoder();
                }
            }
        }
        return delegate;
    }

    private JwtDecoder buildDecoder() {
        Realm master = realmJpaRepository.findByName("master")
                .orElseThrow(() -> new IllegalStateException("Realm 'master' not found"));

        RSAPublicKey publicKey = toPublicKey(master.getRsaPublicKey());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("http://localhost:9000/master"));
        return decoder;
    }

    private RSAPublicKey toPublicKey(String base64) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to reconstruct the master's realm public key", ex);
        }
    }
}