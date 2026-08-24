package com.portfolio.authserver.authorization;

public record UpdatePermissionRequest(String name, String subjectLabel, String actionLabel,
                                      String conditionTemplate, String conditionLabel) {}