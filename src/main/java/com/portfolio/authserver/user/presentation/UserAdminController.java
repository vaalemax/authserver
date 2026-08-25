package com.portfolio.authserver.user.presentation;

import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import com.portfolio.authserver.user.domain.AppUser;
import com.portfolio.authserver.user.domain.AppUserRepository;
import com.portfolio.authserver.user.presentation.dto.CreateUserRequest;
import com.portfolio.authserver.user.presentation.dto.UserResponse;
import com.portfolio.authserver.user.presentation.mapper.UserMapper;
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

    private final AppUserRepository appUserRepository;
    private final RealmRepository realmRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @GetMapping
    public List<UserResponse> findUsers(@PathVariable String realmName) {
        return appUserRepository.findByRealmName(realmName).stream().map(userMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@PathVariable String realmName, @Valid @RequestBody CreateUserRequest request) {
        Realm realm = realmRepository.findByName(realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Realm not found: " + realmName));

        if (appUserRepository.findByRealmNameAndUsername(realmName, request.username()).isPresent()) {
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

        return userMapper.toResponse(appUserRepository.save(user));
    }
}