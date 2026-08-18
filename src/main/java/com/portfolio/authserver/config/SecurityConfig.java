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
                .logout(logout -> logout.logoutSuccessUrl("/login").permitAll());

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
}