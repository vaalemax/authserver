package com.portfolio.authserver.realm;

import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class RealmExistenceFilter extends OncePerRequestFilter {

    private final RealmRepository realmJpaRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String realmName = extractRealm(request.getRequestURI());
        boolean valid = realmName != null && realmJpaRepository.findByName(realmName)
                .map(Realm::isEnabled)
                .orElse(false);

        if (!valid) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Realm not found: " + realmName);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractRealm(String requestUri) {
        String[] segments = requestUri.split("/");
        return segments.length > 1 ? segments[1] : null;
    }
}