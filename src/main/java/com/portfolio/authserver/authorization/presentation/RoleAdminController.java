package com.portfolio.authserver.authorization.presentation;

import com.portfolio.authserver.authorization.application.RoleService;
import com.portfolio.authserver.authorization.domain.*;
import com.portfolio.authserver.authorization.presentation.dto.CreateRoleRequest;
import com.portfolio.authserver.authorization.presentation.dto.RoleResponse;
import com.portfolio.authserver.authorization.presentation.dto.UpdateRoleRequest;
import com.portfolio.authserver.authorization.presentation.mapper.AuthorizationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/admin/realms/{realmName}/roles")
@RequiredArgsConstructor
public class RoleAdminController {

    private final AuthorizationMapper authorizationMapper;
    private final RoleRepository roleRepository;
    private final RoleService roleService;

    @GetMapping
    public List<RoleResponse> findRoles(@PathVariable String realmName) {
        return roleRepository.findByRealmName(realmName).stream().map(authorizationMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse createRole(@PathVariable String realmName, @Valid @RequestBody CreateRoleRequest request) {
        try {
            return authorizationMapper.toResponse(
                    roleService.createRole(realmName, request.name(), request.level(), request.permissionIds()));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @PatchMapping("/{roleId}")
    public RoleResponse update(@PathVariable String realmName, @PathVariable String roleId,
                               @RequestBody UpdateRoleRequest request) {
        try {
            return authorizationMapper.toResponse(
                    roleService.updateRole(realmName, roleId, request.level(), request.permissionIds()));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> delete(@PathVariable String realmName, @PathVariable String roleId) {
        try {
            roleService.deleteRole(realmName, roleId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }
}