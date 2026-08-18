package com.portfolio.authserver.bootstrap;

import com.portfolio.authserver.model.AppUser;
import com.portfolio.authserver.model.Realm;
import com.portfolio.authserver.repository.AppUserJpaRepository;
import com.portfolio.authserver.repository.ClientJpaRepository;
import com.portfolio.authserver.repository.JpaRegisteredClientRepository;
import com.portfolio.authserver.service.RealmService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final String DEFAULT_REALM = "aether";

    private static final String MASTER_REALM = "master";

    @Value("${admin.client-secret}")
    private String adminClientSecret;

    @Value("${admin.console-username}")
    private String adminConsoleUsername;

    @Value("${admin.console-password}")
    private String adminConsolePassword;

    @Value("${admin.console-client-secret}")
    private String adminConsoleClientSecret;

    private final AppUserJpaRepository appUserJpaRepository;
    private final ClientJpaRepository clientJpaRepository;
    private final JpaRegisteredClientRepository jpaRegisteredClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final RealmService realmService;

    @Override
    public void run(ApplicationArguments args) {
        Realm realm = seedRealm();
        seedUser(realm);
        seedClient(realm);
        seedMasterRealmAndAdminClient();
    }

    private void seedMasterRealmAndAdminClient() {
        Realm master = realmService.getOrCreateRealm(MASTER_REALM, "Master");
        seedAdminCliClient(master);
        seedConsoleUser(master);
        seedConsoleClient(master);
    }

    private void seedAdminCliClient(Realm master) {
        if (clientJpaRepository.findByRealm_NameAndClientId(MASTER_REALM, "admin-cli").isPresent()) return;

        RegisteredClient adminClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("admin-cli")
                .clientSecret(passwordEncoder.encode(adminClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("admin")
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(15)).build())
                .build();

        jpaRegisteredClientRepository.saveForRealm(adminClient, master);
    }

    private void seedConsoleUser(Realm master) {
        if (appUserJpaRepository.findByRealm_NameAndUsername(MASTER_REALM, adminConsoleUsername).isPresent()) return;

        AppUser admin = new AppUser();
        admin.setId(UUID.randomUUID().toString());
        admin.setRealm(master);
        admin.setUsername(adminConsoleUsername);
        admin.setPassword(passwordEncoder.encode(adminConsolePassword));
        admin.setEnabled(true);
        admin.setRoles(Set.of("ADMIN"));
        appUserJpaRepository.save(admin);
    }

    private void seedConsoleClient(Realm master) {
        if (clientJpaRepository.findByRealm_NameAndClientId(MASTER_REALM, "admin-console").isPresent()) return;

        RegisteredClient consoleClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("admin-console")
                .clientSecret(passwordEncoder.encode(adminConsoleClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:9000/login/oauth2/code/admin-console")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder().requireProofKey(false).requireAuthorizationConsent(false).build())
                .build();

        jpaRegisteredClientRepository.saveForRealm(consoleClient, master);
    }

    private Realm seedRealm() {
        return realmService.getOrCreateRealm(DEFAULT_REALM, "Aether");
    }

    private void seedUser(Realm realm) {
        if (appUserJpaRepository.findByRealm_NameAndUsername(realm.getName(), "vale").isPresent())
            return;
        AppUser vale = new AppUser();
        vale.setId(UUID.randomUUID().toString());
        vale.setRealm(realm);
        vale.setUsername("vale");
        vale.setPassword(passwordEncoder.encode("password"));
        vale.setEnabled(true);
        vale.setRoles(Set.of("ADMIN"));
        appUserJpaRepository.save(vale);
    }

    private void seedClient(Realm realm) {
        if (clientJpaRepository.findByRealm_NameAndClientId(realm.getName(), "aether-client").isPresent())
            return;

        RegisteredClient aetherClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("aether-client")
                .clientSecret(passwordEncoder.encode("secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:8080/authorized")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(true)
                        .build())
                .build();

        jpaRegisteredClientRepository.saveForRealm(aetherClient, realm);
    }
}