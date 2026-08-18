package com.portfolio.authserver.controller;

import com.portfolio.authserver.model.AppUser;
import com.portfolio.authserver.model.dto.CanRequest;
import com.portfolio.authserver.model.dto.CanResult;
import com.portfolio.authserver.repository.AppUserJpaRepository;
import com.portfolio.authserver.service.AuthorizationService;
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
    private final AppUserJpaRepository appUserJpaRepository;

    @PostMapping("/{realm}/auth/can")
    public CanResult can(@PathVariable String realm,
                         @Valid @RequestBody CanRequest request,
                         JwtAuthenticationToken authentication) {
        String username = authentication.getToken().getSubject();

        AppUser user = appUserJpaRepository.findByRealm_NameAndUsername(realm, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return authorizationService.can(user, request.subject(), request.action());
    }
}
