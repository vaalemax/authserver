package com.portfolio.authserver.client.presentation.dto;

import java.util.Set;

public record UpdateClientRequest(Set<String> redirectUris, Set<String> scopes,
                                  Boolean requireProofKey, Boolean requireAuthorizationConsent,
                                  String newClientSecret) {}