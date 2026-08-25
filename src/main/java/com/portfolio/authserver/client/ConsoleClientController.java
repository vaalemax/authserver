package com.portfolio.authserver.client;

import com.portfolio.authserver.realm.Realm;
import com.portfolio.authserver.realm.RealmJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.UUID;

@Controller
@RequestMapping("/console/realms/{realmName}/clients")
@RequiredArgsConstructor
public class ConsoleClientController {

    private final RealmJpaRepository realmJpaRepository;
    private final ClientJpaRepository clientJpaRepository;
    private final JpaRegisteredClientRepository jpaRegisteredClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientMapper clientMapper;

    @GetMapping
    public String list(@PathVariable String realmName, Model model) {
        model.addAttribute("realmName", realmName);
        model.addAttribute("clients", clientJpaRepository.findByRealm_Name(realmName));
        return "console/clients";
    }

    @PostMapping
    public String create(@PathVariable String realmName,
                         @RequestParam String clientId, @RequestParam String clientSecret,
                         @RequestParam String redirectUri, @RequestParam(defaultValue = "openid,profile") String scopes,
                         RedirectAttributes redirectAttributes) {
        Realm realm = realmJpaRepository.findByName(realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found"));

        if (clientJpaRepository.findByRealm_NameAndClientId(realmName, clientId).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Already existing client: " + clientId);
            return "redirect:/console/realms/" + realmName + "/clients";
        }

        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(redirectUri)
                .scopes(s -> s.addAll(Arrays.asList(scopes.split(","))))
                .clientSettings(ClientSettings.builder().requireProofKey(false).requireAuthorizationConsent(false).build())
                .build();

        jpaRegisteredClientRepository.saveForRealm(registeredClient, realm);
        redirectAttributes.addFlashAttribute("successMessage", "Client '" + clientId + "' created");
        return "redirect:/console/realms/" + realmName + "/clients";
    }

    @PatchMapping("/{clientId}")
    public ClientResponse update(@PathVariable String realmName, @PathVariable String clientId,
                                 @RequestBody UpdateClientRequest request) {
        Realm realm = realmJpaRepository.findByName(realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found"));

        RegisteredClient existing = jpaRegisteredClientRepository.findByRealmAndClientId(realm, clientId);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId);
        }

        RegisteredClient.Builder builder = RegisteredClient.from(existing);

        if (request.redirectUris() != null) {
            builder.redirectUris(uris -> { uris.clear(); uris.addAll(request.redirectUris()); });
        }
        if (request.scopes() != null) {
            builder.scopes(scopes -> { scopes.clear(); scopes.addAll(request.scopes()); });
        }
        if (request.requireProofKey() != null || request.requireAuthorizationConsent() != null) {
            ClientSettings.Builder settings = ClientSettings.withSettings(existing.getClientSettings().getSettings());
            if (request.requireProofKey() != null) settings.requireProofKey(request.requireProofKey());
            if (request.requireAuthorizationConsent() != null)
                settings.requireAuthorizationConsent(request.requireAuthorizationConsent());
            builder.clientSettings(settings.build());
        }
        if (request.newClientSecret() != null && !request.newClientSecret().isBlank()) {
            builder.clientSecret(passwordEncoder.encode(request.newClientSecret()));
        }

        jpaRegisteredClientRepository.saveForRealm(builder.build(), realm);
        return clientMapper.toResponse(clientJpaRepository.findByRealm_NameAndClientId(realmName, clientId).orElseThrow());
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> delete(@PathVariable String realmName, @PathVariable String clientId) {
        Client client = clientJpaRepository.findByRealm_NameAndClientId(realmName, clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId));
        clientJpaRepository.delete(client);
        return ResponseEntity.noContent().build();
    }
}