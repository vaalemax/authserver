package com.portfolio.authserver.security.token;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.application.RealmResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RealmAwareJwkSource implements JWKSource<SecurityContext> {

    private final RealmResolver realmResolver;

    @Override
    public List<JWK> get(JWKSelector jwkSelector, SecurityContext context) throws KeySourceException {
        Realm realm = realmResolver.resolveCurrentRealm();
        return jwkSelector.select(new JWKSet(toRsaKey(realm)));
    }

    private RSAKey toRsaKey(Realm realm) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(realm.getRsaPublicKey())));
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(realm.getRsaPrivateKey())));

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(realm.getRsaKeyId())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to reconstruct the realm's RSA key " + realm.getName(), ex);
        }
    }
}