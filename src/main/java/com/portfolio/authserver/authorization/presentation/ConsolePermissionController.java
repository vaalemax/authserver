package com.portfolio.authserver.authorization.presentation;

import com.portfolio.authserver.authorization.application.PermissionService;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/console/realms/{realmName}/permissions")
@RequiredArgsConstructor
public class ConsolePermissionController {

    private final PermissionRepository permissionRepository;
    private final RealmRepository realmRepository;
    private final RoleRepository roleRepository;

    @GetMapping
    public String list(@PathVariable String realmName, Model model) {
        model.addAttribute("realmName", realmName);
        model.addAttribute("permissions", permissionRepository.findByRealmName(realmName));
        return "console/permissions";
    }

    @PostMapping
    public String create(@PathVariable String realmName,
                         @RequestParam String name, @RequestParam String subject,
                         @RequestParam(required = false) String subjectLabel,
                         @RequestParam String action, @RequestParam(required = false) String actionLabel,
                         @RequestParam(required = false) String conditionTemplate,
                         @RequestParam(required = false) String conditionLabel,
                         RedirectAttributes redirectAttributes) {
        Realm realm = realmRepository.findByName(realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found"));

        if (permissionRepository.findByRealmNameAndSubjectAndAction(realmName, subject, action).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Already existing permission " + subject + "/" + action);
            return "redirect:/console/realms/" + realmName + "/permissions";
        }

        Permission permission = new Permission();
        permission.setId(UUID.randomUUID().toString());
        permission.setRealm(realm);
        permission.setName(name);
        permission.setSubject(subject);
        permission.setSubjectLabel(subjectLabel);
        permission.setAction(action);
        permission.setActionLabel(actionLabel);
        permission.setConditionTemplate(conditionTemplate);
        permission.setConditionLabel(conditionLabel);

        permissionRepository.save(permission);
        redirectAttributes.addFlashAttribute("successMessage",
                "Created '" + name + "' permission");
        return "redirect:/console/realms/" + realmName + "/permissions";
    }

    @GetMapping("/{permissionId}/edit")
    public String editForm(@PathVariable String realmName, @PathVariable String permissionId, Model model) {
        Permission permission = permissionRepository.findByIdAndRealmName(permissionId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found"));
        model.addAttribute("realmName", realmName);
        model.addAttribute("permission", permission);
        return "console/permission-edit";
    }

    @PostMapping("/{permissionId}/update")
    public String update(@PathVariable String realmName, @PathVariable String permissionId,
                         @RequestParam String name,
                         @RequestParam(required = false) String subjectLabel,
                         @RequestParam(required = false) String actionLabel,
                         @RequestParam(required = false) String conditionTemplate,
                         @RequestParam(required = false) String conditionLabel,
                         RedirectAttributes redirectAttributes) {
        Permission permission = permissionRepository.findByIdAndRealmName(permissionId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found"));

        permission.setName(name);
        permission.setSubjectLabel(subjectLabel);
        permission.setActionLabel(actionLabel);
        permission.setConditionTemplate(conditionTemplate);
        permission.setConditionLabel(conditionLabel);
        permissionRepository.save(permission);

        redirectAttributes.addFlashAttribute("successMessage",
                "Updated '" + name + "' permission");
        return "redirect:/console/realms/" + realmName + "/permissions";
    }

    @PostMapping("/{permissionId}/delete")
    public String delete(@PathVariable String realmName, @PathVariable String permissionId,
                         RedirectAttributes redirectAttributes) {
        Permission permission = permissionRepository.findByIdAndRealmName(permissionId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found"));

        List<Role> linkedRoles = roleRepository.findByPermissionsId(permissionId);
        if (!linkedRoles.isEmpty()) {
            String names = linkedRoles.stream().map(Role::getName).collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete: used in roles [" + names + "]. Remove usages first.");
            return "redirect:/console/realms/" + realmName + "/permissions";
        }

        permissionRepository.delete(permission);
        redirectAttributes.addFlashAttribute("successMessage", "Deleted permission");
        return "redirect:/console/realms/" + realmName + "/permissions";
    }
}