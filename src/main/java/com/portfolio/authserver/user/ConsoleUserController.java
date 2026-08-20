package com.portfolio.authserver.user;

import com.portfolio.authserver.realm.Realm;
import com.portfolio.authserver.realm.RealmJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/console/realms/{realmName}/users")
@RequiredArgsConstructor
public class ConsoleUserController {

    private final RealmJpaRepository realmJpaRepository;
    private final AppUserJpaRepository appUserJpaRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String list(@PathVariable String realmName, Model model) {
        model.addAttribute("realmName", realmName);
        model.addAttribute("users", appUserJpaRepository.findByRealm_Name(realmName));
        return "console/users";
    }

    @PostMapping
    public String create(@PathVariable String realmName, @RequestParam String username,
                         @RequestParam String password, @RequestParam(required = false) String roles,
                         RedirectAttributes redirectAttributes) {
        Realm realm = realmJpaRepository.findByName(realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found"));

        if (appUserJpaRepository.findByRealm_NameAndUsername(realmName, username).isPresent()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Already existing user: " + username);
            return "redirect:/console/realms/" + realmName + "/users";
        }

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID().toString());
        user.setRealm(realm);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setRoles(roles != null && !roles.isBlank()
                ? Arrays.stream(roles.split(",")).map(String::trim).collect(Collectors.toSet())
                : Set.of());

        appUserJpaRepository.save(user);
        redirectAttributes.addFlashAttribute(
                "successMessage", "User '" + username + "' created");
        return "redirect:/console/realms/" + realmName + "/users";
    }
}