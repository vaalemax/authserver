package com.portfolio.authserver.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RealmMismatchFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String pathRealm = extractRealm(request.getRequestURI());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean mismatch = false;

        if (auth != null && auth.isAuthenticated()) {
            if (auth.getPrincipal() instanceof RealmAwareUserDetails userDetails) {
                mismatch = !userDetails.getRealmName().equals(pathRealm);
            } else if (auth instanceof OAuth2AuthenticationToken) {
                mismatch = !"master".equals(pathRealm);
            }
        }

        if (mismatch) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractRealm(String requestUri) {
        String[] segments = requestUri.split("/");
        return segments.length > 1 ? segments[1] : null;
    }
}