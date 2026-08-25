package com.portfolio.authserver.authorization.presentation.dto;

public record UpdatePermissionRequest(String name, String subjectLabel, String actionLabel,
                                      String conditionTemplate, String conditionLabel) {}