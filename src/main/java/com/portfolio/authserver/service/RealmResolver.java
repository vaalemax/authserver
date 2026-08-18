package com.portfolio.authserver.service;

import com.portfolio.authserver.model.Realm;
import com.portfolio.authserver.repository.RealmJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealmResolver {

    private final RealmJpaRepository realmJpaRepository;

    // alright with few realms, remove findAll() to scale with hundreds of realms
    public Realm resolveCurrentRealm() {
        AuthorizationServerContext context = AuthorizationServerContextHolder.getContext();
        if (context == null || context.getIssuer() == null) {
            throw new IllegalStateException("Authorization server context unavailable: cannot resolve realm");
        }
        String issuer = context.getIssuer();
        return realmJpaRepository.findAll().stream()
                .filter(realm -> issuer.endsWith(realm.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No realm matches issuer: " + issuer));
    }
}