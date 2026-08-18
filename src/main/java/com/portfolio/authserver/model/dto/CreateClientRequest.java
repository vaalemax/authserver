package com.portfolio.authserver.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record CreateClientRequest(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotEmpty Set<String> redirectUris,
        @NotEmpty Set<String> scopes,
        boolean requireProofKey,
        boolean requireAuthorizationConsent
) {
}
