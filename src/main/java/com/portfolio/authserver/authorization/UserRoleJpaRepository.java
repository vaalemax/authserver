package com.portfolio.authserver.authorization;

import com.portfolio.authserver.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRoleJpaRepository extends JpaRepository<UserRole, String> {
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

    Optional<UserRole> findByIdAndAppUser_Realm_NameAndAppUser_Username(String id, String realmName, String username);
}
