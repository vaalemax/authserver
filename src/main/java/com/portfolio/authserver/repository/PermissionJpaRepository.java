package com.portfolio.authserver.repository;

import com.portfolio.authserver.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionJpaRepository extends JpaRepository<Permission, String> {
    List<Permission> findByRealm_Name(String realmName);
    Optional<Permission> findByRealm_NameAndSubjectAndAction(String realmName, String subject, String action);
}
