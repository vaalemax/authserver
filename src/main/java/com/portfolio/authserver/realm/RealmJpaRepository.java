package com.portfolio.authserver.realm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RealmJpaRepository extends JpaRepository<Realm, String> {
    Optional<Realm> findByName(String name);
}
