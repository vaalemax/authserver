package com.portfolio.authserver.authorization;

import java.time.Instant;
import java.util.List;

public record UserRoleResponse(String id, String roleName, Instant validFrom, Instant validTo, List<UserRoleAttribute> attributes) {}
