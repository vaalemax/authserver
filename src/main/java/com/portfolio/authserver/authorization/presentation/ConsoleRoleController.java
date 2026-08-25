package com.portfolio.authserver.authorization.presentation;

import com.portfolio.authserver.authorization.domain.Permission;
import com.portfolio.authserver.authorization.domain.PermissionRepository;
import com.portfolio.authserver.authorization.domain.Role;
import com.portfolio.authserver.authorization.domain.RoleRepository;
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

@Controller
@RequestMapping("/console/realms/{realmName}/roles")
@RequiredArgsConstructor
public class ConsoleRoleController {

    private final RealmRepository realmRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

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
}