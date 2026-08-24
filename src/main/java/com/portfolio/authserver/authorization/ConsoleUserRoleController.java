package com.portfolio.authserver.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.authserver.user.AppUser;
import com.portfolio.authserver.user.AppUserJpaRepository;
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

    private final RoleJpaRepository roleJpaRepository;
    private final UserRoleJpaRepository userRoleJpaRepository;
    private final ObjectMapper objectMapper;
    private final UserRoleService userRoleService;

    @GetMapping
    public String list(@PathVariable String realmName, @PathVariable String username, Model model) {
        AppUser user = userRoleService.findUserOrThrow(realmName, username);
        model.addAttribute("realmName", realmName);
        model.addAttribute("username", username);
        model.addAttribute("assignments", userRoleJpaRepository.findByAppUser(user));
        model.addAttribute("availableRoles", roleJpaRepository.findByRealm_Name(realmName));
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

        Role role = roleJpaRepository.findByIdAndRealm_Name(roleId, realmName)
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

        userRoleJpaRepository.save(userRole);
        redirectAttributes.addFlashAttribute("successMessage",
                "Role '" + role.getName() + "' assigned to " + username);
        return "redirect:/console/realms/" + realmName + "/users/" + username + "/roles";
    }

    @PatchMapping("/{userRoleId}")
    public UserRoleResponse update(@PathVariable String realmName, @PathVariable String username,
                                   @PathVariable String userRoleId, @RequestBody UpdateUserRoleRequest request) {
        UserRole userRole = userRoleJpaRepository
                .findByIdAndAppUser_Realm_NameAndAppUser_Username(userRoleId, realmName, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assegnazione non trovata"));

        if (request.validTo() != null) userRole.setValidTo(request.validTo());
        if (request.attributes() != null) userRole.setAttributes(writeAttributes(request.attributes()));

        return userRoleService.toResponse(userRoleJpaRepository.save(userRole));
    }

    @DeleteMapping("/{userRoleId}")
    public ResponseEntity<Void> delete(@PathVariable String realmName, @PathVariable String username,
                                       @PathVariable String userRoleId) {
        UserRole userRole = userRoleJpaRepository
                .findByIdAndAppUser_Realm_NameAndAppUser_Username(userRoleId, realmName, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assegnazione non trovata"));

        userRoleJpaRepository.delete(userRole);
        return ResponseEntity.noContent().build();
    }
}