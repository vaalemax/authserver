package com.portfolio.authserver.authorization.presentation.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ConditionMatch(String role, JsonNode condition) {}
