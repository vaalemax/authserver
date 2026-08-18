package com.portfolio.authserver.authorization;

public record UserRoleAttribute(String key, String type, String value, Boolean isArray) {}