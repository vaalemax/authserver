package com.portfolio.authserver.authorization.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.authserver.authorization.domain.*;
import com.portfolio.authserver.authorization.presentation.dto.UserRoleAttribute;
import com.portfolio.authserver.user.domain.AppUser;
import com.portfolio.authserver.user.domain.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final ObjectMapper objectMapper;

    public List<UserRole> listAssignments(String realmName, String username) {
        return userRoleRepository.findByAppUser(findUserOrThrow(realmName, username));
    }

    public UserRole getAssignment(String realmName, String username, String userRoleId) {
        return userRoleRepository.findByIdAndAppUserRealmNameAndAppUserUsername(userRoleId, realmName, username)
                .orElseThrow(() -> new NoSuchElementException("Role assignment not found"));
    }

    public UserRole createAssignment(String realmName, String username, String roleId,
                                     Instant validFrom, Instant validTo, String attributesJson) {
        AppUser appUser = findUserOrThrow(realmName, username);
        Role role = roleRepository.findByIdAndRealmName(roleId, realmName)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));

        validateWindow(validFrom, validTo);
        validateAttributes(attributesJson);

        UserRole userRole = new UserRole();
        userRole.setId(UUID.randomUUID().toString());
        userRole.setAppUser(appUser);
        userRole.setRole(role);
        userRole.setValidFrom(validFrom);
        userRole.setValidTo(validTo);
        userRole.setAttributes(attributesJson);

        return userRoleRepository.save(userRole);
    }

    public UserRole updateAssignment(String realmName, String username, String userRoleId,
                                     Instant validFrom, Instant validTo, String attributesJson) {
        UserRole userRole = getAssignment(realmName, username, userRoleId);

        Instant effectiveValidFrom = validFrom != null ? validFrom : userRole.getValidFrom();
        validateWindow(effectiveValidFrom, validTo);
        if (attributesJson != null) validateAttributes(attributesJson);

        userRole.setValidFrom(effectiveValidFrom);
        userRole.setValidTo(validTo);
        if (attributesJson != null) userRole.setAttributes(attributesJson);

        return userRoleRepository.save(userRole);
    }

    public void deleteAssignment(String realmName, String username, String userRoleId) {
        userRoleRepository.delete(getAssignment(realmName, username, userRoleId));
    }

    public String writeAttributes(List<UserRoleAttribute> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes != null ? attributes : List.of());
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize attributes", ex);
        }
    }

    public Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atZone(ZoneId.systemDefault()).toInstant() : null;
    }

    private AppUser findUserOrThrow(String realmName, String username) {
        return appUserRepository.findByRealmNameAndUsername(realmName, username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
    }

    private void validateWindow(Instant validFrom, Instant validTo) {
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("\"Valid to\" must be after \"valid from\".");
        }
    }

    private void validateAttributes(String attributesJson) {
        try {
            objectMapper.readTree(attributesJson);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid attributes JSON: " + ex.getMessage());
        }
    }
}