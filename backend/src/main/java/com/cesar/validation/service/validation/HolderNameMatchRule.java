package com.cesar.validation.service.validation;

import com.cesar.validation.config.AppProperties;
import com.cesar.validation.enums.RuleOutcome;
import com.cesar.validation.util.TextNormalizationUtil;
import org.springframework.stereotype.Component;

@Component
public class HolderNameMatchRule implements ValidationRule {

    private final AppProperties appProperties;

    public HolderNameMatchRule(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public int order() {
        return 2;
    }

    @Override
    public String code() {
        return "HOLDER_NAME_MATCH";
    }

    @Override
    public String name() {
        return "Correspondência de Titularidade";
    }

    @Override
    public RuleEvaluation evaluate(ValidationContext context) {
        String extractedName = context.extractedData() != null ? context.extractedData().nomeTitular() : null;
        if (extractedName == null || extractedName.isBlank()) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.INCONCLUSIVE,
                "Não foi possível identificar o nome do titular no comprovante para confirmação automática."
            );
        }

        double similarity = TextNormalizationUtil.calculateNameSimilarity(
            context.informedHolderName(),
            extractedName
        );
        double threshold = appProperties.getValidation().getNameSimilarityThreshold();

        if (similarity >= threshold) {
            return new RuleEvaluation(
                code(),
                name(),
                RuleOutcome.PASSED,
                "O nome do titular identificado no comprovante corresponde ao titular informado."
            );
        }

        return new RuleEvaluation(
            code(),
            name(),
            RuleOutcome.FAILED,
            "O nome do titular identificado no documento não corresponde ao nome informado pelo usuário."
        );
    }
}
