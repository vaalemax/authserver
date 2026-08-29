package com.portfolio.authserver.security.token;

import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

public class OfflineAccessRefreshTokenGenerator implements OAuth2TokenGenerator<OAuth2RefreshToken> {

    private static final String OFFLINE_ACCESS_SCOPE = "offline_access";

    private final OAuth2RefreshTokenGenerator delegate = new OAuth2RefreshTokenGenerator();

    @Override
    public OAuth2RefreshToken generate(OAuth2TokenContext context) {
        if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
            return null; // another generator in the chain is handling it
        }
        if (!context.getAuthorizedScopes().contains(OFFLINE_ACCESS_SCOPE)) {
            return null; // offline_access not requested: no refresh token emitted
        }
        return delegate.generate(context);
    }
}