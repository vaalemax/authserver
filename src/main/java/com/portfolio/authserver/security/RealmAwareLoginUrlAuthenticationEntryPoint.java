package com.portfolio.authserver.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RealmAwareLoginUrlAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        String realm = extractRealm(request.getRequestURI());
        String originalUrl=request.getRequestURI()
                +(request.getQueryString() != null ? "?"+request.getQueryString() : "");
        String redirectUrl = request.getContextPath() + "/login"
                +"?realm="+(realm!=null ? realm : "")
                +"&continue="+URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }

    private String extractRealm(String requestUri) {
        String[] segments = requestUri.split("/");
        return segments.length > 1 ? segments[1] : null;
    }
}