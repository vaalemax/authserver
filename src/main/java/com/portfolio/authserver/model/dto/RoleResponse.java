package com.portfolio.authserver.model.dto;

import java.util.Set;

public record RoleResponse(String id, String name, Integer level, Set<String> permissionIds) {}
