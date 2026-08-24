package com.portfolio.authserver.authorization;

import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    public PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getName(), permission.getSubject(),
                permission.getAction(), permission.getConditionTemplate());
    }
}
