package com.portfolio.authserver.authorization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleJpaRepository extends JpaRepository<Role, String> {
    List<Role> findByRealm_Name(String realmName);

    Optional<Role> findByRealm_NameAndName(String realmName, String name);

    Optional<Role> findByIdAndRealm_Name(String id, String realmName);

    List<Role> findByPermissions_Id(String permissionId);
}