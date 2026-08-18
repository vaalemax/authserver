package com.portfolio.authserver.authorization;


import java.util.List;

public record CanResult(boolean can, List<ConditionMatch> condition) {}