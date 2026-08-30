package com.portfolio.authserver.security.token;

import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import com.portfolio.authserver.user.domain.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class EnabledUserJwtIssuerResolver implements AuthenticationManagerResolver<String> {

    private final RealmRepository realmRepository;
    private final AppUserRepository appUserRepository;
    private final Map<String, AuthenticationManager> cache = new ConcurrentHashMap<>();

    @Override
    public AuthenticationManager resolve(String issuer) {
        return cache.computeIfAbsent(issuer, this::buildManager);
    }

    private AuthenticationManager buildManager(String issuer) {
        Realm realm = realmRepository.findAll().stream()
                .filter(r -> issuer.endsWith(r.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Untrusted issuer: " + issuer));

        JwtDecoder decoder = buildDecoder(realm, issuer);
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(
                new EnabledUserJwtAuthenticationConverter(appUserRepository, realm.getName()));

        return new ProviderManager(provider);
    }

    private JwtDecoder buildDecoder(Realm realm, String issuer) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(realm.getRsaPublicKey())));

            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
            return decoder;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot build decoder for realm "+realm.getName(), ex);
        }
    }
}