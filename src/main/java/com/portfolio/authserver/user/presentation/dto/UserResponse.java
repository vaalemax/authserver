package com.portfolio.authserver.user.presentation.dto;

import java.util.Set;

public record UserResponse(String id, String username, Set<String> roles, boolean enabled) {
}
