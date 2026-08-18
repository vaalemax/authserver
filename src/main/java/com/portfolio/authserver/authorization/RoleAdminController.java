package com.portfolio.authserver.authorization;

import com.portfolio.authserver.realm.Realm;
import com.portfolio.authserver.realm.RealmJpaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/realms/{realmName}/roles")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RealmJpaRepository realmJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;

    @GetMapping
    public List<RoleResponse> findRoles(@PathVariable String realmName) {
        return roleJpaRepository.findByRealm_Name(realmName).stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse createRole(@PathVariable String realmName, @Valid @RequestBody CreateRoleRequest request) {
        Realm realm = realmJpaRepository.findByName(realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Realm not found: " + realmName));

        if (roleJpaRepository.findByRealm_NameAndName(realmName, request.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already existing role: " + request.name());
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.permissionIds() != null) {
            for (String permissionId : request.permissionIds()) {
                Permission permission = permissionJpaRepository.findById(permissionId)
                        .filter(p -> p.getRealm().getName().equals(realmName))
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

        return toResponse(roleJpaRepository.save(role));
    }

    private RoleResponse toResponse(Role role) {
        Set<String> permissionIds = role.getPermissions().stream().map(Permission::getId).collect(Collectors.toSet());
        return new RoleResponse(role.getId(), role.getName(), role.getLevel(), permissionIds);
    }
}