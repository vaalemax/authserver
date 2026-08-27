package com.portfolio.authserver.user.presentation;

import com.portfolio.authserver.user.application.UserService;
import com.portfolio.authserver.user.domain.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/console/realms/{realmName}/users")
@RequiredArgsConstructor
public class ConsoleUserController {

    private final UserService userService;

    @GetMapping
    public String listUsers(@PathVariable String realmName, Model model) {
        model.addAttribute("realmName", realmName);
        model.addAttribute("users", userService.listUsers(realmName));
        return "console/users";
    }

    @PostMapping
    public String createUser(@PathVariable String realmName, @RequestParam String username,
                         @RequestParam String password, @RequestParam(required = false) String roles,
                         RedirectAttributes redirectAttributes) {
        try{
            AppUser user = userService.createUser(realmName, username, password, roles);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "User '"+user.getUsername()+"' created");
        }catch(NoSuchElementException | IllegalArgumentException ex){
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/users";
    }

    @GetMapping("/{username}/edit")
    public String editUserForm(@PathVariable String realmName, @PathVariable String username, Model model) {
        model.addAttribute("realmName", realmName);
        model.addAttribute("appUser", userService.getUser(realmName, username));
        return "console/user-edit";
    }

    @PostMapping("/{username}/update")
    public String updateUser(@PathVariable String realmName, @PathVariable String username,
                         @RequestParam(required = false) String password,
                         @RequestParam(required = false) Boolean enabled,
                         RedirectAttributes redirectAttributes) {
        try{
            AppUser user = userService.updateUser(realmName, username, password, enabled);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Updated '"+user.getUsername()+"'");
        }catch(NoSuchElementException ex){
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/users";
    }

    @PostMapping("/{username}/delete")
    public String deleteUser(@PathVariable String realmName, @PathVariable String username,
                         RedirectAttributes redirectAttributes) {
        try{
            userService.deleteUser(realmName, username);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Disabled user '"+username+"'");
        }catch(NoSuchElementException ex){
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/users";
    }
}