package com.portfolio.authserver.model.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public record CreateUserRoleRequest(@NotBlank String roleId, Instant validFrom, Instant validTo, List<UserRoleAttribute> attributes) {}
