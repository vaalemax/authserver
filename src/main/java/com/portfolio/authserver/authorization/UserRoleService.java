package com.portfolio.authserver.authorization;

import com.portfolio.authserver.user.AppUser;
import com.portfolio.authserver.user.AppUserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final AppUserJpaRepository appUserJpaRepository;

    public AppUser findUserOrThrow(String realmName, String username) {
        return appUserJpaRepository.findByRealm_NameAndUsername(realmName, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserRoleResponse toResponse(UserRole userRole) {
        return new UserRoleResponse(userRole.getId(), userRole.getRole().getName(),
                userRole.getValidFrom(), userRole.getValidTo(), userRole.getAttributes());
    }
}
