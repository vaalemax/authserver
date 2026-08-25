package com.portfolio.authserver.client;

import org.springframework.stereotype.Component;

@Component
public class ClientMapper {
    public ClientResponse toResponse(Client client) {
        return new ClientResponse(client.getId(), client.getClientId(), client.getRedirectUris(), client.getScopes());
    }
}
