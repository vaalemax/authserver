package com.portfolio.authserver.user.presentation;

import com.portfolio.authserver.user.application.UserService;
import com.portfolio.authserver.user.presentation.dto.CreateUserRequest;
import com.portfolio.authserver.user.presentation.dto.UpdateUserRequest;
import com.portfolio.authserver.user.presentation.dto.UserResponse;
import com.portfolio.authserver.user.presentation.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/admin/realms/{realmName}/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;
    private final UserMapper userMapper;

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
    }

    @PatchMapping("/{username}")
    public UserResponse updateUser(@PathVariable String realmName, @PathVariable String username,
                                   @RequestBody UpdateUserRequest request){
        try{
            return userMapper.toResponse(userService.updateUser(realmName, username,
                    request.password(), request.enabled()));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> disableUser(@PathVariable String realmName, @PathVariable String username){
        try {
            userService.disableUser(realmName, username);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }
}