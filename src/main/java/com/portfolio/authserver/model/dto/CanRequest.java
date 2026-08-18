package com.portfolio.authserver.model.dto;

import jakarta.validation.constraints.NotBlank;

public record CanRequest(@NotBlank String subject, @NotBlank String action) {}
