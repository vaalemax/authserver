package com.portfolio.authserver.authorization.presentation;

import com.portfolio.authserver.authorization.application.PermissionService;
import com.portfolio.authserver.authorization.domain.Permission;
import com.portfolio.authserver.authorization.domain.PermissionRepository;
import com.portfolio.authserver.authorization.domain.Role;
import com.portfolio.authserver.authorization.domain.RoleRepository;
import com.portfolio.authserver.authorization.presentation.dto.PermissionResponse;
import com.portfolio.authserver.authorization.presentation.dto.UpdatePermissionRequest;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private final RealmJpaRepository realmJpaRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

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
        Realm realm = realmJpaRepository.findByName(realmName)
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

    @PatchMapping("/{permissionId}")
    public PermissionResponse update(@PathVariable String realmName, @PathVariable String permissionId,
                                     @RequestBody UpdatePermissionRequest request) {
        Permission permission = permissionRepository.findByIdAndRealmName(permissionId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found"));

        if (request.name() != null) permission.setName(request.name());
        if (request.subjectLabel() != null) permission.setSubjectLabel(request.subjectLabel());
        if (request.actionLabel() != null) permission.setActionLabel(request.actionLabel());
        if (request.conditionTemplate() != null) permission.setConditionTemplate(request.conditionTemplate());
        if (request.conditionLabel() != null) permission.setConditionLabel(request.conditionLabel());

        return permissionService.toResponse(permissionRepository.save(permission));
    }

    @DeleteMapping("/{permissionId}")
    public ResponseEntity<Void> delete(@PathVariable String realmName, @PathVariable String permissionId) {
        Permission permission = permissionRepository.findByIdAndRealmName(permissionId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found"));

        List<Role> linkedRoles = roleRepository.findByPermissionsId(permissionId);
        if (!linkedRoles.isEmpty()) {
            String names = linkedRoles.stream().map(Role::getName).collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete: used in roles [" + names + "]. Remove usages first.");
        }

        permissionRepository.delete(permission);
        return ResponseEntity.noContent().build();
    }
}