package com.portfolio.authserver.security.login;

import com.portfolio.authserver.user.application.RealmAwareUserLookupService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class LoginConfig {

    @Bean
    public AuthenticationProvider authenticationProvider(RealmAwareUserLookupService userLookupService,
                                                         PasswordEncoder passwordEncoder) {
        return new RealmAwareAuthenticationProvider(userLookupService, passwordEncoder);
    }
}