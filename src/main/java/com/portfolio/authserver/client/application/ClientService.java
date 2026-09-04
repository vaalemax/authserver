package com.portfolio.authserver.client.application;

import com.portfolio.authserver.authorization.domain.Role;
import com.portfolio.authserver.client.domain.Client;
import com.portfolio.authserver.client.domain.ClientRepository;
import com.portfolio.authserver.client.presentation.mapper.ClientMapper;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import com.portfolio.authserver.realm.application.RealmResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final RealmRepository realmRepository;
    private final PasswordEncoder passwordEncoder;
    private final RealmResolver realmResolver;
    private final ClientMapper clientMapper;

    public List<Client> listClients(String realmName){
        return clientRepository.findByRealmName(realmName);
    }

    public Client getClient(String realmName, String clientId) {
        return clientRepository.findByRealmNameAndClientId(realmName, clientId)
                .orElseThrow(() -> new NoSuchElementException("Client not found: " + clientId));
    }

    public Client createClient(String realmName, String clientId, String clientSecret, Set<String> redirectUris,
                               Set<String> scopes, boolean requireProofKey, boolean requireAuthorizationConsent){
        Realm realm = realmRepository.findByName(realmName)
                .orElseThrow(() -> new NoSuchElementException("Realm not found: "+realmName));

        if (clientRepository.findByRealmNameAndClientId(realmName, clientId).isPresent()) {
            throw new IllegalStateException("Already existing client: "+clientId);
        }

        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUris(uris -> uris.addAll(redirectUris))
                .scopes(s -> s.addAll(scopes))
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(requireProofKey)
                        .requireAuthorizationConsent(requireAuthorizationConsent)
                        .build())
                .build();

        return clientRepository.save(clientMapper.toEntity(registeredClient, realm));
    }

    public Client updateClient(String realmName, String clientId, Set<String> redirectUris, Set<String> scopes,
                               boolean requireProofKey, boolean requireAuthorizationConsent, String newClientSecret) {
        Client existingEntity = getClient(realmName, clientId);
        RegisteredClient existing = clientMapper.toRegisteredClient(existingEntity);

        RegisteredClient.Builder builder = RegisteredClient.from(existing)
                .redirectUris(uris -> { uris.clear(); uris.addAll(redirectUris); })
                .scopes(s -> { s.clear(); s.addAll(scopes); })
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(requireProofKey)
                        .requireAuthorizationConsent(requireAuthorizationConsent)
                        .build());

        if (newClientSecret != null && !newClientSecret.isBlank()) {
            builder.clientSecret(passwordEncoder.encode(newClientSecret));
        }

        Realm realm = realmRepository.findByName(realmName)
                .orElseThrow(() -> new NoSuchElementException("Realm not found: " + realmName));

        return clientRepository.save(clientMapper.toEntity(builder.build(), realm));
    }

    public void deleteClient(String realmName, String clientId){
        Client client = getClient(realmName, clientId);

        clientRepository.delete(client);
    }

    public Client toggleEnabled(String realmName, String clientId) {
        Client client = getClient(realmName, clientId);
        client.setEnabled(!client.isEnabled());
        return clientRepository.save(client);
    }

    public void saveForRealm(RegisteredClient registeredClient, Realm realm) {
        clientRepository.save(clientMapper.toEntity(registeredClient, realm));
    }

    public static Set<String> splitCommaSeparated(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Set.of(value.split("\\s*,\\s*"));
    }


    public void saveRegisteredClient(RegisteredClient registeredClient) {
        clientRepository.save(clientMapper.toEntity(registeredClient, realmResolver.resolveCurrentRealm()));
    }

    public RegisteredClient findRegisteredClientById(String id) {
        return clientRepository.findById(id).map(clientMapper::toRegisteredClient).orElse(null);
    }

    public RegisteredClient findRegisteredClientByClientId(String clientId) {
        String realmName = realmResolver.resolveCurrentRealm().getName();
        return clientRepository.findByRealmNameAndClientId(realmName, clientId)
                .map(clientMapper::toRegisteredClient).orElse(null);
    }
}
