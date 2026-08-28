package com.portfolio.authserver.realm.presentation;

import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.application.RealmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Controller
@RequestMapping("/console/realms")
@RequiredArgsConstructor
public class ConsoleRealmController {

    private final RealmService realmService;

    @GetMapping
    public String listRealms(Model model) {
        model.addAttribute("realms", realmService.listRealms());
        return "console/realms";
    }

    @PostMapping
    public String createRealm(@RequestParam String realmName, @RequestParam(required = false) String displayName,
                         RedirectAttributes redirectAttributes) {
        try {
            realmService.createRealm(realmName, displayName);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Realm '"+realmName+"' created");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms";
    }

    @GetMapping("/{realmName}/edit")
    public String editForm(@PathVariable String realmName, Model model) {
        model.addAttribute("realm", realmService.getRealm(realmName));
        return "console/realm-edit";
    }

    @PostMapping("/{realmName}/update")
    public String updateRealm(@PathVariable String realmName, @RequestParam(required = false) String displayName,
                              RedirectAttributes redirectAttributes) {
        try {
            realmService.updateRealm(realmName, displayName, null); // enabled non toccato da questa form
            redirectAttributes.addFlashAttribute("successMessage", "Realm '" + realmName + "' updated");
        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/edit";
    }

    @PostMapping("/{realmName}/toggle-enabled")
    public String toggleEnabled(@PathVariable String realmName, RedirectAttributes redirectAttributes) {
        try {
            Realm realm = realmService.toggleEnabled(realmName);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Realm '" + realmName + "' is now " + (realm.isEnabled() ? "enabled" : "disabled"));
        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/edit";
    }
}