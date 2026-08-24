package com.portfolio.authserver.user;

import java.util.Set;

public record UpdateUserRequest(String password, Set<String> roles, Boolean enabled) {}
