package com.portfolio.authserver.repository;

import com.portfolio.authserver.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientJpaRepository extends JpaRepository<Client, String> {
    Optional<Client> findByRealm_NameAndClientId(String realmName, String clientId);

    List<Client> findByRealm_Name(String realmName);
}
