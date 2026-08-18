package com.portfolio.authserver.client;

import java.util.Set;

public record ClientResponse(String id, String clientId, Set<String> redirectUris, Set<String> scopes) {
}
