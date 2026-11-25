package com.cesar.validation.service.validation;

import com.cesar.validation.config.AppProperties;
import com.cesar.validation.enums.RuleOutcome;
import com.cesar.validation.util.TextNormalizationUtil;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class IssueDateValidityRule implements ValidationRule {

    private final AppProperties appProperties;

    public IssueDateValidityRule(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public int order() {
        return 6;
    }

    @Override
    public String code() {
        return "ISSUE_DATE_VALIDITY";
    }

    @Override
    public String name() {
        return "Prazo de Emissão do Comprovante";
    }

    @Override
    public RuleEvaluation evaluate(ValidationContext context) {
        String rawDate = context.extractedData() != null ? context.extractedData().dataEmissao() : null;
        if (rawDate == null || rawDate.isBlank()) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.INCONCLUSIVE,
                "A data de emissão não foi localizada no documento para verificação automática."
            );
        }

        Optional<LocalDate> parsedOpt = TextNormalizationUtil.parseFlexibleDate(rawDate);
        if (parsedOpt.isEmpty()) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.INCONCLUSIVE,
                "A data de emissão extraída possui formato ambíguo e requer análise manual."
            );
        }

        LocalDate issueDate = parsedOpt.get();
        LocalDate refDate = context.referenceDate() != null ? context.referenceDate() : LocalDate.now();
        int maxAgeDays = appProperties.getValidation().getMaxDocumentAgeDays();

        if (issueDate.isAfter(refDate.plusDays(3))) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.FAILED,
                "A data de emissão do documento é inválida (futura em relação à data de validação)."
            );
        }

        long daysOld = ChronoUnit.DAYS.between(issueDate, refDate);
        if (daysOld > maxAgeDays) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.FAILED,
                "Comprovante emitido há " + daysOld + " dias, acima do limite permitido de " + maxAgeDays + " dias."
            );
        }

        return new RuleEvaluation(
            code(),
            name(),
            RuleOutcome.PASSED,
            "Data de emissão válida (dentro da janela de até " + maxAgeDays + " dias)."
        );
    }
}
