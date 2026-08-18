package com.portfolio.authserver.authorization;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionRequest(
        @NotBlank String name, @NotBlank String subject, String subjectLabel,
        @NotBlank String action, String actionLabel,
        String conditionTemplate, String conditionLabel
) {}