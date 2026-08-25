package com.portfolio.authserver.client;

import com.portfolio.authserver.client.application.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final ClientService clientService;

    @Override
    public void save(RegisteredClient registeredClient) {
        clientService.save(registeredClient);
    }

    @Override
    public RegisteredClient findById(String id) {
        return clientService.findById(id);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return clientService.findByClientId(clientId);
    }
}
