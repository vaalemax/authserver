package com.portfolio.authserver.user.presentation.dto;

import java.util.Set;

public record UpdateUserRequest(String password, Set<String> roles, Boolean enabled) {}
