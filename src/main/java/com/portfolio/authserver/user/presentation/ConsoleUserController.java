package com.portfolio.authserver.user.presentation;

import com.portfolio.authserver.authorization.domain.UserRoleRepository;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import com.portfolio.authserver.user.domain.AppUser;
import com.portfolio.authserver.user.domain.AppUserRepository;
import com.portfolio.authserver.user.presentation.dto.UpdateUserRequest;
import com.portfolio.authserver.user.presentation.dto.UserResponse;
import com.portfolio.authserver.user.presentation.mapper.UserMapper;
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

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final RealmRepository realmRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @GetMapping
    public String list(@PathVariable String realmName, Model model) {
        model.addAttribute("realmName", realmName);
        model.addAttribute("users", appUserRepository.findByRealmName(realmName));
        return "console/users";
    }

    @PostMapping
    public String create(@PathVariable String realmName, @RequestParam String username,
                         @RequestParam String password, @RequestParam(required = false) String roles,
                         RedirectAttributes redirectAttributes) {
        Realm realm = realmRepository.findByName(realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found"));

        if (appUserRepository.findByRealmNameAndUsername(realmName, username).isPresent()) {
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

        appUserRepository.save(user);
        redirectAttributes.addFlashAttribute(
                "successMessage", "User '" + username + "' created");
        return "redirect:/console/realms/" + realmName + "/users";
    }

    @PatchMapping("/{username}")
    public UserResponse update(@PathVariable String realmName, @PathVariable String username,
                               @RequestBody UpdateUserRequest request) {
        AppUser user = appUserRepository.findByRealmNameAndUsername(realmName, username)
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

        return userMapper.toResponse(appUserRepository.save(user));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> delete(@PathVariable String realmName, @PathVariable String username) {
        AppUser user = appUserRepository.findByRealmNameAndUsername(realmName, username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));

        if (!userRoleRepository.findByAppUser(user).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete: the user has ABAC role assigned. Remove them first.");
        }

        appUserRepository.delete(user);
        return ResponseEntity.noContent().build();
    }
}