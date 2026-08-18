package com.portfolio.authserver.repository;

import com.portfolio.authserver.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserJpaRepository extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByRealm_NameAndUsername(String realmName, String username);

    List<AppUser> findByRealm_Name(String realmName);
}