package com.portfolio.authserver.authorization.domain;

import com.portfolio.authserver.user.domain.AppUser;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRoleRepository {
    @Query(value="""
    select ur
    from UserRole ur
    where ur.appUser = :appUser
    and ur.validFrom <= :now
    and (ur.validTo is null or ur.validTo >= :now)
    """)
    List<UserRole> findActiveForUser(AppUser appUser, Instant now);

    List<UserRole> findByAppUser(AppUser appUser);

    List<UserRole> findByRole(Role role);

    Optional<UserRole> findByIdAndAppUserRealmNameAndAppUserUsername(String id, String realmName, String username);

    UserRole save(UserRole userRole);

    void delete(UserRole userRole);
}
