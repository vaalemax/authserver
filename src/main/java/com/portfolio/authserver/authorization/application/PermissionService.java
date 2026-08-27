package com.portfolio.authserver.authorization.application;

import com.portfolio.authserver.authorization.domain.*;
import com.portfolio.authserver.realm.domain.Realm;
import com.portfolio.authserver.realm.domain.RealmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RealmRepository realmRepository;
    private final RoleRepository roleRepository;

    public List<Permission> listPermissions(String realmName) {
        return permissionRepository.findByRealmName(realmName);
    }

    public Permission getPermission(String realmName, String permissionId) {
        return permissionRepository.findByIdAndRealmName(permissionId, realmName)
                .orElseThrow(() -> new NoSuchElementException("Permission not found"));
    }

    public Permission createPermission(String realmName, String name, String subject, String subjectLabel,
                                       String action, String actionLabel,
                                       String conditionTemplate, String conditionLabel) {
        Realm realm = realmRepository.findByName(realmName)
                .orElseThrow(() -> new NoSuchElementException("Realm not found: " + realmName));

        if (permissionRepository.findByRealmNameAndSubjectAndAction(realmName, subject, action).isPresent()) {
            throw new IllegalArgumentException("Already existing permission " + subject + "/" + action);
        }

        Permission permission = new Permission();
        permission.setId(UUID.randomUUID().toString());
        permission.setRealm(realm);
        permission.setName(name);
        permission.setSubject(subject);
        permission.setSubjectLabel(subjectLabel);
        permission.setAction(action);
        permission.setActionLabel(actionLabel);
        permission.setConditionTemplate(conditionTemplate);
        permission.setConditionLabel(conditionLabel);

        return permissionRepository.save(permission);
    }

    public Permission updatePermission(String realmName, String permissionId, String name, String subjectLabel,
                                       String actionLabel, String conditionTemplate, String conditionLabel) {
        Permission permission = getPermission(realmName, permissionId);

        if (name != null) permission.setName(name);
        if (subjectLabel != null) permission.setSubjectLabel(subjectLabel);
        if (actionLabel != null) permission.setActionLabel(actionLabel);
        if (conditionTemplate != null) permission.setConditionTemplate(conditionTemplate);
        if (conditionLabel != null) permission.setConditionLabel(conditionLabel);

        return permissionRepository.save(permission);
    }

    public void deletePermission(String realmName, String permissionId) {
        Permission permission = getPermission(realmName, permissionId);

        List<Role> linkedRoles = roleRepository.findByPermissionsId(permissionId);
        if (!linkedRoles.isEmpty()) {
            String names = linkedRoles.stream().map(Role::getName).collect(Collectors.joining(", "));
            throw new IllegalStateException("Cannot delete: used in roles [" + names + "]. Remove usages first.");
        }

        permissionRepository.delete(permission);
    }
}