package com.portfolio.authserver.user;

import java.util.Set;

public record UserResponse(String id, String username, Set<String> roles, boolean enabled) {
}
