package com.portfolio.authserver.realm.application;

import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealmResolver {

    private final RealmRepository realmRepository;

    // fine with few realms, remove findAll() to scale with hundreds of realms
    public Realm resolveCurrentRealm() {
        AuthorizationServerContext context = AuthorizationServerContextHolder.getContext();
        if (context == null || context.getIssuer() == null) {
            throw new IllegalStateException("Authorization server context unavailable: cannot resolve realm");
        }
        String issuer = context.getIssuer();
        return realmRepository.findAll().stream()
                .filter(realm -> issuer.endsWith(realm.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No realm matches issuer: " + issuer));
    }
}