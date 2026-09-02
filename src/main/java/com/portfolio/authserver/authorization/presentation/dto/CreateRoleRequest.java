package com.portfolio.authserver.authorization.presentation.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record CreateRoleRequest(@NotBlank String name, Integer level, Set<String> permissionIds) {}
