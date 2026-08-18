package com.portfolio.authserver.client;

import com.portfolio.authserver.realm.Realm;
import com.portfolio.authserver.realm.RealmJpaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/realms/{realmName}/clients")
@RequiredArgsConstructor
public class ClientAdminController {

    private final RealmJpaRepository realmJpaRepository;
    private final ClientJpaRepository clientJpaRepository;
    private final JpaRegisteredClientRepository jpaRegisteredClientRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public List<ClientResponse> findClients(@PathVariable String realmName) {
        return clientJpaRepository.findByRealm_Name(realmName).stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse createClient(@PathVariable String realmName, @Valid @RequestBody CreateClientRequest request) {
        Realm realm = findRealmByName(realmName);

        if (clientJpaRepository.findByRealm_NameAndClientId(realmName, request.clientId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Client already existing in this realm: " + request.clientId());
        }

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(request.clientId())
                .clientSecret(passwordEncoder.encode(request.clientSecret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(request.requireProofKey())
                        .requireAuthorizationConsent(request.requireAuthorizationConsent())
                        .build());

        request.redirectUris().forEach(builder::redirectUri);
        request.scopes().forEach(builder::scope);

        jpaRegisteredClientRepository.saveForRealm(builder.build(), realm);

        return toResponse(clientJpaRepository.findByRealm_NameAndClientId(realmName, request.clientId())
                .orElseThrow());
    }

    private Realm findRealmByName(String realmName) {
        return realmJpaRepository.findByName(realmName)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found: " + realmName));
    }

    private ClientResponse toResponse(Client entity) {
        return new ClientResponse(entity.getId(), entity.getClientId(), entity.getRedirectUris(), entity.getScopes());
    }
}