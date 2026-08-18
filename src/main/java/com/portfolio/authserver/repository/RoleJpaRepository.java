package com.portfolio.authserver.repository;

import com.portfolio.authserver.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleJpaRepository extends JpaRepository<Role, String> {
    List<Role> findByRealm_Name(String realmName);
    Optional<Role> findByRealm_NameAndName(String realmName, String name);
}