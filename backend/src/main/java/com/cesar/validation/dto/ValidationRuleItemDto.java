package com.cesar.validation.dto;

import com.cesar.validation.enums.RuleOutcome;

public record ValidationRuleItemDto(
    String ruleCode,
    String ruleName,
    RuleOutcome outcome,
    boolean passed,
    String message
) {
}
