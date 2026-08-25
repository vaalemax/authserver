package com.portfolio.authserver.client.presentation.mapper;

import com.portfolio.authserver.client.domain.Client;
import com.portfolio.authserver.client.presentation.dto.ClientResponse;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {
    public ClientResponse toResponse(Client client) {
        return new ClientResponse(client.getId(), client.getClientId(), client.getRedirectUris(), client.getScopes());
    }
}
