package com.portfolio.authserver.authorization.application;

import com.portfolio.authserver.authorization.domain.Permission;
import com.portfolio.authserver.authorization.presentation.dto.PermissionResponse;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    public PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getName(), permission.getSubject(),
                permission.getAction(), permission.getConditionTemplate());
    }
}
