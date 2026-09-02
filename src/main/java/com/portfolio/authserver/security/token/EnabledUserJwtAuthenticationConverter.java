package com.portfolio.authserver.security.token;

import com.portfolio.authserver.user.domain.AppUser;
import com.portfolio.authserver.user.domain.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;

@RequiredArgsConstructor
public class EnabledUserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AppUserRepository appUserRepository;
    private final String realmName;
    private final JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String username = jwt.getSubject();

        AppUser user = appUserRepository.findByRealmNameAndUsername(realmName, username)
                .orElseThrow(() -> invalidToken("Unknown user: " + username));

        if (!user.isEnabled()) {
            throw invalidToken("User is disabled: " + username);
        }

        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);
        return new JwtAuthenticationToken(jwt, authorities, username);
    }

    private OAuth2AuthenticationException invalidToken(String description) {
        // "invalid_token" standard error code RFC 6750
        OAuth2Error error = new OAuth2Error("invalid_token", description, null);
        return new OAuth2AuthenticationException(error);
    }
}