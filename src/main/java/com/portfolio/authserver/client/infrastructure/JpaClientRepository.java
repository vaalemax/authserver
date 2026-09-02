package com.portfolio.authserver.client.infrastructure;

import com.portfolio.authserver.client.domain.Client;
import com.portfolio.authserver.client.domain.ClientRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaClientRepository extends ClientRepository, JpaRepository<Client, String> {
}
