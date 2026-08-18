package com.portfolio.authserver.authorization;

import com.fasterxml.jackson.databind.JsonNode;

public record ConditionMatch(String role, JsonNode condition) {}
