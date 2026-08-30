package com.portfolio.authserver.config;

import com.portfolio.authserver.realm.infrastructure.RealmExistenceFilter;
import com.portfolio.authserver.realm.domain.RealmRepository;
import com.portfolio.authserver.security.*;
import com.portfolio.authserver.security.login.*;
import com.portfolio.authserver.security.token.DisabledUserFilter;
import com.portfolio.authserver.security.token.EnabledUserJwtIssuerResolver;
import com.portfolio.authserver.security.token.MasterRealmJwtDecoder;
import com.portfolio.authserver.user.application.RealmAwareUserLookupService;
import com.portfolio.authserver.user.domain.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.LinkedHashMap;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
                                                        RealmRepository realmRepository) throws Exception {
        JwtDecoder masterRealmJwtDecoder = new MasterRealmJwtDecoder(realmRepository);

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
                .authorizeHttpRequests(authorize -> authorize.anyRequest().access((authentication, context) -> {
                    Authentication auth = authentication.get();
                    boolean isConsoleLogin = auth instanceof OAuth2AuthenticationToken oAuthToken
                            && "admin-console".equals(oAuthToken.getAuthorizedClientRegistrationId());
                    return new AuthorizationDecision(isConsoleLogin);
                }))
                .oauth2Login(oauth2 ->
                        oauth2.defaultSuccessUrl("/console/realms", true))
                .logout(logout -> logout.logoutSuccessUrl("/login").permitAll());

        return http.build();
    }

    // intercetta SOLO gli endpoint del protocollo
    //                         /oauth2/authorize, /oauth2/token, /oauth2/jwks, /.well-known/openid-configuration.
    // securityMatcher() limita esplicitamente il perimetro di questa chain a quegli URL.
    @Bean
    @Order(3)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http, RealmRepository realmRepository, AppUserRepository appUserRepository) throws Exception {
        OAuth2AuthorizationServerConfigurer configurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http
                .securityMatcher(configurer.getEndpointsMatcher())
                .addFilterAfter(new DisabledUserFilter(appUserRepository), BearerTokenAuthenticationFilter.class)
                .addFilterBefore(new RealmExistenceFilter(realmRepository), WebAsyncManagerIntegrationFilter.class)
                .with(configurer, (as) -> as.oidc(Customizer.withDefaults()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
                .exceptionHandling((exceptions) -> {
                    LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
                    entryPoints.put(new MediaTypeRequestMatcher(MediaType.TEXT_HTML), new RealmAwareLoginUrlAuthenticationEntryPoint());
                    DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(entryPoints);
                    entryPoint.setDefaultEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
                    exceptions.authenticationEntryPoint(entryPoint);
                });
        return http.build();
    }

    @Bean
    @Order(4)
    public SecurityFilterChain authorizationApiSecurityFilterChain(HttpSecurity http,
                                                                   RealmRepository realmRepository,
                                                                   AppUserRepository appUserRepository)throws Exception {
        http
                .securityMatcher("/*/auth/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(
                        new JwtIssuerAuthenticationManagerResolver(
                                new EnabledUserJwtIssuerResolver(realmRepository, appUserRepository))
                ))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    @Order(5)
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${admin.console-client-secret}") String consoleClientSecret) {

        ClientRegistration adminConsole = ClientRegistration.withRegistrationId("admin-console")
                .clientId("admin-console")
                .clientSecret(consoleClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile")
                .authorizationUri("http://localhost:9000/master/oauth2/authorize")
                .tokenUri("http://localhost:9000/master/oauth2/token")
                .jwkSetUri("http://localhost:9000/master/oauth2/jwks")
                .userInfoUri("http://localhost:9000/master/userinfo")
                .userNameAttributeName("sub")
                .issuerUri("http://localhost:9000/master")
                .clientName("Admin Console")
                .build();

        return new InMemoryClientRegistrationRepository(adminConsole);
    }

    private static String extractRealm(String requestUri) {
        String[] segments = requestUri.split("/");
        return segments.length > 1 ? segments[1] : null;
    }
}