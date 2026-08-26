package com.portfolio.authserver.authorization.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.authserver.authorization.application.UserRoleService;
import com.portfolio.authserver.authorization.domain.*;
import com.portfolio.authserver.user.domain.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

    private final UserRoleRepository userRoleRepository;
    private final UserRoleService userRoleService;
    private final RoleRepository roleRepository;
    private final ObjectMapper objectMapper;

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

    @GetMapping("/{userRoleId}/edit")
    public String editForm(@PathVariable String realmName, @PathVariable String username,
                           @PathVariable String userRoleId, Model model) {
        UserRole userRole = userRoleRepository.findByIdAndAppUserRealmNameAndAppUserUsername(userRoleId,
                        realmName, username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        model.addAttribute("realmName", realmName);
        model.addAttribute("username", username);
        model.addAttribute("userRole", userRole);
        model.addAttribute("validFromLocal", LocalDateTime.ofInstant(
                userRole.getValidFrom(), ZoneId.systemDefault()));
        model.addAttribute("validToLocal", userRole.getValidTo() != null
                ? LocalDateTime.ofInstant(userRole.getValidTo(), ZoneId.systemDefault()) : null);
        return "console/user-role-edit";
    }

    @PostMapping("/{userRoleId}/update")
    public String update(@PathVariable String realmName, @PathVariable String username,
                         @PathVariable String userRoleId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validFrom,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                             LocalDateTime validTo,
                         @RequestParam String attributes,
                         RedirectAttributes redirectAttributes) {
        UserRole userRole = userRoleRepository.findByIdAndAppUserRealmNameAndAppUserUsername(userRoleId,
                realmName, username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        userRole.setValidFrom(validFrom.atZone(ZoneId.systemDefault()).toInstant());
        userRole.setValidTo(validTo != null ? validTo.atZone(ZoneId.systemDefault()).toInstant() : null);
        userRole.setAttributes(attributes);
        userRoleRepository.save(userRole);

        redirectAttributes.addFlashAttribute("successMessage",
                "Updated role assignment for '" + username + "'");
        return "redirect:/console/realms/"+realmName+"/users/"+username+"/roles";
    }

    @PostMapping("/{userRoleId}/delete")
    public String delete(@PathVariable String realmName, @PathVariable String userRoleId,
                         @PathVariable String username, RedirectAttributes redirectAttributes) {
        UserRole userRole = userRoleRepository.findByIdAndAppUserRealmNameAndAppUserUsername(userRoleId,
                realmName, username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        userRoleRepository.delete(userRole);
        redirectAttributes.addFlashAttribute("successMessage",
                "Deleted role assignment for user '"+username+"'");
        return "redirect:/console/realms/"+realmName+"/users/"+username+"/roles";
    }
}