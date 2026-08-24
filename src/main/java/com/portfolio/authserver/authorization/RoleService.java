package com.portfolio.authserver.authorization;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {
    public RoleResponse toResponse(Role role) {
        Set<String> permissionIds = role.getPermissions().stream().map(Permission::getId).collect(Collectors.toSet());
        return new RoleResponse(role.getId(), role.getName(), role.getLevel(), permissionIds);
    }
}
