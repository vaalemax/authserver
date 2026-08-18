package com.portfolio.authserver.model.dto;


import java.util.List;

public record CanResult(boolean can, List<ConditionMatch> condition) {}