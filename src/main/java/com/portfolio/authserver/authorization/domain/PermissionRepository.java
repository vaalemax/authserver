package com.portfolio.authserver.authorization.domain;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository {

    List<Permission> findByRealmName(String realmName);

    Optional<Permission> findByRealmNameAndSubjectAndAction(String realmName, String subject, String action);

    Optional<Permission> findByIdAndRealmName(String id, String realmName);

    Permission save(Permission permission);

    void delete(Permission permission);
}
