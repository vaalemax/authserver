package com.portfolio.authserver.realm.presentation;

import com.portfolio.authserver.realm.domain.RealmRepository;
import com.portfolio.authserver.realm.application.RealmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Controller
@RequestMapping("/console/realms")
@RequiredArgsConstructor
public class ConsoleRealmController {

    private final RealmRepository realmRepository;
    private final RealmService realmService;

    @GetMapping
    public String listRealms(Model model) {
        model.addAttribute("realms", realmRepository.findAll());
        return "console/realms";
    }

    @PostMapping
    public String createRealm(@RequestParam String realmName, @RequestParam(required = false) String displayName,
                         RedirectAttributes redirectAttributes) {
        try {
            realmService.createRealm(realmName, displayName);
            redirectAttributes.addFlashAttribute("successMessage", "Realm '"+realmName+"' created");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms";
    }

    @PostMapping("/{realmName}/update")
    public String updateRealm(@RequestParam String realmName, @RequestParam(required = false) String displayName,
                              RedirectAttributes redirectAttributes) {
        try{
            realmService.updateRealm(realmName, displayName);
            redirectAttributes.addFlashAttribute("successMessage", "Realm '"+realmName+"' updated");
        }catch(NoSuchElementException ex){
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms";
    }


    @PostMapping("/{realmName}/delete")
}