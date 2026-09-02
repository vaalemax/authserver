package com.portfolio.authserver.realm.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRealmRequest(@NotBlank String name, String displayName) {
}
