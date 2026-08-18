package com.portfolio.authserver.model.dto;

import java.util.Set;

public record ClientResponse(String id, String clientId, Set<String> redirectUris, Set<String> scopes) {
}
