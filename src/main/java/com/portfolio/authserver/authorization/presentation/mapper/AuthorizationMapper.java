package com.portfolio.authserver.authorization.presentation.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.authserver.authorization.domain.Permission;
import com.portfolio.authserver.authorization.domain.Role;
import com.portfolio.authserver.authorization.domain.UserRole;
import com.portfolio.authserver.authorization.presentation.dto.PermissionResponse;
import com.portfolio.authserver.authorization.presentation.dto.RoleResponse;
import com.portfolio.authserver.authorization.presentation.dto.UserRoleAttribute;
import com.portfolio.authserver.authorization.presentation.dto.UserRoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthorizationMapper {

    private final ObjectMapper objectMapper;

    public PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getName(), permission.getSubject(),
                permission.getAction(), permission.getConditionTemplate());
    }

    public RoleResponse toResponse(Role role) {
        Set<String> permissionIds = role.getPermissions().stream()
                .map(Permission::getId).collect(Collectors.toSet());
        return new RoleResponse(role.getId(), role.getName(), role.getLevel(), permissionIds);
    }

    public UserRoleResponse toResponse(UserRole userRole) {
        return new UserRoleResponse(userRole.getId(), userRole.getRole().getName(),
                userRole.getValidFrom(), userRole.getValidTo(), readAttributes(userRole.getAttributes()));
    }

    private List<UserRoleAttribute> readAttributes(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, UserRoleAttribute.class));
        } catch (Exception ex) {
            return List.of();
        }
    }
}