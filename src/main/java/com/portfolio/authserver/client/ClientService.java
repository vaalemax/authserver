package com.portfolio.authserver.client;

import org.springframework.stereotype.Service;

@Service
public class ClientService {
    public ClientResponse toResponse(Client client) {
        return new ClientResponse(client.getId(), client.getClientId(), client.getRedirectUris(), client.getScopes());
    }
}
