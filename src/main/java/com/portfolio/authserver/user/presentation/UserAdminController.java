package com.portfolio.authserver.user.presentation;

import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import com.portfolio.authserver.user.application.UserService;
import com.portfolio.authserver.user.domain.AppUser;
import com.portfolio.authserver.user.domain.AppUserRepository;
import com.portfolio.authserver.user.presentation.dto.CreateUserRequest;
import com.portfolio.authserver.user.presentation.dto.UpdateUserRequest;
import com.portfolio.authserver.user.presentation.dto.UserResponse;
import com.portfolio.authserver.user.presentation.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/realms/{realmName}/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final AppUserRepository appUserRepository;
    private final RealmRepository realmRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserService userService;

    @GetMapping
    public List<UserResponse> findUsers(@PathVariable String realmName) {
        return userService.listUsers(realmName).stream().map(userMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@PathVariable String realmName, @Valid @RequestBody CreateUserRequest request) {
        try{
            return userMapper.toResponse(userService.createUser(realmName, request.username(),
                    request.password(), request.roles()));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }//todo

    @PatchMapping("/{username}/update")
    public String updateUser(@PathVariable String realmName, @Valid @RequestBody UpdateUserRequest request){
        try{
            return userMapper.toResponse(userService.updateUser(realmName, request.username(),
                    request.password(), request.isEnabled()));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }
}