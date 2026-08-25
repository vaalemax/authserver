package com.portfolio.authserver.user.application;

import com.portfolio.authserver.security.RealmAwareUserDetails;
import com.portfolio.authserver.user.domain.AppUser;
import com.portfolio.authserver.user.domain.AppUserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RealmAwareUserLookupService {

    private final AppUserJpaRepository appUserJpaRepository;

    public UserDetails loadUserByRealmAndUsername(String realmName, String username) {
        AppUser appUser = appUserJpaRepository.findByRealm_NameAndUsername(realmName, username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username + " in realm " + realmName));

        Set<GrantedAuthority> authorities = appUser.getRoles().stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toSet());

        return new RealmAwareUserDetails(appUser.getUsername(), appUser.getPassword(), realmName,
                appUser.isEnabled(), authorities);
    }
}