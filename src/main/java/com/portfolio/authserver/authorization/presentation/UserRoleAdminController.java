package com.portfolio.authserver.authorization.presentation;


import com.portfolio.authserver.authorization.application.UserRoleService;
import com.portfolio.authserver.authorization.domain.*;
import com.portfolio.authserver.authorization.presentation.dto.CreateUserRoleRequest;
import com.portfolio.authserver.authorization.presentation.dto.UpdateUserRoleRequest;
import com.portfolio.authserver.authorization.presentation.dto.UserRoleResponse;
import com.portfolio.authserver.authorization.presentation.mapper.AuthorizationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/admin/realms/{realmName}/users/{username}/roles")
@RequiredArgsConstructor
public class UserRoleAdminController {

    private final AuthorizationMapper authorizationMapper;
    private final UserRoleService userRoleService;

    @GetMapping
    public List<UserRoleResponse> list(@PathVariable String realmName, @PathVariable String username) {
        try {
            return userRoleService.listAssignments(realmName, username).stream()
                    .map(authorizationMapper::toResponse).toList();
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserRoleResponse create(@PathVariable String realmName, @PathVariable String username,
                                   @Valid @RequestBody CreateUserRoleRequest request) {
        try {
            String attributesJson = userRoleService.writeAttributes(request.attributes());
            Instant validFrom = request.validFrom() != null ? request.validFrom() : Instant.now();
            return authorizationMapper.toResponse(userRoleService.createAssignment(
                    realmName, username, request.roleId(), validFrom, request.validTo(), attributesJson));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PatchMapping("/{userRoleId}")
    public UserRoleResponse update(@PathVariable String realmName, @PathVariable String username,
                                   @PathVariable String userRoleId, @RequestBody UpdateUserRoleRequest request) {
        try {
            String attributesJson = request.attributes() != null
                    ? userRoleService.writeAttributes(request.attributes()) : null;
            return authorizationMapper.toResponse(userRoleService.updateAssignment(
                    realmName, username, userRoleId, request.validFrom(), request.validTo(), attributesJson));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/{userRoleId}")
    public ResponseEntity<Void> delete(@PathVariable String realmName, @PathVariable String username,
                                       @PathVariable String userRoleId) {
        try {
            userRoleService.deleteAssignment(realmName, username, userRoleId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}
