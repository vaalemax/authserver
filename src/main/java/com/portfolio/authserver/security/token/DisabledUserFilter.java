package com.portfolio.authserver.security.token;

import com.portfolio.authserver.user.domain.AppUser;
import com.portfolio.authserver.user.domain.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class DisabledUserFilter extends OncePerRequestFilter {

    private final AppUserRepository appUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String realmName = extractRealm(jwt.getIssuer().toString());
            String username = jwt.getSubject();

            boolean enabled = appUserRepository.findByRealmNameAndUsername(realmName, username)
                    .map(AppUser::isEnabled)
                    .orElse(false);

            if (!enabled) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User is disabled");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractRealm(String issuer) {
        String[] segments = issuer.split("/");
        return segments.length > 0 ? segments[segments.length - 1] : null;
    }
}