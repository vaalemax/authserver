package com.portfolio.authserver.authorization;

import com.portfolio.authserver.realm.Realm;
import com.portfolio.authserver.realm.RealmJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/console/realms/{realmName}/permissions")
@RequiredArgsConstructor
public class ConsolePermissionController {

    private final RealmJpaRepository realmJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;

    @GetMapping
    public String list(@PathVariable String realmName, Model model) {
        model.addAttribute("realmName", realmName);
        model.addAttribute("permissions", permissionJpaRepository.findByRealm_Name(realmName));
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

        if (permissionJpaRepository.findByRealm_NameAndSubjectAndAction(realmName, subject, action).isPresent()) {
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

        permissionJpaRepository.save(permission);
        redirectAttributes.addFlashAttribute("successMessage",
                "Created '" + name + "' permission");
        return "redirect:/console/realms/" + realmName + "/permissions";
    }
}