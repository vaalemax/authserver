package com.portfolio.authserver.client.domain;

import java.util.List;
import java.util.Optional;

public interface ClientRepository{
    Optional<Client> findByRealmNameAndClientId(String realmName, String clientId);

    List<Client> findByRealmName(String realmName);

    Client save(Client client);

    void delete(Client client);

    Optional<Client> findById(String id);
}
