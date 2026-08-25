package com.portfolio.authserver.authorization.domain;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    List<Role> findByRealmName(String realmName);

    Optional<Role> findByRealmNameAndName(String realmName, String name);

    Optional<Role> findByIdAndRealmName(String id, String realmName);

    List<Role> findByPermissionsId(String permissionId);

    Role save(Role role);

    void delete(Role role);
}