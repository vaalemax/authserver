package com.portfolio.authserver.authorization.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.authserver.authorization.application.UserRoleService;
import com.portfolio.authserver.authorization.domain.Role;
import com.portfolio.authserver.authorization.domain.RoleRepository;
import com.portfolio.authserver.authorization.domain.UserRole;
import com.portfolio.authserver.authorization.domain.UserRoleRepository;
import com.portfolio.authserver.authorization.presentation.dto.UpdateUserRoleRequest;
import com.portfolio.authserver.authorization.presentation.dto.UserRoleResponse;
import com.portfolio.authserver.authorization.presentation.mapper.AuthorizationMapper;
import com.portfolio.authserver.user.domain.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Controller
@RequestMapping("/console/realms/{realmName}/users/{username}/roles")
@RequiredArgsConstructor
public class ConsoleUserRoleController {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ObjectMapper objectMapper;
    private final UserRoleService userRoleService;
    private final AuthorizationMapper authorizationMapper;

    @GetMapping
    public String list(@PathVariable String realmName, @PathVariable String username, Model model) {
        AppUser user = userRoleService.findUserOrThrow(realmName, username);
        model.addAttribute("realmName", realmName);
        model.addAttribute("username", username);
        model.addAttribute("assignments", userRoleRepository.findByAppUser(user));
        model.addAttribute("availableRoles", roleRepository.findByRealmName(realmName));
        return "console/user-roles";
    }

    @PostMapping
    public String create(@PathVariable String realmName, @PathVariable String username,
                         @RequestParam String roleId,
                         @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validFrom,
                         @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validTo,
                         @RequestParam(required = false) String attributesJson,
                         RedirectAttributes redirectAttributes) {
        AppUser user = userRoleService.findUserOrThrow(realmName, username);

        Role role = roleRepository.findByIdAndRealmName(roleId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));

        String normalizedAttributes = "[]";
        if (attributesJson != null && !attributesJson.isBlank()) {
            try {
                objectMapper.readTree(attributesJson);
                normalizedAttributes = attributesJson;
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Invalid JSON attributes: " + ex.getMessage());
                return "redirect:/console/realms/" + realmName + "/users/" + username + "/roles";
            }
        }

        UserRole userRole = new UserRole();
        userRole.setId(UUID.randomUUID().toString());
        userRole.setAppUser(user);
        userRole.setRole(role);
        userRole.setValidFrom(validFrom != null ? validFrom.atZone(
                ZoneId.systemDefault()).toInstant() : Instant.now());
        userRole.setValidTo(validTo != null ? validTo.atZone(ZoneId.systemDefault()).toInstant() : null);
        userRole.setAttributes(normalizedAttributes);

        userRoleRepository.save(userRole);
        redirectAttributes.addFlashAttribute("successMessage",
                "Role '" + role.getName() + "' assigned to " + username);
        return "redirect:/console/realms/" + realmName + "/users/" + username + "/roles";
    }

    @PatchMapping("/{userRoleId}")
    public UserRoleResponse update(@PathVariable String realmName, @PathVariable String username,
                                   @PathVariable String userRoleId, @RequestBody UpdateUserRoleRequest request) {
        UserRole userRole = userRoleRepository
                .findByIdAndAppUserRealmNameAndAppUserUsername(userRoleId, realmName, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (request.validTo() != null) userRole.setValidTo(request.validTo());
        if (request.attributes() != null) userRole.setAttributes(userRoleService.writeAttributes(request.attributes()));

        return authorizationMapper.toResponse(userRoleRepository.save(userRole));
    }

    @DeleteMapping("/{userRoleId}")
    public ResponseEntity<Void> delete(@PathVariable String realmName, @PathVariable String username,
                                       @PathVariable String userRoleId) {
        UserRole userRole = userRoleRepository
                .findByIdAndAppUserRealmNameAndAppUserUsername(userRoleId, realmName, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        userRoleRepository.delete(userRole);
        return ResponseEntity.noContent().build();
    }
}