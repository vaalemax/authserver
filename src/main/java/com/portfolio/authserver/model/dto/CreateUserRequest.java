package com.portfolio.authserver.model.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record CreateUserRequest(@NotBlank String username, @NotBlank String password, Set<String> roles) {
}
