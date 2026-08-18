package com.portfolio.authserver.realm;

import jakarta.validation.constraints.NotBlank;

public record CreateRealmRequest(@NotBlank String name, String displayName) {
}
