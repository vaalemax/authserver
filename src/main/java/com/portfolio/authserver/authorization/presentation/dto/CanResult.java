package com.portfolio.authserver.authorization.presentation.dto;


import java.util.List;

public record CanResult(boolean can, List<ConditionMatch> condition) {}