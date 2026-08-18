package com.portfolio.authserver.authorization;

import java.util.Set;

public record RoleResponse(String id, String name, Integer level, Set<String> permissionIds) {}
