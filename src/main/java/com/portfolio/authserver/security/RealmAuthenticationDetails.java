package com.portfolio.authserver.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class RealmAuthenticationDetails extends WebAuthenticationDetails {

    private final String realm;

    public RealmAuthenticationDetails(HttpServletRequest request) {
        super(request); // preserva il comportamento standard (remoteAddress, sessionId)
        this.realm = request.getParameter("realm");
    }

    public String getRealm() {
        return realm;
    }
}