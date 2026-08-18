package com.portfolio.authserver.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealmAwareUserLookupService {

    private final AppUserJpaRepository appUserJpaRepository;

    public UserDetails loadUserByRealmAndUsername(String realmName, String username) {
        AppUser appUser = appUserJpaRepository.findByRealm_NameAndUsername(realmName, username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username + " in realm " + realmName));

        return User.withUsername(appUser.getUsername())
                .password(appUser.getPassword())
                .authorities(appUser.getRoles().stream().map(r -> "ROLE_" + r).toArray(String[]::new))
                .disabled(!appUser.isEnabled())
                .build();
    }
}