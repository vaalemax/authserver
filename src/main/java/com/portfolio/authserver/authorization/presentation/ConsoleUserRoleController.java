package com.portfolio.authserver.authorization.presentation;

import com.portfolio.authserver.authorization.application.UserRoleService;
import com.portfolio.authserver.authorization.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/console/realms/{realmName}/users/{username}/roles")
@RequiredArgsConstructor
public class ConsoleUserRoleController {

    private final UserRoleService userRoleService;
    private final RoleRepository roleRepository;

    @GetMapping
    public String list(@PathVariable String realmName, @PathVariable String username, Model model) {
        model.addAttribute("realmName", realmName);
        model.addAttribute("username", username);
        model.addAttribute("assignments", userRoleService.listAssignments(realmName, username));
        model.addAttribute("availableRoles", roleRepository.findByRealmName(realmName));
        return "console/user-roles";
    }

    @PostMapping
    public String create(@PathVariable String realmName, @PathVariable String username,
                         @RequestParam String roleId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validFrom,
                         @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validTo,
                         @RequestParam String attributes,
                         RedirectAttributes redirectAttributes) {
        try {
            userRoleService.createAssignment(realmName, username, roleId, userRoleService.toInstant(validFrom),
                    userRoleService.toInstant(validTo), attributes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Assigned role to '" + username + "'");
        } catch (NoSuchElementException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/users/" + username + "/roles";
    }

    @GetMapping("/{userRoleId}/edit")
    public String editForm(@PathVariable String realmName, @PathVariable String username,
                           @PathVariable String userRoleId, Model model) {
        UserRole userRole = userRoleService.getAssignment(realmName, username, userRoleId);
        model.addAttribute("realmName", realmName);
        model.addAttribute("username", username);
        model.addAttribute("userRole", userRole);
        model.addAttribute("validFromLocal",
                LocalDateTime.ofInstant(userRole.getValidFrom(),ZoneId.systemDefault()));
        model.addAttribute("validToLocal", userRole.getValidTo() != null
                ? LocalDateTime.ofInstant(userRole.getValidTo(), ZoneId.systemDefault()) : null);
        return "console/user-role-edit";
    }

    @PostMapping("/{userRoleId}/update")
    public String update(@PathVariable String realmName, @PathVariable String username,
                         @PathVariable String userRoleId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validFrom,
                         @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validTo,
                         @RequestParam String attributes,
                         RedirectAttributes redirectAttributes) {
        try {
            userRoleService.updateAssignment(realmName, username, userRoleId, userRoleService.toInstant(validFrom),
                    userRoleService.toInstant(validTo), attributes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Updated role assignment for '" + username + "'");
        } catch (NoSuchElementException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/users/" + username + "/roles";
    }

    @PostMapping("/{userRoleId}/delete")
    public String delete(@PathVariable String realmName, @PathVariable String username,
                         @PathVariable String userRoleId, RedirectAttributes redirectAttributes) {
        try {
            userRoleService.deleteAssignment(realmName, username, userRoleId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Deleted role assignment for '" + username + "'");
        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/users/" + username + "/roles";
    }
}