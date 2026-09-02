package com.portfolio.authserver.user.infrastructure;

import com.portfolio.authserver.user.domain.AppUser;
import com.portfolio.authserver.user.domain.AppUserRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAppUserRepository extends AppUserRepository, JpaRepository<AppUser, String> {
}
