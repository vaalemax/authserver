package com.portfolio.authserver.client.presentation;

import com.portfolio.authserver.client.application.ClientService;
import com.portfolio.authserver.client.domain.ClientRepository;
import com.portfolio.authserver.client.presentation.dto.ClientResponse;
import com.portfolio.authserver.client.presentation.dto.CreateClientRequest;
import com.portfolio.authserver.client.presentation.mapper.ClientMapper;
import com.portfolio.authserver.realm.Realm;
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

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientService clientService;
    private final ClientMapper clientMapper;

    @GetMapping
    public List<ClientResponse> findClients(@PathVariable String realmName) {
        return clientRepository.findByRealmName(realmName).stream().map(clientMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse createClient(@PathVariable String realmName, @Valid @RequestBody CreateClientRequest request) {
        Realm realm = clientService.findRealmByName(realmName);

        if (clientRepository.findByRealmNameAndClientId(realmName, request.clientId()).isPresent()) {
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

        clientService.saveForRealm(builder.build(), realm);

        return clientMapper.toResponse(clientRepository.findByRealmNameAndClientId(realmName, request.clientId())
                .orElseThrow());
    }
}