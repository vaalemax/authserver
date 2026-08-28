package com.portfolio.authserver.user.application;

import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import com.portfolio.authserver.user.domain.AppUser;
import com.portfolio.authserver.user.infrastructure.JpaAppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final JpaAppUserRepository appUserRepository;
    private final RealmRepository realmRepository;
    private final PasswordEncoder passwordEncoder;

    public List<AppUser> listUsers(String realmName){
        return appUserRepository.findByRealmName(realmName);
    }

    public AppUser getUser(String realmName, String username){
        return appUserRepository.findByRealmNameAndUsername(realmName, username)
                .orElseThrow(() -> new NoSuchElementException("User not found: "+username));
    }

    public AppUser createUser(String realmName, String username, String password, Set<String> roles){
        Realm realm = realmRepository.findByName(realmName)
                .orElseThrow(() -> new NoSuchElementException("Realm not found: "+realmName));

        if (appUserRepository.findByRealmNameAndUsername(realmName, username).isPresent()) {
            throw new IllegalStateException("Already existing user: "+username);
        }

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID().toString());
        user.setRealm(realm);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setRoles(roles);

        appUserRepository.save(user);
        return user;
    }

    public AppUser updateUser(String realmName, String username, String password, Boolean enabled){
        AppUser appUser = this.getUser(realmName, username);

        if (password != null && !password.isBlank())
            appUser.setPassword(passwordEncoder.encode(password));
        if(enabled != null && enabled && !appUser.isEnabled())
            appUser.setEnabled(true);

        appUserRepository.save(appUser);
        return appUser;
    }

    public void disableUser(String realmName, String username){
        AppUser appUser = this.getUser(realmName, username);
        appUser.setEnabled(false);

        appUserRepository.save(appUser);
    }

    public static Set<String> splitCommaSeparated(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Set.of(value.split("\\s*,\\s*"));
    }
}
