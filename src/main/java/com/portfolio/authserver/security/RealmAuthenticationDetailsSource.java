package com.portfolio.authserver.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;

public class RealmAuthenticationDetailsSource
        implements AuthenticationDetailsSource<HttpServletRequest, RealmAuthenticationDetails> {

    @Override
    public RealmAuthenticationDetails buildDetails(HttpServletRequest request) {
        return new RealmAuthenticationDetails(request);
    }
}