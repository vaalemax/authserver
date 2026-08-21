package com.portfolio.authserver.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RealmMismatchFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String pathRealm = extractRealm(request.getRequestURI());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean mismatch = auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof RealmAwareUserDetails userDetails
                && !userDetails.getRealmName().equals(pathRealm);

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