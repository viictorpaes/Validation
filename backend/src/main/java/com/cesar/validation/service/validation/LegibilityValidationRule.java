package com.cesar.validation.service.validation;

import com.cesar.validation.config.AppProperties;
import com.cesar.validation.enums.RuleOutcome;
import org.springframework.stereotype.Component;

@Component
public class LegibilityValidationRule implements ValidationRule {

    private final AppProperties appProperties;

    public LegibilityValidationRule(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public String code() {
        return "DOCUMENT_LEGIBILITY";
    }

    @Override
    public String name() {
        return "Legibilidade do Documento";
    }

    @Override
    public RuleEvaluation evaluate(ValidationContext context) {
        String text = context.ocrText() == null ? "" : context.ocrText().replaceAll("\\s+", "");
        int minChars = appProperties.getValidation().getMinOcrChars();

        if (text.length() < minChars) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.FAILED,
                "Documento ilegível ou sem texto reconhecível suficiente pelo OCR."
            );
        }
        return new RuleEvaluation(
            code(),
            name(),
            RuleOutcome.PASSED,
            "O documento apresenta texto legível extraído com sucesso."
        );
    }
}
