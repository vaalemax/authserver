package com.portfolio.authserver.authorization.presentation;

import com.portfolio.authserver.authorization.application.AuthorizationService;
import com.portfolio.authserver.authorization.presentation.dto.CanRequest;
import com.portfolio.authserver.authorization.presentation.dto.CanResult;
import com.portfolio.authserver.user.domain.AppUser;
import com.portfolio.authserver.user.domain.AppUserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationService authorizationService;
    private final AppUserRepository appUserRepository;

    @PostMapping("/{realm}/auth/can")
    public CanResult can(@PathVariable String realm,
                         @Valid @RequestBody CanRequest request,
                         JwtAuthenticationToken authentication) {
        String username = authentication.getToken().getSubject();

        AppUser user = appUserRepository.findByRealmNameAndUsername(realm, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return authorizationService.can(user, request.subject(), request.action());
    }
}
