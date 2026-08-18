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

    // Va bene con pochi realm; con centinaia servirebbe una query mirata invece di findAll()
    public Realm resolveCurrentRealm() {
        AuthorizationServerContext context = AuthorizationServerContextHolder.getContext();
        if (context == null || context.getIssuer() == null) {
            throw new IllegalStateException("Nessun contesto authorization server disponibile: impossibile risolvere il realm");
        }
        String issuer = context.getIssuer();
        return realmJpaRepository.findAll().stream()
                .filter(realm -> issuer.endsWith(realm.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nessun realm corrisponde all'issuer: " + issuer));
    }
}