package com.portfolio.authserver.model.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ConditionMatch(String role, JsonNode condition) {}
