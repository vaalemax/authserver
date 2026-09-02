package com.portfolio.authserver.security.login;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

@Getter
public class RealmAuthenticationDetails extends WebAuthenticationDetails {

    private final String realm;

    public RealmAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.realm = request.getParameter("realm");
    }

}