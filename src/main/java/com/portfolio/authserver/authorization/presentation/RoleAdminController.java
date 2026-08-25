package com.portfolio.authserver.authorization.presentation;

import com.portfolio.authserver.authorization.domain.*;
import com.portfolio.authserver.authorization.presentation.dto.CreateRoleRequest;
import com.portfolio.authserver.authorization.presentation.dto.RoleResponse;
import com.portfolio.authserver.authorization.presentation.dto.UpdateRoleRequest;
import com.portfolio.authserver.authorization.presentation.mapper.AuthorizationMapper;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmJpaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/admin/realms/{realmName}/roles")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RealmJpaRepository realmJpaRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuthorizationMapper authorizationMapper;
    private final UserRoleRepository userRoleRepository;

    @GetMapping
    public List<RoleResponse> findRoles(@PathVariable String realmName) {
        return roleRepository.findByRealmName(realmName).stream().map(authorizationMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse createRole(@PathVariable String realmName, @Valid @RequestBody CreateRoleRequest request) {
        Realm realm = realmJpaRepository.findByName(realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found: " + realmName));

        if (roleRepository.findByRealmNameAndName(realmName, request.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already existing role: " + request.name());
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.permissionIds() != null) {
            for (String permissionId : request.permissionIds()) {
                Permission permission = permissionRepository.findByIdAndRealmName(permissionId, realmName)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Permission not found: " + permissionId));
                permissions.add(permission);
            }
        }

        Role role = new Role();
        role.setId(UUID.randomUUID().toString());
        role.setRealm(realm);
        role.setName(request.name());
        role.setLevel(request.level());
        role.setPermissions(permissions);

        return authorizationMapper.toResponse(roleRepository.save(role));
    }

    @PatchMapping("/{roleId}")
    public RoleResponse update(@PathVariable String realmName, @PathVariable String roleId,
                               @RequestBody UpdateRoleRequest request) {
        Role role = roleRepository.findByIdAndRealmName(roleId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        if (request.level() != null) {
            role.setLevel(request.level());
        }
        if (request.permissionIds() != null) {
            Set<Permission> permissions = new HashSet<>();
            for (String id : request.permissionIds()) {
                permissions.add(permissionRepository.findByIdAndRealmName(id, realmName)
                        .orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permission not found: " + id)));
            }
            role.setPermissions(permissions);
        }

        return authorizationMapper.toResponse(roleRepository.save(role));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> delete(@PathVariable String realmName, @PathVariable String roleId) {
        Role role = roleRepository.findByIdAndRealmName(roleId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        if (!userRoleRepository.findByRole(role).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete: the role is assigned to a user. Remove usages first.");
        }

        roleRepository.delete(role);
        return ResponseEntity.noContent().build();
    }
}