package com.portfolio.authserver.authorization.application;

import com.portfolio.authserver.authorization.domain.*;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RealmRepository realmRepository;
    private final RoleRepository roleRepository;

    public List<Role> listRoles(String realmName) {
        return roleRepository.findByRealmName(realmName);
    }

    public Role createRole(String realmName, String name, Integer level, Set<String> permissionIds) {
        Realm realm = realmRepository.findByName(realmName)
                .orElseThrow(() -> new NoSuchElementException("Realm not found: " + realmName));

        if (roleRepository.findByRealmNameAndName(realmName, name).isPresent()) {
            throw new IllegalArgumentException("Already existing role: " + name);
        }

        Role role = new Role();
        role.setId(UUID.randomUUID().toString());
        role.setRealm(realm);
        role.setName(name);
        role.setLevel(level);
        role.setPermissions(resolvePermissions(realmName, permissionIds));

        return roleRepository.save(role);
    }

    public Role updateRole(String realmName, String roleId, Integer level, Set<String> permissionIds) {
        Role role = roleRepository.findByIdAndRealmName(roleId, realmName)
                .orElseThrow(() -> new NoSuchElementException("Role not found"));

        if (level != null) role.setLevel(level);
        if (permissionIds != null) role.setPermissions(resolvePermissions(realmName, permissionIds));

        return roleRepository.save(role);
    }

    public void deleteRole(String realmName, String roleId) {
        Role role = roleRepository.findByIdAndRealmName(roleId, realmName)
                .orElseThrow(() -> new NoSuchElementException("Role not found"));

        if (!userRoleRepository.findByRole(role).isEmpty()) {
            throw new IllegalStateException("Cannot delete: the role is assigned to a user. Remove usages first.");
        }

        roleRepository.delete(role);
    }

    private Set<Permission> resolvePermissions(String realmName, Set<String> permissionIds) {
        Set<Permission> permissions = new HashSet<>();
        if (permissionIds != null) {
            for (String id : permissionIds) {
                permissions.add(permissionRepository.findByIdAndRealmName(id, realmName)
                        .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id)));
            }
        }
        return permissions;
    }
}