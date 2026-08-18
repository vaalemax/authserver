package com.portfolio.authserver.config;

import com.portfolio.authserver.repository.RealmJpaRepository;
import com.portfolio.authserver.security.*;
import com.portfolio.authserver.service.RealmAwareUserLookupService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
                                                        RealmJpaRepository realmJpaRepository) throws Exception {
        JwtDecoder masterRealmJwtDecoder = new MasterRealmJwtDecoder(realmJpaRepository);

        http
                .securityMatcher("/admin/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasAuthority("SCOPE_admin"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(masterRealmJwtDecoder)))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain consoleSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/console/**", "/oauth2/authorization/**", "/login/oauth2/code/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/console/realms").permitAll());

        return http.build();
    }

    // intercetta SOLO gli endpoint del protocollo
    //                         /oauth2/authorize, /oauth2/token, /oauth2/jwks, /.well-known/openid-configuration.
    // securityMatcher() limita esplicitamente il perimetro di questa chain a quegli URL.
    @Bean
    @Order(3)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http, RealmJpaRepository realmJpaRepository) throws Exception {
        OAuth2AuthorizationServerConfigurer configurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http
                .securityMatcher(configurer.getEndpointsMatcher())
                .addFilterBefore(new RealmExistenceFilter(realmJpaRepository), WebAsyncManagerIntegrationFilter.class)
                .with(configurer, (as) -> as.oidc(Customizer.withDefaults()))
                .authorizeHttpRequests((authorize)
                        -> authorize.anyRequest().authenticated())
                .exceptionHandling((exceptions)
                        -> exceptions.defaultAuthenticationEntryPointFor(
                        new RealmAwareLoginUrlAuthenticationEntryPoint(),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                ));
        return http.build();
    }

    @Bean
    @Order(4)
    public SecurityFilterChain authorizationApiSecurityFilterChain(HttpSecurity http,
                                                                   RealmJpaRepository realmJpaRepository) throws Exception {
        http
                .securityMatcher("/*/auth/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(
                        JwtIssuerAuthenticationManagerResolver.fromTrustedIssuers(
                                issuer -> realmJpaRepository.findAll().stream().anyMatch(r -> issuer.endsWith(r.getName()))
                        )
                ))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    @Order(5) // fallback: tutto ciò che NON matcha la chain 2(era 1)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((authorize)
                        -> authorize
                        .requestMatchers("/login","/favicon.ico","/.well-known/**","/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .authenticationDetailsSource(new RealmAuthenticationDetailsSource())
                        .successHandler(new RealmAwareAuthenticationSuccessHandler())
                        .failureHandler(new RealmAwareAuthenticationFailureHandler())
                );
        return http.build();
    }


    // Il "Client" registrato —> concettualmente identico a un Client dentro un Realm di Keycloak.
    // In-memory per ora, JPA nella Sessione 3.
    /*@Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
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
                        .requireProofKey(true)             // PKCE obbligatorio: best practice OAuth 2.1
                        .requireAuthorizationConsent(true)  // mostra la consent screen, utile per vedere il flusso
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(aetherClient);
    }*/

    /*
    // Coppia di chiavi RSA generata a runtime per firmare i JWT (RS256).
    // In produzione va caricata da un keystore persistente — se la rigeneri
    // a ogni riavvio, tutti i token emessi prima diventano invalidabili
    // perché il JWKS espone chiavi diverse.
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }*/

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // L'issuer: finisce nel claim "iss" di ogni JWT e nel discovery document.
    // Deve combaciare esattamente con l'URL su cui gira il server.
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                //.issuer("http://localhost:9000")
                .multipleIssuersAllowed(true)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(RealmAwareUserLookupService userLookupService,
                                                         PasswordEncoder passwordEncoder) {
        return new RealmAwareAuthenticationProvider(userLookupService, passwordEncoder);
    }

    // Un solo utente in-memory per testare il login. Sessione 3: tabella "users" via JPA.
    /*@Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails vale = User.withUsername("vale")
                .password(passwordEncoder.encode("password"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(vale);
    }*/
}