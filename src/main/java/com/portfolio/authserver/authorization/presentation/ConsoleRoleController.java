package com.portfolio.authserver.authorization.presentation;

import com.portfolio.authserver.authorization.domain.*;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/console/realms/{realmName}/roles")
@RequiredArgsConstructor
public class ConsoleRoleController {

    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RealmRepository realmRepository;
    private final RoleRepository roleRepository;

    @GetMapping
    public String list(@PathVariable String realmName, Model model) {
        model.addAttribute("realmName", realmName);
        model.addAttribute("roles", roleRepository.findByRealmName(realmName));
        model.addAttribute("allPermissions", permissionRepository.findByRealmName(realmName));
        return "console/roles";
    }

    @PostMapping
    public String create(@PathVariable String realmName, @RequestParam String name,
                         @RequestParam(required = false) Integer level,
                         @RequestParam(required = false) List<String> permissionIds,
                         RedirectAttributes redirectAttributes) {
        Realm realm = realmRepository.findByName(realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found"));

        if (roleRepository.findByRealmNameAndName(realmName, name).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Already existing role: " + name);
            return "redirect:/console/realms/" + realmName + "/roles";
        }

        Set<Permission> permissions = new HashSet<>();
        if (permissionIds != null) {
            for (String id : permissionIds) {
                permissionRepository.findByIdAndRealmName(id, realmName)
                        .ifPresent(permissions::add);
            }
        }

        Role role = new Role();
        role.setId(UUID.randomUUID().toString());
        role.setRealm(realm);
        role.setName(name);
        role.setLevel(level);
        role.setPermissions(permissions);

        roleRepository.save(role);
        redirectAttributes.addFlashAttribute("successMessage",
                "Created '" + name + "' role");
        return "redirect:/console/realms/" + realmName + "/roles";
    }

    @GetMapping("/{roleId}/edit")
    public String editForm(@PathVariable String realmName, @PathVariable String roleId, Model model) {
        Role role = roleRepository.findByIdAndRealmName(roleId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        Set<String> assignedPermissionIds = role.getPermissions().stream()
                .map(Permission::getId).collect(Collectors.toSet());

        model.addAttribute("realmName", realmName);
        model.addAttribute("role", role);
        model.addAttribute("allPermissions", permissionRepository.findByRealmName(realmName));
        model.addAttribute("assignedPermissionIds", assignedPermissionIds);
        return "console/role-edit";
    }

    @PostMapping("/{roleId}/update")
    public String update(@PathVariable String realmName, @PathVariable String roleId,
                         @RequestParam(required = false) Integer level,
                         @RequestParam(required = false) List<String> permissionIds,
                         RedirectAttributes redirectAttributes) {
        Role role = roleRepository.findByIdAndRealmName(roleId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        role.setLevel(level);

        Set<Permission> permissions = new HashSet<>();
        if (permissionIds != null) {
            for (String id : permissionIds) {
                permissionRepository.findByIdAndRealmName(id, realmName).ifPresent(permissions::add);
            }
        }
        role.setPermissions(permissions);

        roleRepository.save(role);
        redirectAttributes.addFlashAttribute("successMessage",
                "Updated '" + role.getName() + "' role");
        return "redirect:/console/realms/" + realmName + "/roles";
    }

    @PostMapping("/{roleId}/delete")
    public String delete(@PathVariable String realmName, @PathVariable String roleId,
                         RedirectAttributes redirectAttributes) {
        Role role = roleRepository.findByIdAndRealmName(roleId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        if (!userRoleRepository.findByRole(role).isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete: role is assigned to at least one user. Remove assignments first.");
            return "redirect:/console/realms/" + realmName + "/roles";
        }

        roleRepository.delete(role);
        redirectAttributes.addFlashAttribute("successMessage", "Deleted role");
        return "redirect:/console/realms/" + realmName + "/roles";
    }
}