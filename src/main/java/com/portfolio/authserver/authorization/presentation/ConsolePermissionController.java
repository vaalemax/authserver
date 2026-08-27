package com.portfolio.authserver.authorization.presentation;

import com.portfolio.authserver.authorization.application.PermissionService;
import com.portfolio.authserver.authorization.domain.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/console/realms/{realmName}/permissions")
@RequiredArgsConstructor
public class ConsolePermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public String listPermissions(@PathVariable String realmName, Model model) {
        List<Permission> permissions = permissionService.listPermissions(realmName);
        model.addAttribute("realmName", realmName);
        model.addAttribute("permissions", permissions);
        return "console/permissions";
    }

    @PostMapping
    public String createPermission(@PathVariable String realmName,
                         @RequestParam String name, @RequestParam String subject,
                         @RequestParam(required = false) String subjectLabel,
                         @RequestParam String action, @RequestParam(required = false) String actionLabel,
                         @RequestParam(required = false) String conditionTemplate,
                         @RequestParam(required = false) String conditionLabel,
                         RedirectAttributes redirectAttributes) {
        try {
            Permission created = permissionService.createPermission(realmName, name, subject, subjectLabel,
                    action, actionLabel, conditionTemplate, conditionLabel);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Created '" + created.getName() + "' permission");
        } catch (NoSuchElementException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/permissions";
    }

    @GetMapping("/{permissionId}/edit")
    public String editPermissionForm(@PathVariable String realmName, @PathVariable String permissionId, Model model) {
        Permission permission = permissionService.getPermission(realmName, permissionId);

        model.addAttribute("realmName", realmName);
        model.addAttribute("permission", permission);
        return "console/permission-edit";
    }

    @PostMapping("/{permissionId}/update")
    public String updatePermission(@PathVariable String realmName, @PathVariable String permissionId,
                         @RequestParam String name,
                         @RequestParam(required = false) String subjectLabel,
                         @RequestParam(required = false) String actionLabel,
                         @RequestParam(required = false) String conditionTemplate,
                         @RequestParam(required = false) String conditionLabel,
                         RedirectAttributes redirectAttributes) {
        try {
            Permission updated = permissionService.updatePermission(realmName, permissionId, name, subjectLabel,
                    actionLabel, conditionTemplate, conditionLabel);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Updated '" + updated.getName() + "' permission");
        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/permissions";
    }

    @PostMapping("/{permissionId}/delete")
    public String deletePermission(@PathVariable String realmName, @PathVariable String permissionId,
                         RedirectAttributes redirectAttributes) {
        try {
            permissionService.deletePermission(realmName, permissionId);
            redirectAttributes.addFlashAttribute("successMessage", "Deleted permission");
        } catch (NoSuchElementException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/permissions";
    }
}