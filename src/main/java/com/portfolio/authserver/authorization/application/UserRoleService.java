package com.portfolio.authserver.authorization.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.authserver.authorization.presentation.dto.UserRoleAttribute;
import com.portfolio.authserver.user.AppUser;
import com.portfolio.authserver.user.AppUserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final AppUserJpaRepository appUserJpaRepository;
    private final ObjectMapper objectMapper;

    public AppUser findUserOrThrow(String realmName, String username) {
        return appUserJpaRepository.findByRealm_NameAndUsername(realmName, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public String writeAttributes(List<UserRoleAttribute> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes != null ? attributes : List.of());
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize attributes", ex);
        }
    }
}
