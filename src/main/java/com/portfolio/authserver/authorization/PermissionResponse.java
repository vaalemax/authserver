package com.portfolio.authserver.authorization;

public record PermissionResponse(String id, String name, String subject, String action, String conditionTemplate) {}
