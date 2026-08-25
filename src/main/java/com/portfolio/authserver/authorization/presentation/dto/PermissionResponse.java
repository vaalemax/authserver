package com.portfolio.authserver.authorization.presentation.dto;

public record PermissionResponse(String id, String name, String subject, String action, String conditionTemplate) {}
