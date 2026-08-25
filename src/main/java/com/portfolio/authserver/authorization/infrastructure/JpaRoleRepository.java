package com.portfolio.authserver.authorization.infrastructure;

import com.portfolio.authserver.authorization.domain.Role;
import com.portfolio.authserver.authorization.domain.RoleRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRoleRepository extends RoleRepository, JpaRepository<Role, String> {
}
