package com.portfolio.authserver.authorization.presentation.dto;

import java.util.Set;

public record UpdateRoleRequest(Integer level, Set<String> permissionIds) {}