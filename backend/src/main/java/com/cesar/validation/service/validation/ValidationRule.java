package com.cesar.validation.service.validation;

import com.cesar.validation.enums.RuleOutcome;

public interface ValidationRule {

    int order();

    String code();

    String name();

    RuleEvaluation evaluate(ValidationContext context);

    record RuleEvaluation(
        String code,
        String name,
        RuleOutcome outcome,
        String message
    ) {
    }
}
