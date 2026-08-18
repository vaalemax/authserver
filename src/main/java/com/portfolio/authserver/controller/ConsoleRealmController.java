package com.portfolio.authserver.controller;

import com.portfolio.authserver.repository.RealmJpaRepository;
import com.portfolio.authserver.service.RealmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/console/realms")
@RequiredArgsConstructor
public class ConsoleRealmController {

    private final RealmJpaRepository realmJpaRepository;
    private final RealmService realmService;
    @GetMapping
    public String list(Model model) {
        model.addAttribute("realms", realmJpaRepository.findAll());
        return "console/realms";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam(required = false) String displayName,
                         RedirectAttributes redirectAttributes) {
        try {
            realmService.createRealm(name, displayName);
            redirectAttributes.addFlashAttribute("successMessage", "Realm '" + name + "' creato");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms";
    }
}