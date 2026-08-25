package com.portfolio.authserver.authorization.infrastructure;

import com.portfolio.authserver.authorization.domain.Permission;
import com.portfolio.authserver.authorization.domain.PermissionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPermissionRepository extends PermissionRepository, JpaRepository<Permission, String> {
}
