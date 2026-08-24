package com.portfolio.authserver.authorization;

import com.portfolio.authserver.realm.Realm;
import com.portfolio.authserver.realm.RealmJpaRepository;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/realms/{realmName}/roles")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RealmJpaRepository realmJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;
    private final RoleService roleService;
    private final UserRoleJpaRepository userRoleJpaRepository;

    @GetMapping
    public List<RoleResponse> findRoles(@PathVariable String realmName) {
        return roleJpaRepository.findByRealm_Name(realmName).stream().map(roleService::toResponse).toList();
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
                Permission permission = permissionJpaRepository.findByIdAndRealm_Name(permissionId, realmName)
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

        return roleService.toResponse(roleJpaRepository.save(role));
    }

    @PatchMapping("/{roleId}")
    public RoleResponse update(@PathVariable String realmName, @PathVariable String roleId,
                               @RequestBody UpdateRoleRequest request) {
        Role role = roleJpaRepository.findByIdAndRealm_Name(roleId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role non trovato"));

        if (request.level() != null) {
            role.setLevel(request.level());
        }
        if (request.permissionIds() != null) {
            Set<Permission> permissions = new HashSet<>();
            for (String id : request.permissionIds()) {
                permissions.add(permissionJpaRepository.findByIdAndRealm_Name(id, realmName)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permission non trovata: " + id)));
            }
            role.setPermissions(permissions);
        }

        return roleService.toResponse(roleJpaRepository.save(role));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> delete(@PathVariable String realmName, @PathVariable String roleId) {
        Role role = roleJpaRepository.findByIdAndRealm_Name(roleId, realmName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role non trovato"));

        if (!userRoleJpaRepository.findByRole(role).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossibile eliminare: il ruolo è assegnato ad almeno un utente. Rimuovi le assegnazioni prima.");
        }

        roleJpaRepository.delete(role);
        return ResponseEntity.noContent().build();
    }
}