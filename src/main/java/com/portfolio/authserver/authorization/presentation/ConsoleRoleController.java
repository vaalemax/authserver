package com.portfolio.authserver.authorization.presentation;

import com.portfolio.authserver.authorization.application.RoleService;
import com.portfolio.authserver.authorization.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/console/realms/{realmName}/roles")
@RequiredArgsConstructor
public class ConsoleRoleController {

    private final PermissionRepository permissionRepository;
    private final RoleService roleService;

    @GetMapping
    public String listRoles(@PathVariable String realmName, Model model) {
        model.addAttribute("realmName", realmName);
        model.addAttribute("roles", roleService.listRoles(realmName));
        model.addAttribute("allPermissions", permissionRepository.findByRealmName(realmName));
        return "console/roles";
    }

    @PostMapping
    public String createRole(@PathVariable String realmName, @RequestParam String name,
                         @RequestParam(required = false) Integer level,
                         @RequestParam(required = false) List<String> permissionIds,
                         RedirectAttributes redirectAttributes) {
        try {
            Set<String> ids = permissionIds != null ? new HashSet<>(permissionIds) : Set.of();
            Role created = roleService.createRole(realmName, name, level, ids);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Created '" + created.getName() + "' role");
        } catch (NoSuchElementException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/roles";
    }

    @GetMapping("/{roleId}/edit")
    public String editRoleForm(@PathVariable String realmName, @PathVariable String roleId, Model model) {
        Role role = roleService.getRole(realmName, roleId);
        Set<String> assignedPermissionIds = role.getPermissions().stream()
                .map(Permission::getId).collect(Collectors.toSet());

        model.addAttribute("realmName", realmName);
        model.addAttribute("role", role);
        model.addAttribute("allPermissions", permissionRepository.findByRealmName(realmName));
        model.addAttribute("assignedPermissionIds", assignedPermissionIds);
        return "console/role-edit";
    }

    @PostMapping("/{roleId}/update")
    public String updateRole(@PathVariable String realmName, @PathVariable String roleId,
                         @RequestParam(required = false) Integer level,
                         @RequestParam(required = false) List<String> permissionIds,
                         RedirectAttributes redirectAttributes) {
        try {
            Set<String> ids = permissionIds != null ? new HashSet<>(permissionIds) : Set.of();
            Role updated = roleService.updateRole(realmName, roleId, level, ids);
            redirectAttributes.addFlashAttribute("successMessage", "Updated '" + updated.getName() + "' role");
        } catch (NoSuchElementException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/roles";
    }

    @PostMapping("/{roleId}/delete")
    public String deleteRole(@PathVariable String realmName, @PathVariable String roleId,
                         RedirectAttributes redirectAttributes) {
        try {
            roleService.deleteRole(realmName, roleId);
            redirectAttributes.addFlashAttribute("successMessage", "Deleted role");
        } catch (NoSuchElementException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/roles";
    }
}