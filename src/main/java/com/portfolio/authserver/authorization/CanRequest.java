package com.portfolio.authserver.authorization;

import jakarta.validation.constraints.NotBlank;

public record CanRequest(@NotBlank String subject, @NotBlank String action) {}
