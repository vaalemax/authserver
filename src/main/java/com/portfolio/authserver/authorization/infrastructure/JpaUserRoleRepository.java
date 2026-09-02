package com.portfolio.authserver.authorization.infrastructure;

import com.portfolio.authserver.authorization.domain.UserRole;
import com.portfolio.authserver.authorization.domain.UserRoleRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRoleRepository extends UserRoleRepository, JpaRepository<UserRole, String> {
}
