package com.portfolio.authserver.user;

import com.portfolio.authserver.authorization.domain.UserRoleRepository;
import com.portfolio.authserver.realm.Realm;
import com.portfolio.authserver.realm.RealmJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final UserMapper userMapper;
    private final UserRoleRepository userRoleRepository;

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

    @PatchMapping("/{username}")
    public UserResponse update(@PathVariable String realmName, @PathVariable String username,
                               @RequestBody UpdateUserRequest request) {
        AppUser user = appUserJpaRepository.findByRealm_NameAndUsername(realmName, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.roles() != null) {
            user.setRoles(request.roles());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        return userMapper.toResponse(appUserJpaRepository.save(user));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> delete(@PathVariable String realmName, @PathVariable String username) {
        AppUser user = appUserJpaRepository.findByRealm_NameAndUsername(realmName, username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));

        if (!userRoleRepository.findByAppUser(user).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete: the user has ABAC role assigned. Remove them first.");
        }

        appUserJpaRepository.delete(user);
        return ResponseEntity.noContent().build();
    }
}