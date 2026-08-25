package com.portfolio.authserver.user;

import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/realms/{realmName}/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final AppUserJpaRepository appUserJpaRepository;
    private final RealmRepository realmRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public List<UserResponse> findUsers(@PathVariable String realmName) {
        return appUserJpaRepository.findByRealm_Name(realmName).stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@PathVariable String realmName, @Valid @RequestBody CreateUserRequest request) {
        Realm realm = realmRepository.findByName(realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Realm not found: " + realmName));

        if (appUserJpaRepository.findByRealm_NameAndUsername(realmName, request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Already existing user: " + request.username());
        }

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID().toString());
        user.setRealm(realm);
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRoles(request.roles());

        return toResponse(appUserJpaRepository.save(user));
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRoles(), user.isEnabled());
    }
}