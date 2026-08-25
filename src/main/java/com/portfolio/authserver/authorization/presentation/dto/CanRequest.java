package com.portfolio.authserver.authorization.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CanRequest(@NotBlank String subject, @NotBlank String action) {}
