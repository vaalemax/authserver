package com.portfolio.authserver.user.domain;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository {
    Optional<AppUser> findByRealmNameAndUsername(String realmName, String username);

    List<AppUser> findByRealmName(String realmName);

    AppUser save(AppUser appUser);

    void delete(AppUser appUser);
}