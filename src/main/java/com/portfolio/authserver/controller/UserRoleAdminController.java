package com.portfolio.authserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.authserver.model.AppUser;
import com.portfolio.authserver.model.Role;
import com.portfolio.authserver.model.UserRole;
import com.portfolio.authserver.model.dto.CreateUserRoleRequest;
import com.portfolio.authserver.model.dto.UserRoleAttribute;
import com.portfolio.authserver.model.dto.UserRoleResponse;
import com.portfolio.authserver.repository.AppUserJpaRepository;
import com.portfolio.authserver.repository.RoleJpaRepository;
import com.portfolio.authserver.repository.UserRoleJpaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/realms/{realmName}/users/{username}/roles")
@RequiredArgsConstructor
public class UserRoleAdminController {

    private final AppUserJpaRepository appUserJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final UserRoleJpaRepository userRoleJpaRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    public List<UserRoleResponse> findUserRoles(@PathVariable String realmName, @PathVariable String username) {
        return userRoleJpaRepository.findByAppUser(findUserOrThrow(realmName, username))
                .stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserRoleResponse createUserRoles(@PathVariable String realmName, @PathVariable String username,
                                   @Valid @RequestBody CreateUserRoleRequest request) {
        AppUser user = findUserOrThrow(realmName, username);

        Role role = roleJpaRepository.findById(request.roleId())
                .filter(r -> r.getRealm().getName().equals(realmName))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Role non trovato nel realm: " + request.roleId()));

        UserRole userRole = new UserRole();
        userRole.setId(UUID.randomUUID().toString());
        userRole.setAppUser(user);
        userRole.setRole(role);
        userRole.setValidFrom(request.validFrom() != null ? request.validFrom() : Instant.now());
        userRole.setValidTo(request.validTo());
        userRole.setAttributes(writeAttributes(request.attributes()));

        return toResponse(userRoleJpaRepository.save(userRole));
    }

    private AppUser findUserOrThrow(String realmName, String username) {
        return appUserJpaRepository.findByRealm_NameAndUsername(realmName, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Utente non trovato: " + username));
    }

    private String writeAttributes(List<UserRoleAttribute> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes != null ? attributes : List.of());
        } catch (Exception ex) {
            throw new IllegalStateException("Impossibile serializzare gli attributi", ex);
        }
    }

    private List<UserRoleAttribute> readAttributes(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, UserRoleAttribute.class));
        } catch (Exception ex) {
            return List.of();
        }
    }

    private UserRoleResponse toResponse(UserRole ur) {
        return new UserRoleResponse(ur.getId(), ur.getRole().getName(), ur.getValidFrom(), ur.getValidTo(), readAttributes(ur.getAttributes()));
    }
}