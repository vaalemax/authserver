package com.portfolio.authserver.security.console;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class ConsoleOAuth2Config {

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
}