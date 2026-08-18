package com.portfolio.authserver.model.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRealmRequest(@NotBlank String name, String displayName) {
}
