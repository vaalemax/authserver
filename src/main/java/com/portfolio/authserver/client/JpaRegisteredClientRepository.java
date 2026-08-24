package com.portfolio.authserver.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.authserver.realm.Realm;
import com.portfolio.authserver.realm.RealmResolver;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final ClientJpaRepository clientJpaRepository;
    private final ObjectMapper objectMapper;
    private final RealmResolver realmResolver;

    public JpaRegisteredClientRepository(ClientJpaRepository clientJpaRepository, RealmResolver realmResolver) {
        this.clientJpaRepository = clientJpaRepository;
        this.realmResolver = realmResolver;

        this.objectMapper = new ObjectMapper();
        ClassLoader classLoader = JpaRegisteredClientRepository.class.getClassLoader();
        this.objectMapper.registerModules(SecurityJackson2Modules.getModules(classLoader));
        this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        clientJpaRepository.save(toEntity(registeredClient, realmResolver.resolveCurrentRealm()));
    }

    public void saveForRealm(RegisteredClient registeredClient, Realm realm) {
        clientJpaRepository.save(toEntity(registeredClient, realm));
    }

    @Override
    public RegisteredClient findById(String id) {
        return clientJpaRepository.findById(id).map(this::toRegisteredClient).orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        String realmName = realmResolver.resolveCurrentRealm().getName();
        return clientJpaRepository.findByRealm_NameAndClientId(realmName, clientId)
                .map(this::toRegisteredClient).orElse(null);
    }

    public RegisteredClient findByRealmAndClientId(Realm realm, String clientId) {
        return clientJpaRepository.findByRealm_NameAndClientId(realm.getName(), clientId)
                .map(this::toRegisteredClient)
                .orElse(null);
    }

    private Client toEntity(RegisteredClient rc, Realm realm) {
        Client entity = new Client();
        entity.setId(rc.getId());
        entity.setRealm(realm);
        entity.setClientId(rc.getClientId());
        entity.setClientIdIssuedAt(rc.getClientIdIssuedAt());
        entity.setClientSecret(rc.getClientSecret());
        entity.setClientSecretExpiresAt(rc.getClientSecretExpiresAt());
        entity.setClientName(rc.getClientName());
        entity.setClientAuthenticationMethods(rc.getClientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::getValue).collect(Collectors.toSet()));
        entity.setAuthorizationGrantTypes(rc.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::getValue).collect(Collectors.toSet()));
        entity.setRedirectUris(new HashSet<>(rc.getRedirectUris()));
        entity.setScopes(new HashSet<>(rc.getScopes()));
        entity.setClientSettings(writeMap(rc.getClientSettings().getSettings()));
        entity.setTokenSettings(writeMap(rc.getTokenSettings().getSettings()));
        return entity;
    }

    private RegisteredClient toRegisteredClient(Client entity) {
        Set<ClientAuthenticationMethod> authMethods = entity.getClientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::new).collect(Collectors.toSet());
        Set<AuthorizationGrantType> grantTypes = entity.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::new).collect(Collectors.toSet());

        return RegisteredClient.withId(entity.getId())
                .clientId(entity.getClientId())
                .clientIdIssuedAt(entity.getClientIdIssuedAt())
                .clientSecret(entity.getClientSecret())
                .clientSecretExpiresAt(entity.getClientSecretExpiresAt())
                .clientName(entity.getClientName())
                .clientAuthenticationMethods(methods -> methods.addAll(authMethods))
                .authorizationGrantTypes(grants -> grants.addAll(grantTypes))
                .redirectUris(uris -> uris.addAll(entity.getRedirectUris()))
                .scopes(scopes -> scopes.addAll(entity.getScopes()))
                .clientSettings(ClientSettings.withSettings(readMap(entity.getClientSettings())).build())
                .tokenSettings(TokenSettings.withSettings(readMap(entity.getTokenSettings())).build())
                .build();
    }

    private String writeMap(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize client settings", ex);
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot deserialize client settings", ex);
        }
    }
}
