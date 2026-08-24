package com.portfolio.authserver.authorization;

import java.time.Instant;
import java.util.List;

public record UpdateUserRoleRequest(Instant validTo, List<UserRoleAttribute> attributes) {}