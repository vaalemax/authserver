package com.portfolio.authserver.authorization;

import java.util.Set;

public record UpdateRoleRequest(Integer level, Set<String> permissionIds) {}