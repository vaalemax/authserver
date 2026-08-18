package com.portfolio.authserver.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RealmAwareAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String realm = request.getParameter("realm");
        String continueUrl = request.getParameter("continue");

        StringBuilder redirectUrl = new StringBuilder(request.getContextPath() + "/login?error=true");
        if (realm != null && !realm.isBlank()) {
            redirectUrl.append("&realm=").append(URLEncoder.encode(realm, StandardCharsets.UTF_8));
        }
        if (continueUrl != null && !continueUrl.isBlank()) {
            redirectUrl.append("&continue=").append(URLEncoder.encode(continueUrl, StandardCharsets.UTF_8));
        }
        response.sendRedirect(redirectUrl.toString());
    }
}